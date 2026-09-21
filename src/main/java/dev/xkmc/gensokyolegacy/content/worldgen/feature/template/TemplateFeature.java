package dev.xkmc.gensokyolegacy.content.worldgen.feature.template;

import com.mojang.serialization.Codec;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Places a hand-built vegetation template (tree, bush, huge mushroom) like a tree feature:
 * random rotation / mirror around the trunk, terrain checks, blending via
 * {@link TemplateBlendProcessor}. Same mechanism as the vanilla fossil feature, so the template
 * must stay within the 3x3 chunks a feature may write to (reach from the anchor &lt;= 15).
 * <p>
 * A plain configured feature: usable from saplings and mushroom growth as well.
 */
public class TemplateFeature extends Feature<TemplateFeatureConfig> {

	private static final int MAX_GROUND_SCAN = 48;
	private static final int MAX_SURFACE_CLIMB = 8;
	private static final int CLEARANCE_HEIGHT = 5;

	private static final Set<ResourceLocation> MISSING = ConcurrentHashMap.newKeySet();

	public TemplateFeature(Codec<TemplateFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<TemplateFeatureConfig> ctx) {
		WorldGenLevel level = ctx.level();
		RandomSource random = ctx.random();
		TemplateFeatureConfig config = ctx.config();

		BlockPos ground = findGround(level, ctx.origin());
		if (ground == null || !level.getBlockState(ground).is(config.ground())) return false;
		if (!checkTerrain(level, ground, config)) return false;
		BlockPos base = ground.above(1 + config.yOffset());
		if (!checkClearance(level, base)) return false;

		Optional<ResourceLocation> id = config.templates().getRandomValue(random);
		if (id.isEmpty()) return false;
		Optional<StructureTemplate> opt = level.getLevel().getServer().getStructureManager().get(id.get());
		if (opt.isEmpty()) {
			if (MISSING.add(id.get())) GensokyoLegacy.LOGGER.error("Missing vegetation template {}", id.get());
			return false;
		}
		StructureTemplate template = opt.get();
		Vec3i size = template.getSize();
		if (!checkHeadroom(level, base, size.getY())) return false;

		// anchor = centre of the bottom face; mirroring is about the origin and rotation about the
		// pivot, so transform the anchor first to keep the trunk on the feature origin
		Rotation rotation = Rotation.getRandom(random);
		Mirror mirror = random.nextBoolean() ? Mirror.NONE : Mirror.FRONT_BACK;
		BlockPos anchor = StructureTemplate.transform(new BlockPos(size.getX() / 2, 0, size.getZ() / 2), mirror, rotation, BlockPos.ZERO);
		BlockPos start = base.subtract(anchor);

		StructurePlaceSettings settings = new StructurePlaceSettings()
				.setRotation(rotation).setMirror(mirror).setRandom(random)
				.setIgnoreEntities(true)
				.setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING)
				// template states are self-consistent; skipping shape updates also avoids a tick per leaf
				.setKnownShape(true)
				.addProcessor(new TemplateBlendProcessor(config.rootDepth()));
		config.processors().value().list().forEach(settings::addProcessor);
		if (level instanceof WorldGenRegion region) {
			ChunkPos center = region.getCenter();
			settings.setBoundingBox(new BoundingBox(
					center.getMinBlockX() - 16, level.getMinBuildHeight(), center.getMinBlockZ() - 16,
					center.getMaxBlockX() + 16, level.getMaxBuildHeight(), center.getMaxBlockZ() + 16));
		}
		if (!template.placeInWorld(level, start, start, settings, random, Block.UPDATE_CLIENTS)) return false;
		for (Holder<PlacedFeature> post : config.postFeatures()) {
			post.value().place(level, ctx.chunkGenerator(), random, base);
		}
		return true;
	}

	/**
	 * The origin may sit on a canopy or a limb: look through vegetation for the real ground.
	 * Patch style placements may also hand in an origin inside the terrain.
	 */
	@Nullable
	private static BlockPos findGround(WorldGenLevel level, BlockPos origin) {
		BlockPos.MutableBlockPos pos = origin.mutable();
		for (int i = 0; !TemplateBlendProcessor.isPassable(level.getBlockState(pos)); i++) {
			if (i >= MAX_SURFACE_CLIMB || pos.getY() >= level.getMaxBuildHeight() - 1) return null;
			pos.move(Direction.UP);
		}
		for (int i = 0; i < MAX_GROUND_SCAN && pos.getY() > level.getMinBuildHeight(); i++) {
			if (!TemplateBlendProcessor.isPassable(level.getBlockState(pos))) return pos.immutable();
			pos.move(Direction.DOWN);
		}
		return null;
	}

	/**
	 * 8 samples around the anchor: all dry and within max slope of each other and the anchor.
	 */
	private static boolean checkTerrain(WorldGenLevel level, BlockPos ground, TemplateFeatureConfig config) {
		if (!level.getFluidState(ground.above()).isEmpty()) return false;
		int r = config.footprintRadius();
		if (r == 0) return true;
		int min = ground.getY(), max = ground.getY();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) continue;
				int y = groundAt(level, pos, ground.getX() + dx * r, ground.getY(), ground.getZ() + dz * r, config.maxSlope());
				if (y == Integer.MIN_VALUE) return false;
				min = Math.min(min, y);
				max = Math.max(max, y);
			}
		}
		return max - min <= config.maxSlope();
	}

	/**
	 * Ground height within y +- range, MIN_VALUE when the terrain leaves that window or is wet.
	 */
	private static int groundAt(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z, int range) {
		pos.set(x, y + range + 1, z);
		if (!TemplateBlendProcessor.isPassable(level.getBlockState(pos))) return Integer.MIN_VALUE;
		for (int i = 0; i <= 2 * range; i++) {
			pos.move(Direction.DOWN);
			BlockState state = level.getBlockState(pos);
			if (TemplateBlendProcessor.isPassable(state)) continue;
			return state.getFluidState().isEmpty() ? pos.getY() : Integer.MIN_VALUE;
		}
		return Integer.MIN_VALUE;
	}

	/**
	 * No foreign trunk or mushroom cap within one block of where ours will stand: a trunk placed
	 * into either would be left with holes.
	 */
	private static boolean checkClearance(WorldGenLevel level, BlockPos base) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = 0; dy < CLEARANCE_HEIGHT; dy++) {
					BlockState state = level.getBlockState(pos.setWithOffset(base, dx, dy, dz));
					if (state.is(GLTagGen.TEMPLATE_TRUNK) || state.getBlock() instanceof HugeMushroomBlock) return false;
				}
			}
		}
		return true;
	}

	/**
	 * Nothing hard above the anchor for the lower half of the template (overhangs, cliffs).
	 */
	private static boolean checkHeadroom(WorldGenLevel level, BlockPos base, int height) {
		BlockPos.MutableBlockPos pos = base.mutable();
		int top = Math.min(level.getMaxBuildHeight() - 1, base.getY() + height / 2);
		while (pos.getY() <= top) {
			if (!TemplateBlendProcessor.isSoft(level.getBlockState(pos))) return false;
			pos.move(Direction.UP);
		}
		return base.getY() + height < level.getMaxBuildHeight();
	}

}

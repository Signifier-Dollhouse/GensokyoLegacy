package dev.xkmc.gensokyolegacy.content.worldgen.feature.lake;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pond feature for the magical forest, adapted from TheLostLegends lake.
 * Carves a union of ellipsoid blobs: water below {@code depth}, air above, with a clay rim
 * under water, dirt padding around it and grass on the shoreline. Afterwards the waterline is
 * planted with lily pads, small dripleafs and flame cattails.
 * <p>
 * Placed in the LAKES step before vegetation: {@code TemplateFeature} rejects wet ground, so
 * giant and large trees keep off the water on their own. The {@link LakeMaker#test} log and
 * leaf check additionally rejects ponds that would cut canopies reaching in from neighbours.
 */
public class MagicalForestLakeFeature extends Feature<MagicalForestLakeFeature.Data> {

	private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

	public MagicalForestLakeFeature(Codec<Data> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<Data> ctx) {
		WorldGenLevel level = ctx.level();
		Data data = ctx.config();
		BlockPos pos = findValid(level, ctx.origin(), 8);
		if (pos == null) return false;
		pos = pos.below(data.depth);
		int size = ctx.random().nextInt(data.minTrial, data.maxTrial);
		var maker = new LakeMaker(this, data.maxWidth, data.maxHeight);
		maker.pre(ctx.random(), data, size);
		if (!maker.test(level, pos, data)) return false;
		maker.gen(level, pos, ctx.random(), data);
		decorate(level, pos, data, ctx.random());
		return true;
	}

	public void setAir(WorldGenLevel level, BlockPos pos) {
		setBlock(level, pos, AIR);
		level.scheduleTick(pos, AIR.getBlock(), 0);
		markAboveForPostProcessing(level, pos);
	}

	public void setFluid(WorldGenLevel level, BlockPos pos, BlockState fluid) {
		setBlock(level, pos, fluid);
	}

	public void setBarrier(WorldGenLevel level, BlockPos pos, BlockState barrier) {
		setBlock(level, pos, barrier);
		markAboveForPostProcessing(level, pos);
	}

	/**
	 * Waterline planting from the {@code decorations} list: floating plants on open water,
	 * shallow plants in 1-deep water over matching soil, shore plants on matching soil next
	 * to the water. Entries are tried in order and skip claimed cells, so list order is
	 * priority. Runs on the freshly carved pond, so every plant lands on the lake itself.
	 */
	private void decorate(WorldGenLevel level, BlockPos origin, Data data, RandomSource rand) {
		if (data.decorations().isEmpty()) return;
		var pos = new BlockPos.MutableBlockPos();
		for (int dx = 0; dx < data.maxWidth(); dx++) {
			for (int dz = 0; dz < data.maxWidth(); dz++) {
				int x = origin.getX() + dx - data.maxWidth() / 2;
				int z = origin.getZ() + dz - data.maxWidth() / 2;
				for (int dy = data.maxHeight() - 1; dy >= 0; dy--) {
					pos.set(x, origin.getY() + dy, z);
					BlockState state = level.getBlockState(pos);
					if (state.isAir()) continue;
					if (state.is(Blocks.WATER) && level.getBlockState(pos.above()).isAir()) {
						decorateWater(level, pos.immutable(), data, rand);
					} else if (state.isSolid() && level.getFluidState(pos).isEmpty() &&
							level.getBlockState(pos.above()).isAir()) {
						decorateShore(level, pos.immutable(), data, rand);
					}
					break;
				}
			}
		}
	}

	private void decorateWater(WorldGenLevel level, BlockPos surface, Data data, RandomSource rand) {
		var probe = new BlockPos.MutableBlockPos();
		probe.set(surface);
		int depth = 0;
		while (level.getBlockState(probe).is(Blocks.WATER)) {
			depth++;
			probe.move(0, -1, 0);
		}
		BlockState soil = level.getBlockState(probe);
		BlockPos top = surface.above();
		for (Deco deco : data.decorations()) {
			if (deco.target() == Target.FLOATING) {
				if (depth >= 2 && level.getBlockState(top).isAir() && rand.nextFloat() < deco.chance())
					setBlock(level, top, deco.block());
			} else if (deco.target() == Target.SHALLOW && depth == 1) {
				if (level.getBlockState(surface).is(Blocks.WATER) && level.getBlockState(top).isAir() &&
						soil.is(deco.soil()) && rand.nextFloat() < deco.chance()) {
					setBlock(level, surface, deco.block());
					if (!deco.upper().isAir()) setBlock(level, top, deco.upper());
				}
			}
		}
	}

	private void decorateShore(WorldGenLevel level, BlockPos ground, Data data, RandomSource rand) {
		BlockPos top = ground.above();
		BlockState soil = level.getBlockState(ground);
		for (Deco deco : data.decorations()) {
			if (deco.target() != Target.SHORE) continue;
			if (soil.is(deco.soil()) && level.getBlockState(top).isAir() &&
					nextToWater(level, ground) && rand.nextFloat() < deco.chance())
				setBlock(level, top, deco.block());
		}
	}

	private static boolean nextToWater(WorldGenLevel level, BlockPos ground) {
		var pos = new BlockPos.MutableBlockPos();
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			pos.setWithOffset(ground, dir);
			pos.move(0, 1, 0);
			if (level.getBlockState(pos).is(Blocks.WATER)) return true;
		}
		return false;
	}

	/**
	 * Ground search from TheLostLegends OnGroundFeature: down through air to the soil, then
	 * back up to the first free block.
	 */
	@Nullable
	private static BlockPos findValid(LevelAccessor level, BlockPos origin, int maxStep) {
		var pos = new BlockPos.MutableBlockPos();
		pos.set(origin);
		while (level.isEmptyBlock(pos)) {
			pos.move(0, -1, 0);
			maxStep--;
			if (maxStep < 0) return null;
			if (level.isOutsideBuildHeight(pos)) return null;
		}
		maxStep++;
		while (!level.isEmptyBlock(pos)) {
			pos.move(0, 1, 0);
			maxStep--;
			if (maxStep < 0) return null;
			if (level.isOutsideBuildHeight(pos)) return null;
		}
		return pos;
	}

	public record Data(
			BlockState fluid, BlockState barrier, BlockState padding, BlockState surface,
			int depth, int maxWidth, int maxHeight, int minTrial, int maxTrial, int radius, int margin,
			List<Deco> decorations
	) implements FeatureConfiguration {

		public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockState.CODEC.fieldOf("fluid").forGetter(Data::fluid),
				BlockState.CODEC.fieldOf("barrier").forGetter(Data::barrier),
				BlockState.CODEC.fieldOf("padding").forGetter(Data::padding),
				BlockState.CODEC.fieldOf("surface").forGetter(Data::surface),
				Codec.INT.fieldOf("depth").forGetter(Data::depth),
				Codec.INT.fieldOf("max_width").forGetter(Data::maxWidth),
				Codec.INT.fieldOf("max_height").forGetter(Data::maxHeight),
				Codec.INT.fieldOf("min_component").forGetter(Data::minTrial),
				Codec.INT.fieldOf("max_component").forGetter(Data::maxTrial),
				Codec.INT.fieldOf("component_radius").forGetter(Data::radius),
				Codec.INT.fieldOf("margin").forGetter(Data::margin),
				Deco.CODEC.listOf().optionalFieldOf("decorations", List.of()).forGetter(Data::decorations)
		).apply(i, Data::new));

	}

	/**
	 * Where a decoration goes: on open water, in 1-deep water, or on the shore next to water.
	 */
	public enum Target {

		FLOATING("floating"), SHALLOW("shallow"), SHORE("shore");

		public static final Codec<Target> CODEC = Codec.STRING.comapFlatMap(s -> switch (s) {
			case "floating" -> DataResult.success(FLOATING);
			case "shallow" -> DataResult.success(SHALLOW);
			case "shore" -> DataResult.success(SHORE);
			default -> DataResult.error(() -> "Unknown lake decoration target: " + s);
		}, Target::serialized);

		private final String serialized;

		Target(String serialized) {
			this.serialized = serialized;
		}

		public String serialized() {
			return serialized;
		}

	}

	/**
	 * One waterline plant: {@code block} at the water surface or shore top, {@code upper} above
	 * it for double-height plants, on soil matching {@code soil}.
	 */
	public record Deco(Target target, BlockState block, BlockState upper, float chance, TagKey<Block> soil) {

		public static final Codec<Deco> CODEC = RecordCodecBuilder.create(i -> i.group(
				Target.CODEC.fieldOf("target").forGetter(Deco::target),
				BlockState.CODEC.fieldOf("block").forGetter(Deco::block),
				BlockState.CODEC.optionalFieldOf("upper", Blocks.AIR.defaultBlockState()).forGetter(Deco::upper),
				Codec.FLOAT.fieldOf("chance").forGetter(Deco::chance),
				TagKey.hashedCodec(Registries.BLOCK).optionalFieldOf("soil", BlockTags.DIRT).forGetter(Deco::soil)
		).apply(i, Deco::new));

	}

}

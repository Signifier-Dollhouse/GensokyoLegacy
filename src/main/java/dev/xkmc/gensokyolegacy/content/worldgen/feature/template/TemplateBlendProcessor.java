package dev.xkmc.gensokyolegacy.content.worldgen.feature.template;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blends a vegetation template into whatever is already there instead of stamping it:
 * <ul>
 * <li>trunk blocks ({@link GLTagGen#TEMPLATE_TRUNK}) replace soft blocks and dirt, so uphill roots
 * sink into the slope;</li>
 * <li>foliage (leaves, mushroom caps) only fills soft blocks and never eats foreign leaves;</li>
 * <li>attachments (vines, carpets, side mushrooms, hanging plants) only fill air and are dropped
 * when their support did not make it;</li>
 * <li>trunk blocks of layer 0 are extended downwards to the ground, so downhill roots never
 * float.</li>
 * </ul>
 * Templates carry no air, hence nothing is ever carved.
 */
public class TemplateBlendProcessor extends StructureProcessor {

	public static final MapCodec<TemplateBlendProcessor> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(0, 16).fieldOf("root_depth").forGetter(e -> e.rootDepth)
	).apply(i, TemplateBlendProcessor::new));

	private enum Kind {
		TRUNK, FOLIAGE, ATTACHMENT;

		static Kind of(BlockState state) {
			if (state.is(GLTagGen.TEMPLATE_TRUNK)) return TRUNK;
			if (state.is(BlockTags.LEAVES) || state.getBlock() instanceof HugeMushroomBlock) return FOLIAGE;
			return ATTACHMENT;
		}
	}

	private final int rootDepth;

	public TemplateBlendProcessor(int rootDepth) {
		this.rootDepth = rootDepth;
	}

	/**
	 * May be overwritten by any template block: air and replaceable, fluid-free blocks.
	 */
	public static boolean isSoft(BlockState state) {
		if (state.isAir()) return true;
		if (!state.getFluidState().isEmpty()) return false;
		return state.canBeReplaced() || state.is(BlockTags.REPLACEABLE_BY_TREES);
	}

	/**
	 * Vegetation a ground scan looks through: soft blocks plus trunks and mushroom caps.
	 */
	public static boolean isPassable(BlockState state) {
		return isSoft(state) || state.is(GLTagGen.TEMPLATE_TRUNK) || state.getBlock() instanceof HugeMushroomBlock;
	}

	@Nullable
	@Override
	public StructureBlockInfo processBlock(
			LevelReader level, BlockPos offset, BlockPos pos,
			StructureBlockInfo blockInfo, StructureBlockInfo rel, StructurePlaceSettings settings
	) {
		BlockState world = level.getBlockState(rel.pos());
		if (world.is(BlockTags.FEATURES_CANNOT_REPLACE)) return null;
		boolean place = switch (Kind.of(rel.state())) {
			case TRUNK -> isSoft(world) || world.is(BlockTags.DIRT);
			case FOLIAGE -> isSoft(world) && !world.is(BlockTags.LEAVES);
			case ATTACHMENT -> world.isAir();
		};
		return place ? rel : null;
	}

	/**
	 * Vanilla builds {@code original} (template local) and {@code processed} (world) in lockstep,
	 * so index {@code i} refers to the same block in both. The lists returned by this method are
	 * not aligned any more (roots added, attachments dropped): this processor must run first.
	 */
	@Override
	public List<StructureBlockInfo> finalizeProcessing(
			ServerLevelAccessor level, BlockPos offset, BlockPos pos,
			List<StructureBlockInfo> original, List<StructureBlockInfo> processed, StructurePlaceSettings settings
	) {
		List<StructureBlockInfo> ans = new ArrayList<>(processed.size() + 32);
		List<StructureBlockInfo> attachments = new ArrayList<>();
		Set<BlockPos> solid = new HashSet<>();
		for (int i = 0; i < processed.size(); i++) {
			StructureBlockInfo info = processed.get(i);
			Kind kind = Kind.of(info.state());
			if (kind == Kind.ATTACHMENT) {
				attachments.add(info);
				continue;
			}
			ans.add(info);
			solid.add(info.pos());
			if (kind == Kind.TRUNK && original.get(i).pos().getY() == 0) {
				extendRoot(level, info, ans, solid);
			}
		}
		// top-down, so a hanging chain is cut at the first segment that lost its support
		attachments.sort(Comparator.comparingInt(e -> -e.pos().getY()));
		Map<BlockPos, BlockState> kept = new HashMap<>();
		for (StructureBlockInfo info : attachments) {
			BlockState state = info.state().mirror(settings.getMirror()).rotate(settings.getRotation());
			if (isSupported(level, info.pos(), state, solid, kept)) {
				kept.put(info.pos(), state);
				ans.add(info);
			}
		}
		return ans;
	}

	private void extendRoot(LevelReader level, StructureBlockInfo info, List<StructureBlockInfo> ans, Set<BlockPos> solid) {
		BlockPos.MutableBlockPos pos = info.pos().mutable();
		for (int i = 0; i < rootDepth; i++) {
			pos.move(Direction.DOWN);
			if (!isSoft(level.getBlockState(pos))) return;
			BlockPos root = pos.immutable();
			ans.add(new StructureBlockInfo(root, info.state(), null));
			solid.add(root);
		}
	}

	private static boolean isSupported(LevelReader level, BlockPos pos, BlockState state, Set<BlockPos> solid, Map<BlockPos, BlockState> kept) {
		if (state.getBlock() instanceof VineBlock) {
			if (state.getValue(VineBlock.UP) && isSolid(level, pos.above(), solid)) return true;
			BlockState above = kept.get(pos.above());
			for (Direction dir : Direction.Plane.HORIZONTAL) {
				var prop = VineBlock.getPropertyForFace(dir);
				if (!state.getValue(prop)) continue;
				if (isSolid(level, pos.relative(dir), solid)) return true;
				if (above != null && above.getBlock() instanceof VineBlock && above.getValue(prop)) return true;
			}
			return false;
		}
		if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return isSolid(level, pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()), solid);
		}
		if (state.getBlock() instanceof CarpetBlock) {
			return isSolid(level, pos.below(), solid);
		}
		// hanging plants chain downwards, anything else needs a floor
		return isSolid(level, pos.above(), solid) || kept.containsKey(pos.above()) || isSolid(level, pos.below(), solid);
	}

	private static boolean isSolid(LevelReader level, BlockPos pos, Set<BlockPos> solid) {
		if (solid.contains(pos)) return true;
		BlockState state = level.getBlockState(pos);
		return state.is(BlockTags.LEAVES) || !isSoft(state) && state.getFluidState().isEmpty();
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return GLWorldGen.TEMPLATE_BLEND.get();
	}

}

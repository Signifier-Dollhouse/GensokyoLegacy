package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.TriState;

public class SimpleBushBlock extends BushBlock {

	private static final MapCodec<SimpleBushBlock> CODEC = simpleCodec(SimpleBushBlock::new);

	public static final VoxelShape FULL = Shapes.block();
	public static final VoxelShape THIN = Block.box(0, 0, 7, 16, 16, 9);
	public static final VoxelShape LAYER = Block.box(0, 0, 0, 16, 1, 16);

	private final VoxelShape shape;

	public SimpleBushBlock(Properties properties, VoxelShape shape) {
		super(properties);
		this.shape = shape;
	}

	public SimpleBushBlock(Properties properties) {
		this(properties, FULL);
	}

	@Override
	public MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(BlockTags.DIRT) || state.getBlock() instanceof FarmBlock
				|| state.isFaceSturdy(level, pos, Direction.UP);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos below = pos.below();
		BlockState belowState = level.getBlockState(below);
		TriState soilDecision = belowState.canSustainPlant(level, below, Direction.UP, state);
		if (!soilDecision.isDefault()) return soilDecision.isTrue();
		return this.mayPlaceOn(belowState, level, below);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shape;
	}

}
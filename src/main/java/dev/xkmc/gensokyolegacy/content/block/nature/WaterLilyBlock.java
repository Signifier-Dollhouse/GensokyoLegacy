package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A water lily floating on the surface: it needs water or ice below it and free space above.
 * <p>
 * The item is a {@link PlaceOnWaterBlockItem}, so the lily lands on the block above the water source the
 * player aims at, the way the vanilla lily pad does, and the stem of the model reaches down into the water.
 */
public class WaterLilyBlock extends BushBlock {

	private static final MapCodec<WaterLilyBlock> CODEC = simpleCodec(WaterLilyBlock::new);

	/**
	 * The lily is not symmetric, so it keeps the direction it was planted in.
	 */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/**
	 * The pad of the small lily, a thin slab at the height its model draws it.
	 */
	protected static final VoxelShape SMALL_PAD = Block.box(1.0, 5.0, 1.0, 15.0, 6.5, 15.0);

	private final VoxelShape shape;

	public WaterLilyBlock(Properties properties) {
		this(properties, SMALL_PAD);
	}

	public WaterLilyBlock(Properties properties, VoxelShape shape) {
		super(properties);
		this.shape = shape;
	}

	@Override
	public MapCodec<? extends BushBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		if (!level.getBlockState(pos).canBeReplaced()) return null;
		if (!mayPlaceOn(level.getBlockState(pos.below()), level, pos.below())) return null;
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		FluidState below = level.getFluidState(pos);
		FluidState here = level.getFluidState(pos.above());
		boolean water = below.getType() == Fluids.WATER || state.getBlock() instanceof IceBlock;
		return water && here.getType() == Fluids.EMPTY;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		// the lily drifts sideways, so its hitbox has to drift with it
		var offset = state.getOffset(level, pos);
		return shape.move(offset.x, offset.y, offset.z);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		super.entityInside(state, level, pos, entity);
		if (level instanceof ServerLevel && entity instanceof Boat) {
			level.destroyBlock(pos, true, entity);
		}
	}

	protected float getMaxVerticalOffset() {
		return 1 / 16f;
	}

}

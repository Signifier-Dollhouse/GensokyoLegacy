package dev.xkmc.gensokyolegacy.content.block.door;

import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.impl.DoubleBlockImpl;
import dev.xkmc.l2modularblock.mult.*;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

public class SlidingDoor implements CreateBlockStateBlockMethod, DefaultStateBlockMethod, PlacementBlockMethod,
		UseWithoutItemBlockMethod, UseItemOnBlockMethod, ShapeBlockMethod {

	public static final int MAX = 4;

	public static final IntegerProperty STACK = IntegerProperty.create("stack", 1, MAX);
	public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;

	public static final VoxelShape[][] SHAPES = new VoxelShape[MAX][4];

	static {
		for (int stack = 1; stack <= MAX; stack++) {
			var builder = new VoxelBuilder(0, 0, 0, 16, 16, stack + 1);
			for (int i = 0; i < 4; i++) {
				SHAPES[stack - 1][i] = builder.rotateFromNorth(Direction.from2DDataValue(i));
			}
		}
	}

	public static DelegateBlock create(BlockBehaviour.Properties p) {
		return DelegateBlock.newBaseBlock(p, BlockTemplates.HORIZONTAL, new DoubleBlockImpl(), new SlidingDoor());
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HINGE, STACK);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(HINGE, DoorHingeSide.LEFT).setValue(STACK, 1);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockState def, BlockPlaceContext context) {
		if (def == null) return null;
		Direction facing = def.getValue(HORIZONTAL_FACING).getOpposite();
		DoorHingeSide hinge = hingeFromClick(context);
		return def.setValue(HORIZONTAL_FACING, facing).setValue(HINGE, hinge);
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player pl, BlockHitResult result) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		BlockPos bottom = bottom(level, pos);
		BlockState bs = level.getBlockState(bottom);
		boolean open = canOpen(level, bottom, bs);
		boolean close = canClose(level, bottom, bs);
		if (open && close) {
			Direction left = bs.getValue(HORIZONTAL_FACING).getCounterClockWise();
			Axis axis = left.getAxis();
			Vec3 click = result.getLocation().subtract(bottom.getCenter());
			double coord = axis.choose(click.x, click.y, click.z) * left.getAxisDirection().getStep();
			DoorHingeSide clickedHinge = coord < 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
			if (clickedHinge == bs.getValue(HINGE)) {
				doOpen(level, bottom, bs);
			} else {
				doClose(level, bottom, bs);
			}
		} else if (open) {
			doOpen(level, bottom, bs);
		} else if (close) {
			doClose(level, bottom, bs);
		} else {
			return InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player pl, InteractionHand hand, BlockHitResult result) {
		if (stack.is(state.getBlock().asItem())) {
			if (!level.isClientSide() && state.getValue(STACK) < MAX) {
				BlockPos bottom = bottom(level, pos);
				int value = level.getBlockState(bottom).getValue(STACK) + 1;
				setStack(level, bottom, value);
				if (!pl.getAbilities().instabuild) {
					stack.shrink(1);
				}
			}
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(STACK) - 1][state.getValue(HORIZONTAL_FACING).get2DDataValue()];
	}

	static boolean canOpen(BlockGetter level, BlockPos bottom, BlockState bs) {
		BlockPos pocket = bottom.relative(hingeDir(bs));
		if (bs.getValue(STACK) == 1 && !isConnected(level, bottom, bs)) {
			return isAir(level, pocket);
		}
		BlockState pocketState = level.getBlockState(pocket);
		return !isConnected(level, bottom, bs, hingeDir(bs).getOpposite())
				&& pocketState.is(bs.getBlock())
				&& pocketState.getValue(HORIZONTAL_FACING) == bs.getValue(HORIZONTAL_FACING)
				&& pocketState.getValue(HINGE) == bs.getValue(HINGE)
				&& pocketState.getValue(STACK) + bs.getValue(STACK) <= MAX;
	}

	static void doOpen(Level level, BlockPos bottom, BlockState bs) {
		if (!canOpen(level, bottom, bs)) return;
		BlockPos pocket = bottom.relative(hingeDir(bs));
		if (bs.getValue(STACK) == 1 && !isConnected(level, bottom, bs)) {
			place(level, pocket, bs);
		} else {
			BlockState pocketState = level.getBlockState(pocket);
			int sum = pocketState.getValue(STACK) + bs.getValue(STACK);
			setStack(level, pocket, sum);
		}
		setAir(level, bottom);
		playSound(level, bottom, true);
	}

	static boolean canClose(BlockGetter level, BlockPos bottom, BlockState bs) {
		BlockPos leaf = bottom.relative(hingeDir(bs).getOpposite());
		if (bs.getValue(STACK) == 1 && !isConnected(level, bottom, bs)) {
			return isAir(level, leaf);
		}
		return bs.getValue(STACK) > 1 && isAir(level, leaf);
	}

	static void doClose(Level level, BlockPos bottom, BlockState bs) {
		if (!canClose(level, bottom, bs)) return;
		BlockPos leaf = bottom.relative(hingeDir(bs).getOpposite());
		if (bs.getValue(STACK) == 1 && !isConnected(level, bottom, bs)) {
			place(level, leaf, bs);
			setAir(level, bottom);
		} else {
			int move = bs.getValue(STACK) - 1;
			level.setBlock(leaf, bs.setValue(STACK, move).setValue(HALF, Half.BOTTOM), 3);
			level.setBlock(leaf.above(), bs.setValue(STACK, move).setValue(HALF, Half.TOP), 3);
			setStack(level, bottom, 1);
		}
		playSound(level, bottom, false);
	}

	public static BlockPos bottom(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).getValue(HALF) == Half.BOTTOM ? pos : pos.below();
	}

	private static void setStack(Level level, BlockPos bottom, int value) {
		BlockState state = level.getBlockState(bottom);
		level.setBlock(bottom, state.setValue(STACK, value).setValue(HALF, Half.BOTTOM), 3);
		level.setBlock(bottom.above(), state.setValue(STACK, value).setValue(HALF, Half.TOP), 3);
	}

	private static void setAir(Level level, BlockPos bottom) {
		var old = level.getBlockState(bottom);
		level.setBlock(bottom, Blocks.AIR.defaultBlockState(), 3 | 16);
		level.setBlock(bottom.above(), Blocks.AIR.defaultBlockState(), 3);
		level.updateNeighborsAt(bottom, old.getBlock());
	}

	private static boolean isAir(BlockGetter level, BlockPos pos) {
		return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
	}

	private static boolean isConnected(BlockGetter level, BlockPos bottom, BlockState bs) {
		return isConnected(level, bottom, bs, hingeDir(bs))
				|| isConnected(level, bottom, bs, hingeDir(bs).getOpposite());
	}

	private static boolean isConnected(BlockGetter level, BlockPos bottom, BlockState bs, Direction dir) {
		BlockState neighbor = level.getBlockState(bottom.relative(dir));
		return neighbor.is(bs.getBlock())
				&& neighbor.getValue(HORIZONTAL_FACING) == bs.getValue(HORIZONTAL_FACING)
				&& neighbor.getValue(HINGE) == bs.getValue(HINGE);
	}

	private static void place(Level level, BlockPos bottom, BlockState bs) {
		BlockState base = bs.setValue(STACK, 1);
		level.setBlock(bottom, base.setValue(HALF, Half.BOTTOM), 3);
		level.setBlock(bottom.above(), base.setValue(HALF, Half.TOP), 3);
	}

	static Direction hingeDir(BlockState state) {
		var facing = state.getValue(HORIZONTAL_FACING);
		return state.getValue(HINGE) == DoorHingeSide.LEFT ? facing.getCounterClockWise() : facing.getClockWise();
	}

	private static void playSound(Level level, BlockPos pos, boolean open) {
		level.playSound(null, pos, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
				SoundSource.BLOCKS, 1, 1);
	}

	private static DoorHingeSide hingeFromClick(BlockPlaceContext context) {
		Direction left = context.getHorizontalDirection().getCounterClockWise();
		Axis axis = left.getAxis();
		Vec3 click = context.getClickLocation().subtract(context.getClickedPos().getCenter());
		double coord = axis.choose(click.x, click.y, click.z) * left.getAxisDirection().getStep();
		return coord > 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
	}

}

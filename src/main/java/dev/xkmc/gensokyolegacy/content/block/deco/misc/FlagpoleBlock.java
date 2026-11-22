package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlagpoleBlock extends Block {

	public static final BooleanProperty BASE = BooleanProperty.create("base");
	private static final MapCodec<FlagpoleBlock> CODEC = simpleCodec(FlagpoleBlock::new);
	private static final VoxelShape POLE = Block.box(6.5, 0, 6.5, 9.5, 16, 9.5);
	private static final VoxelShape WITH_BASE = Shapes.or(POLE, Block.box(5.5, 0, 5.5, 10.5, 1, 10.5));

	public FlagpoleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BASE, true));
	}

	@Override
	public MapCodec<FlagpoleBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BASE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(BASE,
				!context.getLevel().getBlockState(context.getClickedPos().below()).is(this));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
									 LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return direction == Direction.DOWN ? state.setValue(BASE, !neighbor.is(this)) :
				super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(BASE) ? WITH_BASE : POLE;
	}
}

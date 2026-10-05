package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.PlacementBlockMethod;
import dev.xkmc.l2modularblock.mult.ShapeUpdateBlockMethod;
import dev.xkmc.l2modularblock.mult.SurviveBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

/**
 * 摆在魔法道具工作台桌面上的装饰（缝纫机、魔药瓶）。
 *
 * <p>桌面就是工作块的正上方一格，所以支撑判定等于"下方是工作台"。悬空或台子被拆掉时会掉回空气。
 * 朝向来自 {@link BlockTemplates#HORIZONTAL}，默认朝向玩家，再按 {@code placementTurn} 顺时针多转几度。
 */
public class TableDecoBlock implements CreateBlockStateBlockMethod, DefaultStateBlockMethod,
		PlacementBlockMethod, SurviveBlockMethod, ShapeUpdateBlockMethod, ShapePathFindBlockMethod {

	private final VoxelShape[] shapes;
	/** 摆放时在"背对玩家"之外再顺时针转多少度。 */
	private final int placementTurn;

	/**
	 * @param shapes 朝北的碰撞盒按 {@link MagicTableBlock#allFaces} 转出的四个朝向，
	 *               按 {@link Direction#get2DDataValue()} 索引
	 * @param placementTurn 摆放时再顺时针转的度数
	 */
	public TableDecoBlock(VoxelShape[] shapes, int placementTurn) {
		this.shapes = shapes;
		this.placementTurn = placementTurn;
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(BlockTemplates.HORIZONTAL_FACING, Direction.NORTH);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockState def, BlockPlaceContext context) {
		if (def == null) return null;
		// BlockTemplates.HORIZONTAL 已经把朝向设成背对玩家，这里只补上要的那点旋转。
		Direction facing = def.getValue(BlockTemplates.HORIZONTAL_FACING);
		for (int i = 0; i < placementTurn / 90; i++) facing = facing.getClockWise();
		return def.setValue(BlockTemplates.HORIZONTAL_FACING, facing);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return MagicTableBlock.isTable(level.getBlockState(pos.below()));
	}

	@Override
	public BlockState updateShape(Block self, BlockState current, BlockState old, Direction from,
			BlockState source, LevelAccessor level, BlockPos pos, BlockPos sourcePos) {
		if (from == Direction.DOWN && !MagicTableBlock.isTable(source)) return Blocks.AIR.defaultBlockState();
		return current;
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return shapes[state.getValue(BlockTemplates.HORIZONTAL_FACING).get2DDataValue()];
	}

	/**
	 * 装饰只有朝向这一个状态，交给 {@code horizontalBlock} 出四个朝向的变体。
	 *
	 * @param model 装饰的自定义模型，如 {@code custom/deco/sewing_machine}
	 */
	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name, String model) {
		pvd.horizontalBlock(block, pvd.models().getBuilder("block/" + name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc(model)))
				.texture("all", pvd.modLoc("block/deco/magic_table"))
				.texture("particle", pvd.modLoc("block/deco/magic_table"))
				.renderType("cutout"));
	}

}
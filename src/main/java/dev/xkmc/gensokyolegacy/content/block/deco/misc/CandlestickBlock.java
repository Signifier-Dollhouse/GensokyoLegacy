package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.ShapeUpdateBlockMethod;
import dev.xkmc.l2modularblock.mult.SurviveBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

/**
 * 烛台。落地摆件，底下要能站东西；点起来当光源，亮度跟原版蜡烛一致。
 *
 * <p>模型来自 32×32 的单块贴图，四面基本对称，朝向状态只用来转模型角度。
 */
public class CandlestickBlock implements CreateBlockStateBlockMethod, DefaultStateBlockMethod,
		SurviveBlockMethod, ShapeUpdateBlockMethod, ShapeBlockMethod {

	/** 朝向由 {@link BlockTemplates#HORIZONTAL} 加上，这里只引用它的属性。 */
	public static final DirectionProperty FACING = BlockTemplates.HORIZONTAL_FACING;

	/**
	 * 底盘 y 0..1、杆身 y 1..8、烛台碗 y 9..13。长轴 x 比短轴 z 宽，所以朝向转了看得出来。
	 */
	public static final VoxelShape[] SHAPES =
			MagicTableBlock.allFaces(new VoxelBuilder(1, 0, 6, 15, 13, 10));

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(FACING, Direction.NORTH);
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	@Override
	public BlockState updateShape(Block self, BlockState current, BlockState old, Direction from,
			BlockState source, LevelAccessor level, BlockPos pos, BlockPos sourcePos) {
		if (from == Direction.DOWN && !source.isFaceSturdy(level, sourcePos, Direction.UP)) {
			return Blocks.AIR.defaultBlockState();
		}
		return current;
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(FACING).get2DDataValue()];
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		pvd.horizontalBlock(block,
				pvd.models().getBuilder("block/" + name)
						.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/candlestick")))
						.texture("all", pvd.modLoc("block/deco/candlestick"))
						.texture("particle", pvd.modLoc("block/deco/candlestick"))
						.renderType("cutout"));
	}

}
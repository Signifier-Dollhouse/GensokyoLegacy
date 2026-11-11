package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

public class PlaqueBlock implements ShapePathFindBlockMethod, DefaultStateBlockMethod, PlacementBlockMethod,
		SurviveBlockMethod, ShapeUpdateBlockMethod, CreateBlockStateBlockMethod {

	public static final BooleanProperty ALTERNATE = BooleanProperty.create("alternate");
	private static final VoxelShape[] SHAPES = MagicTableBlock.allFaces(new VoxelBuilder(4, 0, 15, 12, 16, 16));

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ALTERNATE);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(BlockTemplates.HORIZONTAL_FACING, Direction.NORTH).setValue(ALTERNATE, false);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(BlockTemplates.HORIZONTAL_FACING).get2DDataValue()];
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(BlockTemplates.HORIZONTAL_FACING);
		BlockPos wall = pos.relative(facing.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockState state, BlockPlaceContext context) {
		return canSurvive(state, context.getLevel(), context.getClickedPos().above()) ? state : null;
	}

	@Override
	public BlockState updateShape(Block block, BlockState current, BlockState old, Direction from,
								  BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (current.is(block) && from == current.getValue(BlockTemplates.HORIZONTAL_FACING).getOpposite()
				&& !canSurvive(current, level, pos)) {
			return Blocks.AIR.defaultBlockState();
		}
		return current;
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		var base = pvd.models().getBuilder("block/" + name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/locust_plaque")))
				.texture("all", pvd.modLoc("block/deco/locust_plaque_2"))
				.renderType("cutout");
		var alternate = pvd.models().getBuilder("block/" + name + "_alternate")
				.parent(base)
				.texture("all", pvd.modLoc("block/deco/locust_plaque_3"))
				.renderType("cutout");
		var empty = pvd.models().getBuilder("block/" + name + "_top")
				.texture("particle", pvd.modLoc("block/deco/locust_plaque_2"));
		pvd.horizontalBlock(block, state -> state.getValue(BlockStateProperties.HALF) == Half.TOP
				? empty : state.getValue(ALTERNATE) ? alternate : base);
	}

	public static void buildItemModel(RegistrateItemModelProvider pvd, String name) {
		var parent = new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/locust_plaque_item"));
		var alternate = pvd.getBuilder(name + "_alternate")
				.parent(parent).texture("all", pvd.modLoc("block/deco/locust_plaque_3")).renderType("cutout");
		pvd.getBuilder(name).parent(parent).texture("all", pvd.modLoc("block/deco/locust_plaque_2"))
				.renderType("cutout")
				.override().predicate(PlaqueItem.ALTERNATE_MODEL, 1).model(alternate).end();
	}

	public static void genLoot(RegistrateBlockLootTables pvd, DelegateBlock block) {
		var helper = new LootHelper(pvd);
		var pool = LootPool.lootPool().add(helper.item(block.asItem())
				.when(helper.enumState(block, BlockStateProperties.HALF, Half.BOTTOM))
				.apply(CopyBlockState.copyState(block).copy(ALTERNATE)));
		pvd.add(block, LootTable.lootTable().withPool(pvd.applyExplosionCondition(block, pool)));
	}
}

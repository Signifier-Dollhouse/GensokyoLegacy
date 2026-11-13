package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

/**
 * 画卷。模型高过 y16，所以摆两格：下半格画整个模型，上半格不画东西，两半靠 {@code DoubleBlockImpl} 绑在一起。
 * 画卷贴着挂它的那面墙，只有朝向轴上一格厚。
 */
public class ScrollBlock implements ShapePathFindBlockMethod, DefaultStateBlockMethod, PlacementBlockMethod,
		SurviveBlockMethod, ShapeUpdateBlockMethod {

	/**
	 * 朝向由 {@link BlockTemplates#HORIZONTAL} 加上，这里只引用它的属性。
	 */
	public static final DirectionProperty FACING = BlockTemplates.HORIZONTAL_FACING;

	/**
	 * 上下两半由 {@code DoubleBlockImpl} 加上，这里只引用它的属性。
	 */
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	/**
	 * 画心 x1..15、满高，贴着墙面那 1px；上下两半同一形状。
	 */
	public static final VoxelShape[] SHAPES = MagicTableBlock.allFaces(
			new VoxelBuilder(1, 0, 15, 15, 16, 16));

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(HALF, Half.BOTTOM).setValue(FACING, Direction.NORTH);
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPES[state.getValue(FACING).get2DDataValue()];
	}

	/**
	 * 画卷背面贴墙，墙那一面要能承托；两半都要各自检查，所以墙只砌一格高时上半格挂不住。
	 */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction back = state.getValue(FACING).getOpposite();
		BlockPos wall = pos.relative(back);
		return level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
	}

	/**
	 * 上半格也要有墙可贴：{@code DoubleBlockImpl} 只查上方能不能放，这里补查那面墙。
	 */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockState state, BlockPlaceContext context) {
		Direction facing = state.getValue(FACING);
		BlockPos wall = context.getClickedPos().above().relative(facing.getOpposite());
		var level = context.getLevel();
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing) ? state : null;
	}

	/**
	 * 墙拆掉时连上半格一起没；上半格的移除由 {@code DoubleBlockImpl} 先处理，这里只认还在的方块。
	 */
	@Override
	public BlockState updateShape(Block self, BlockState selfCurrent, BlockState selfOld, Direction from,
								  BlockState sourceState, LevelAccessor level, BlockPos selfPos, BlockPos sourcePos) {
		if (!selfCurrent.is(self)) return selfCurrent;
		if (from == selfCurrent.getValue(FACING).getOpposite() && !canSurvive(selfCurrent, level, selfPos))
			return Blocks.AIR.defaultBlockState();
		return selfCurrent;
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		var whole = pvd.models().getBuilder("block/" + name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/scroll")))
				.texture("all", pvd.modLoc("block/deco/" + name))
				.texture("particle", pvd.modLoc("block/deco/" + name))
				.renderType("cutout");
		// 上半格只占位置，模型在下半格那一份里
		var empty = pvd.models().getBuilder("block/" + name + "_top")
				.texture("particle", pvd.modLoc("block/deco/" + name));
		pvd.horizontalBlock(block, state -> state.getValue(HALF) == Half.TOP ? empty : whole);
	}

	/**
	 * 两格高的模型直接当物品图标会超出格子，所以单独出一份压扁、居中的图标模型。
	 */
	public static void genItemModel(RegistrateItemModelProvider pvd, String name) {
		pvd.getBuilder(name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/scroll_item")))
				.texture("all", pvd.modLoc("block/deco/" + name))
				.renderType("cutout");
	}

	/**
	 * 上半格是跟着下半格一起没的，只有下半格掉东西，免得拆一次掉两个。
	 */
	public static void genLoot(RegistrateBlockLootTables pvd, DelegateBlock block) {
		var helper = new LootHelper(pvd);
		var pool = LootPool.lootPool().add(helper.item(block.asItem())
				.when(helper.enumState(block, HALF, Half.BOTTOM)));
		pvd.add(block, LootTable.lootTable().withPool(pvd.applyExplosionCondition(block, pool)));
	}

}

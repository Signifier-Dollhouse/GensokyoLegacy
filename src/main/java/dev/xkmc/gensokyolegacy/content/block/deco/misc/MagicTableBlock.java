package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.mult.OnReplacedBlockMethod;
import dev.xkmc.l2modularblock.mult.PlacementBlockMethod;
import dev.xkmc.l2modularblock.mult.SetPlacedByBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import org.jetbrains.annotations.Nullable;

/**
 * 魔法道具工作台。横向两格长的桌子，用 {@link #ORIGIN} 区分这一格是左半还是右半：
 * 放下时把自己当成左半，在右手边补出另一半，两半各自带各自的模型。
 *
 * <p>桌面就是本方块上面那一格，装饰（{@link TableDecoBlock}）直接摆在上面，所以这里不用为装饰留状态。
 */
public class MagicTableBlock implements CreateBlockStateBlockMethod, DefaultStateBlockMethod,
		PlacementBlockMethod, SetPlacedByBlockMethod, OnReplacedBlockMethod, ShapePathFindBlockMethod {

	public static final BooleanProperty ORIGIN = BooleanProperty.create("origin");
	/** 朝向由 {@link BlockTemplates#HORIZONTAL} 提供，不能自己再加一个同名的。 */
	public static final DirectionProperty FACING = BlockTemplates.HORIZONTAL_FACING;

	/** 桌面：整格一层的板，y 15..16。四面都对称，转不转一样。 */
	public static final VoxelShape TOP = Block.box(0, 15, 0, 16, 16, 16);

	private static final VoxelShape[] LEFT_SHAPES = allFaces(
			new VoxelBuilder(0, 11, 1, 16, 15, 16),
			new VoxelBuilder(0, 0, 1, 1, 11, 3),
			new VoxelBuilder(0, 0, 14, 1, 11, 16));

	private static final VoxelShape[] RIGHT_SHAPES = allFaces(
			new VoxelBuilder(0, 11, 1, 16, 15, 16),
			new VoxelBuilder(15, 0, 1, 16, 11, 3),
			new VoxelBuilder(15, 0, 14, 16, 11, 16),
			new VoxelBuilder(2, 0, 5, 15, 8, 15));

	/**
	 * 把朝北的盒子转出四个朝向，按 {@link Direction#get2DDataValue()} 索引，
	 * 正好对上 {@code state.getValue(FACING).get2DDataValue()}。
	 *
	 * <p>转法是 {@link VoxelBuilder#rotateFromNorth}，也就是 {@code LargeChairBlock} 在用的那种。
	 * 它转的是 {@code 180 - toYRot}，而 blockstate 里模型转的是 {@code toYRot + 180}，两者在
	 * 朝北朝南相等、在东西朝向差 180 度；这里按这套既有的配对来。
	 */
	public static VoxelShape[] allFaces(VoxelBuilder... northBoxes) {
		VoxelShape[] ans = new VoxelShape[4];
		for (var dir : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = null;
			for (var box : northBoxes) {
				var turned = box.rotateFromNorth(dir);
				shape = shape == null ? turned : Shapes.or(shape, turned);
			}
			ans[dir.get2DDataValue()] = shape;
		}
		return ans;
	}

	/**
	 * 朝向由 {@link BlockTemplates#HORIZONTAL} 加上，这里只补 {@link #ORIGIN}，重复加同名属性会直接报错。
	 */
	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ORIGIN);
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(ORIGIN, true).setValue(FACING, Direction.NORTH);
	}

	/**
	 * 另一半所在的位置。原点格在朝向的顺时针一侧，补出来的那格在逆时针一侧，
	 * 两半拆哪一边都要能找到对方。
	 */
	private static BlockPos partner(BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FACING);
		return state.getValue(ORIGIN)
				? pos.relative(facing.getClockWise())
				: pos.relative(facing.getCounterClockWise());
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockState def, BlockPlaceContext context) {
		if (def == null) return null;
		// 另一半那格被占了就放不下，免得补格子时覆盖别的方块
		BlockPos other = context.getClickedPos().relative(def.getValue(FACING).getClockWise());
		if (!context.getLevel().getBlockState(other).canBeReplaced(context)) return null;
		return def;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
		if (level.isClientSide() || !state.getValue(ORIGIN)) return;
		level.setBlockAndUpdate(partner(pos, state), state.setValue(ORIGIN, false));
	}

	@Override
	public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (level.isClientSide() || state.is(newState.getBlock())) return;
		BlockPos other = partner(pos, state);
		// 拆掉的那半只掉一次物品，所以另一半只销毁不掉落
		if (level.getBlockState(other).is(state.getBlock())) level.destroyBlock(other, false);
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(ORIGIN)
				? LEFT_SHAPES[state.getValue(FACING).get2DDataValue()]
				: RIGHT_SHAPES[state.getValue(FACING).get2DDataValue()];
	}

	/**
	 * 判定一格是不是桌面用的：左右半块都算，所以装饰可以摆在整张桌子的任意一格上。
	 */
	public static boolean isTable(BlockState state) {
		return state.is(GLFurniture.MAGIC_TABLE.get());
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		var left = pvd.models().getBuilder("block/" + name + "_left")
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/magic_table_left")))
				.texture("all", pvd.modLoc("block/deco/magic_table"))
				.texture("particle", pvd.modLoc("block/deco/magic_table"))
				.renderType("cutout");
		var right = pvd.models().getBuilder("block/" + name + "_right")
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/magic_table_right")))
				.texture("all", pvd.modLoc("block/deco/magic_table"))
				.texture("particle", pvd.modLoc("block/deco/magic_table"))
				.renderType("cutout");

		// 两半拼起来的完整桌子不在这里出模型，物品图标直接用 custom/deco/magic_table_item，
		// 那个文件自带两半几何体和缩放好的坐标。
		MultiPartBlockStateBuilder builder = pvd.getMultipartBuilder(block);
		for (var dir : Direction.Plane.HORIZONTAL) {
			// 和碰撞盒一致：模型转 toYRot + 180
			int yRot = ((int) dir.toYRot() + 180) % 360;
			builder.part().modelFile(left).rotationY(yRot).addModel()
					.condition(ORIGIN, true).condition(FACING, dir).end();
			builder.part().modelFile(right).rotationY(yRot).addModel()
					.condition(ORIGIN, false).condition(FACING, dir).end();
		}
	}

	public static void genItemModel(RegistrateItemModelProvider pvd, String name) {
		pvd.getBuilder(name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/magic_table_item")))
				.texture("all", pvd.modLoc("block/deco/magic_table"))
				.renderType("cutout");
	}

}
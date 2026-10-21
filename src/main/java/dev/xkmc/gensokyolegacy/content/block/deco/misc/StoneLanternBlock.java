package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

/**
 * 石灯笼。纯装饰，不发光,顶上的笠只是块石头。
 * 模型高过 y16,所以摆两格:下半格画整个模型,上半格不画东西,两半靠 {@code DoubleBlockImpl} 绑在一起。
 */
public class StoneLanternBlock implements ShapePathFindBlockMethod {

	/**
	 * 上下两半由 {@code DoubleBlockImpl} 加上,这里只引用它的属性。
	 */
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	/**
	 * 下半格:底座和石柱。
	 */
	public static final VoxelShape BOTTOM_SHAPE = Shapes.or(
			Block.box(0, 0, 0, 16, 2, 16),
			Block.box(5.5, 2, 5.5, 10.5, 16, 10.5));

	/**
	 * 上半格:柱顶、中台、火袋、笠和宝珠。笠的四面是斜的,碰撞盒按外接矩形算。
	 */
	public static final VoxelShape TOP_SHAPE = Shapes.or(
			Block.box(5.5, 0, 5.5, 10.5, 2, 10.5),
			Block.box(4, 0, 4, 12, 1.5, 12),
			Block.box(2, 1.5, 2, 14, 3, 14),
			Block.box(4, 3, 4, 12, 9, 12),
			Block.box(0.4, 8.5, 0.5, 15.6, 10, 15.6),
			Block.box(1.5, 8, 5, 6.9, 13.5, 11),
			Block.box(9.1, 8, 5, 14.5, 13.5, 11),
			Block.box(5, 8, 1.5, 11, 13.5, 6.9),
			Block.box(5, 8, 9.1, 11, 13.5, 14.5),
			Block.box(5, 11.8, 5, 11, 13.5, 11),
			Block.box(6.6, 13.1, 7, 9.4, 15.9, 9));

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(HALF) == Half.TOP ? TOP_SHAPE : BOTTOM_SHAPE;
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		var whole = pvd.models().getBuilder("block/" + name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/stone_lantern")))
				.texture("all", pvd.modLoc("block/deco/" + name))
				.texture("particle", pvd.modLoc("block/deco/" + name))
				.renderType("cutout");
		// 上半格只占位置,模型在下半格那一份里
		var empty = pvd.models().getBuilder("block/" + name + "_top")
				.texture("particle", pvd.modLoc("block/deco/" + name));
		pvd.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
				.modelFile(state.getValue(HALF) == Half.TOP ? empty : whole).build());
	}

	/**
	 * 上半格是跟着下半格一起没的,只有下半格掉东西,免得拆一次掉两个。
	 */
	public static void genLoot(RegistrateBlockLootTables pvd, DelegateBlock block) {
		var helper = new LootHelper(pvd);
		var pool = LootPool.lootPool().add(helper.item(block.asItem())
				.when(helper.enumState(block, HALF, Half.BOTTOM)));
		pvd.add(block, LootTable.lootTable().withPool(pvd.applyExplosionCondition(block, pool)));
	}

}

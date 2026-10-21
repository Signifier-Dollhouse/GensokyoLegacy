package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
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
 * 荒原小墓碑。底下整格是石台,北边横一道矮木栏,石板立在南边。
 * 模型高过 y16,所以摆两格:下半格画整个模型,上半格不画东西,两半靠 {@code DoubleBlockImpl} 绑在一起。
 */
public class WastelandGravestoneBlock implements ShapePathFindBlockMethod, DefaultStateBlockMethod {

	/** 朝向由 {@link BlockTemplates#HORIZONTAL} 加上，这里只引用它的属性。 */
	public static final DirectionProperty FACING = BlockTemplates.HORIZONTAL_FACING;

	/** 上下两半由 {@code DoubleBlockImpl} 加上，这里只引用它的属性。 */
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	/** 石台、矮木栏、石板三截。木栏只挡 y7..9,再往下是三根细柱,可以从缝里穿过去。 */
	public static final VoxelShape[] BOTTOM_SHAPES =
			MagicTableBlock.allFaces(
					new VoxelBuilder(0, 0, 0, 16, 3, 16),
					new VoxelBuilder(0, 7, 1, 16, 9, 6),
					new VoxelBuilder(2, 3, 11, 14, 16, 14));

	/** 石板原本到 y24,搬进上半格还剩 y0..8。 */
	public static final VoxelShape[] TOP_SHAPES =
			MagicTableBlock.allFaces(new VoxelBuilder(2, 0, 11, 14, 8, 14));

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(HALF, Half.BOTTOM).setValue(FACING, Direction.NORTH);
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		int facing = state.getValue(FACING).get2DDataValue();
		return state.getValue(HALF) == Half.TOP ? TOP_SHAPES[facing] : BOTTOM_SHAPES[facing];
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		var whole = pvd.models().getBuilder("block/" + name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/wasteland_gravestone")))
				.texture("all", pvd.modLoc("block/deco/" + name))
				.texture("particle", pvd.modLoc("block/deco/" + name))
				.renderType("cutout");
		// 上半格只占位置,模型在下半格那一份里
		var empty = pvd.models().getBuilder("block/" + name + "_top")
				.texture("particle", pvd.modLoc("block/deco/" + name));
		pvd.horizontalBlock(block, state -> state.getValue(HALF) == Half.TOP ? empty : whole);
	}

	/** 上半格是跟着下半格一起没的,只有下半格掉东西,免得拆一次掉两个。 */
	public static void genLoot(RegistrateBlockLootTables pvd, DelegateBlock block) {
		var helper = new LootHelper(pvd);
		var pool = LootPool.lootPool().add(helper.item(block.asItem())
				.when(helper.enumState(block, HALF, Half.BOTTOM)));
		pvd.add(block, LootTable.lootTable().withPool(pvd.applyExplosionCondition(block, pool)));
	}

}

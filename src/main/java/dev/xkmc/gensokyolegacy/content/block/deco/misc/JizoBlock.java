package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.core.DelegateBlock;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

/**
 * 地藏。四种配色共用 {@code custom/deco/jizo} 这一个模型,只有围兜和前面的木牌换贴图。
 * 模型高过 y16,所以摆两格:下半格画整个模型,上半格不画东西,两半靠 {@code DoubleBlockImpl} 绑在一起。
 * 上半格的碰撞盒分两种:戴斗笠的两款要连宽檐一起挡,不戴的两款只剩一个头。
 */
public class JizoBlock implements ShapePathFindBlockMethod, DefaultStateBlockMethod {

	/** 朝向由 {@link BlockTemplates#HORIZONTAL} 加上，这里只引用它的属性。 */
	public static final DirectionProperty FACING = BlockTemplates.HORIZONTAL_FACING;

	/** 上下两半由 {@code DoubleBlockImpl} 加上，这里只引用它的属性。 */
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	/** 底座 x z 都是 2..14。上下对称,朝向转了碰撞盒也一样。 */
	public static final VoxelShape BOTTOM_SHAPE = Block.box(2, 0, 2, 14, 16, 14);

	/** 不戴帽的两款:上半格只有头,x z 跟下半格一样,高度取模型上 y17..21 那截。 */
	public static final VoxelShape HEAD_TOP_SHAPE = Block.box(2, 0, 2, 14, 5, 14);

	/** 戴斗笠的两款:上半格是宽檐加帽顶,宽檐撑满整格。 */
	public static final VoxelShape HAT_TOP_SHAPE = Shapes.or(
			Block.box(0, 0, 0, 16, 5, 16),
			Block.box(2.5, 5, 2.5, 13.5, 7, 13.5));

	/** 戴斗笠的配色帽檐更大,上半格的碰撞盒跟着换一种。 */
	private final VoxelShape topShape;

	public JizoBlock(VoxelShape topShape) {
		this.topShape = topShape;
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state.setValue(HALF, Half.BOTTOM).setValue(FACING, Direction.NORTH);
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(HALF) == Half.TOP ? topShape : BOTTOM_SHAPE;
	}

	public static void buildStates(Block block, RegistrateBlockstateProvider pvd, String name) {
		var whole = pvd.models().getBuilder("block/" + name)
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/deco/jizo")))
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

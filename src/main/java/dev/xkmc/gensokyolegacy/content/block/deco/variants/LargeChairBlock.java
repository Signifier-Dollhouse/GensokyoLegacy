package dev.xkmc.gensokyolegacy.content.block.deco.variants;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.l2core.serial.loot.LootHelper;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.core.VoxelBuilder;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/**
 * Two block large chair: the seat and legs sit on the lower half, the backrest
 * and armrests on the upper one, both halves carrying their own geometry so the
 * whole chair is lit as normal blocks. The seat interaction lives on the lower
 * half only, the pos the seat entity is bound to.
 */
public class LargeChairBlock implements CreateBlockStateBlockMethod, DefaultStateBlockMethod, ShapePathFindBlockMethod {

	public static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 12, 15);

	/**
	 * Backrest posts, back panel and armrests, mirroring
	 * {@code custom/furniture/wooden_large_chair_top.json}, rotated to match the
	 * facing the model is rotated to. Indexed by facing 2D data value.
	 */
	public static final VoxelShape[] TOP_SHAPES = new VoxelShape[4];

	static {
		for (int i = 0; i < 4; i++) {
			Direction dir = Direction.from2DDataValue(i);
			TOP_SHAPES[i] = Shapes.or(
					new VoxelBuilder(1, 0, 13, 3, 14, 15).rotateFromNorth(dir),
					new VoxelBuilder(13, 0, 13, 15, 14, 15).rotateFromNorth(dir),
					new VoxelBuilder(3, 0, 13, 13, 14, 15).rotateFromNorth(dir),
					new VoxelBuilder(2, 0, 5, 3, 1, 13).rotateFromNorth(dir),
					new VoxelBuilder(14, 0, 5, 15, 1, 13).rotateFromNorth(dir),
					new VoxelBuilder(1, 0, 3, 3, 1, 5).rotateFromNorth(dir),
					new VoxelBuilder(13, 0, 3, 15, 1, 5).rotateFromNorth(dir));
		}
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(HALF) == Half.TOP ? TOP_SHAPES[state.getValue(HORIZONTAL_FACING).get2DDataValue()] : SHAPE;
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state;
	}

	public static void buildStates(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd) {
		buildStates(ctx, pvd, ctx.getName().replace("_large_chair", ""));
	}

	/**
	 * @param padPrefix name prefix of the pad models, the wood name for the
	 *                  per-wood chairs and the block name for the themed one
	 */
	public static void buildStates(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd, String padPrefix) {
		String wood = "block/wood/" + ctx.getName();

		var bottom = pvd.models().getBuilder("block/" + ctx.getName())
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/wooden_large_chair_bottom")))
				.texture("all", pvd.modLoc(wood))
				.texture("particle", pvd.mcLoc("block/birch_planks"))
				.renderType("cutout");

		var top = pvd.models().getBuilder("block/" + ctx.getName() + "_top")
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/wooden_large_chair_top")))
				.texture("all", pvd.modLoc(wood))
				.texture("particle", pvd.mcLoc("block/birch_planks"))
				.renderType("cutout");

		CoverableImpl.buildChairStates(pvd, padPrefix, wood);
		genFullModel(pvd, ctx.getName());
		pvd.horizontalBlock(ctx.get(), state -> {
			if (state.getValue(HALF) == Half.TOP) return top;
			if (state.getValue(CoverableImpl.COLOR) == CoverableImpl.Color.NONE) return bottom;
			var col = state.getValue(CoverableImpl.COLOR);
			String suffix = col == CoverableImpl.Color.BASE ? "pad" : col.getSerializedName() + "_pad";
			return new ModelFile.UncheckedModelFile(pvd.modLoc("block/" + padPrefix + "_" + suffix));
		});
	}

	/**
	 * Whole chair geometry, unused by the block and only kept as item icon parent:
	 * the item model inherits the item display transforms from it.
	 */
	public static void genFullModel(RegistrateBlockstateProvider pvd, String name) {
		pvd.models().getBuilder("block/" + name + "_full")
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/wooden_large_chair")))
				.texture("all", pvd.modLoc("block/wood/" + name))
				.texture("particle", pvd.mcLoc("block/birch_planks"))
				.renderType("cutout");
	}

	public static void genItemModel(DataGenContext<Item, BlockItem> ctx, RegistrateItemModelProvider pvd) {
		pvd.withExistingParent(ctx.getName(), pvd.modLoc("block/" + ctx.getName() + "_full"));
	}

	public static void genLoot(RegistrateBlockLootTables pvd, DelegateBlock block) {
		pvd.add(block, CoverableImpl.loot(pvd, block, halfOnly(pvd, block)));
	}

	private static LootItemCondition.Builder halfOnly(RegistrateBlockLootTables pvd, Block block) {
		return new LootHelper(pvd).enumState(block, HALF, Half.BOTTOM);
	}

}

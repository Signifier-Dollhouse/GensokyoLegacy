package dev.xkmc.gensokyolegacy.content.block.deco.variants;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.mult.CreateBlockStateBlockMethod;
import dev.xkmc.l2modularblock.mult.DefaultStateBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class LargeChairBlock implements ShapeBlockMethod, CreateBlockStateBlockMethod, DefaultStateBlockMethod {

	public static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
	}

	@Override
	public BlockState getDefaultState(BlockState state) {
		return state;
	}

	public static void buildStates(DataGenContext<Block, DelegateBlock> ctx, RegistrateBlockstateProvider pvd) {
		String woodName = ctx.getName().replace("_large_chair", "");
		var chair = pvd.models().getBuilder("block/" + ctx.getName())
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/wooden_large_chair")))
				.texture("all", pvd.modLoc("block/wood/" + ctx.getName()))
				.texture("particle", pvd.mcLoc("block/birch_planks"))
				.renderType("cutout");

		ChairPadImpl.buildStates(pvd, woodName, "block/wood/" + ctx.getName());
		pvd.horizontalBlock(ctx.get(), state -> {
			if (state.getValue(ChairPadImpl.COLOR) == ChairPadImpl.Color.NONE) return chair;
			var col = state.getValue(ChairPadImpl.COLOR);
			String suffix = col == ChairPadImpl.Color.BASE ? "pad" : col.getSerializedName() + "_pad";
			return new ModelFile.UncheckedModelFile(pvd.modLoc("block/" + woodName + "_" + suffix));
		});
	}

	public static void genLoot(RegistrateBlockLootTables pvd, DelegateBlock block) {
		pvd.add(block, ChairPadImpl.loot(pvd, block));
	}

}

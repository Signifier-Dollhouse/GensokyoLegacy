package dev.xkmc.gensokyolegacy.content.block.bed;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.jetbrains.annotations.Nullable;

public class HighBedShape implements BedShape {

	public static VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	public void buildStates(DataGenContext<Block, YoukaiBedBlock> ctx, RegistrateBlockstateProvider pvd) {
		pvd.horizontalBlock(ctx.get(), state -> pvd.models().getBuilder(ctx.getName() + "_" + state.getValue(YoukaiBedBlock.PART))
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/bed_high_" + state.getValue(YoukaiBedBlock.PART))))
				.texture("all", pvd.modLoc("block/bed/" + ctx.getName())), 0);
	}

	public void buildItemModel(DataGenContext<Item, BedItem> ctx, RegistrateItemModelProvider pvd) {
		pvd.getBuilder(ctx.getName())
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/bed_high")))
				.texture("all", pvd.modLoc("block/bed/" + ctx.getName()));
	}

}

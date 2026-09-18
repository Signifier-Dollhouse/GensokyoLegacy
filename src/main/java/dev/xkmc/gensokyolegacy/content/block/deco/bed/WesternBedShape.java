package dev.xkmc.gensokyolegacy.content.block.deco.bed;

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

/**
 * Single full-sized model spanning both blocks: rendered entirely by the head part,
 * the foot part renders nothing.
 */
public class WesternBedShape implements BedShape {

	public static VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	public void buildStates(DataGenContext<Block, YoukaiBedBlock> ctx, RegistrateBlockstateProvider pvd) {
		pvd.horizontalBlock(ctx.get(), state -> switch (state.getValue(YoukaiBedBlock.PART)) {
			case HEAD -> pvd.models().getBuilder(ctx.getName() + "_head")
					.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/bed_western")))
					.texture("all", pvd.modLoc("block/bed/" + ctx.getName()));
			case FOOT -> pvd.models().getBuilder(ctx.getName() + "_foot")
					.texture("particle", pvd.modLoc("block/bed/" + ctx.getName()));
		}, 0);
	}

	public void buildItemModel(DataGenContext<Item, BedItem> ctx, RegistrateItemModelProvider pvd) {
		pvd.getBuilder(ctx.getName())
				.parent(new ModelFile.UncheckedModelFile(pvd.modLoc("custom/furniture/bed_western")))
				.texture("all", pvd.modLoc("block/bed/" + ctx.getName()));
	}

}

package dev.xkmc.gensokyolegacy.content.block.deco.bed;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public interface BedShape extends ShapeBlockMethod {

	/**
	 * Y offset from the head block origin to the sleeping entity origin.
	 * Vanilla beds use 0.6875 (9/16 mattress top + 2/16 gap).
	 */
	double sleepOffset();

	void buildStates(DataGenContext<Block, YoukaiBedBlock> ctx, RegistrateBlockstateProvider pvd);

	void buildItemModel(DataGenContext<Item, BedItem> ctx, RegistrateItemModelProvider pvd);
}

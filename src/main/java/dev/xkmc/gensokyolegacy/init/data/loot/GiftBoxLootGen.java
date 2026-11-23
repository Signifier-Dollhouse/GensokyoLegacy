package dev.xkmc.gensokyolegacy.init.data.loot;

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * The gift box rolls from {@link GLTagGen#MORICHIKA_GIFTS}, the gift subset of the shelf stock, so it
 * cannot contain the tools and curios Morichika also sells.
 */
public class GiftBoxLootGen {

	public static void genLoot(RegistrateLootTableProvider pvd) {
		pvd.addLootAction(LootContextParamSets.GIFT, c -> c.accept(GLItems.GIFT_BOX_LOOT,
				LootTable.lootTable().withPool(LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1))
						.add(TagEntry.expandTag(GLTagGen.MORICHIKA_GIFTS)))));
	}

}

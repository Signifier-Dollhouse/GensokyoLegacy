package dev.xkmc.gensokyolegacy.content.block.deco.shelf;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Shop offer range for an item sold on shelves.
 * Items without this datamap entry are sold at stock 1 and price 1.
 * Items with a non-empty advancement list are only stocked once a player has earned one of them.
 */
public record MorichikaOfferData(int minPrice, int maxPrice, int minStock, int maxStock, List<ResourceLocation> advancements) {

	public MorichikaOfferData(int minPrice, int maxPrice, int minStock, int maxStock) {
		this(minPrice, maxPrice, minStock, maxStock, List.of());
	}

	public int rollPrice(RandomSource rand) {
		if (maxPrice <= minPrice) return minPrice;
		return minPrice + rand.nextInt(maxPrice - minPrice + 1);
	}

	public int rollStock(RandomSource rand) {
		if (maxStock <= minStock) return minStock;
		return minStock + rand.nextInt(maxStock - minStock + 1);
	}

	public boolean isAvailable(ServerPlayer player) {
		if (advancements == null || advancements.isEmpty()) return true;
		for (var id : advancements) {
			var adv = player.server.getAdvancements().get(id);
			if (adv != null && player.getAdvancements().getOrStartProgress(adv).isDone())
				return true;
		}
		return false;
	}

}

package dev.xkmc.gensokyolegacy.content.block.deco.shelf;

import net.minecraft.util.RandomSource;

/**
 * Shop offer range for an item sold on shelves.
 * Items without this datamap entry are sold at stock 1 and price 1.
 */
public record MorichikaOfferData(int minPrice, int maxPrice, int minStock, int maxStock) {

	public int rollPrice(RandomSource rand) {
		if (maxPrice <= minPrice) return minPrice;
		return minPrice + rand.nextInt(maxPrice - minPrice + 1);
	}

	public int rollStock(RandomSource rand) {
		if (maxStock <= minStock) return minStock;
		return minStock + rand.nextInt(maxStock - minStock + 1);
	}

}

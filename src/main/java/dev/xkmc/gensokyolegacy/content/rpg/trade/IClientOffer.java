package dev.xkmc.gensokyolegacy.content.rpg.trade;

import dev.xkmc.gensokyolegacy.content.rpg.core.IngredientEntry;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IClientOffer {

	static IClientOffer resolve(TradeOffer offer) {
		if (offer.isSellOffer()) return new SellOffer(offer);
		return new StockOffer(offer);
	}

	boolean isSell();

	ItemStack currency();

	ItemStack result();

	List<IngredientEntry> ingredients();

	record StockOffer(TradeOffer offer) implements IClientOffer {

		@Override
		public boolean isSell() {
			return true;
		}

		@Override
		public ItemStack currency() {
			return offer.result().copy();
		}

		@Override
		public ItemStack result() {
			return offer.result();
		}

		@Override
		public List<IngredientEntry> ingredients() {
			return offer.ingredients();
		}

	}

	record SellOffer(TradeOffer offer) implements IClientOffer {

		@Override
		public boolean isSell() {
			return false;
		}

		@Override
		public ItemStack currency() {
			for (var e : offer.ingredients()) {
				var items = e.ingredient().getItems();
				if (items.length == 1 && items[0].is(GLTagGen.CURRENCY))
					return items[0].copyWithCount(e.count());
			}
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack result() {
			return offer.result();
		}

		@Override
		public List<IngredientEntry> ingredients() {
			return offer.ingredients();
		}

	}

}

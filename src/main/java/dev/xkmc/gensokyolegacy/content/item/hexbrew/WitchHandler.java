package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import com.tterrag.registrate.builders.ItemBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2library.base.L2Registrate;
import dev.xkmc.gensokyolegacy.util.PotionData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;

public record WitchHandler(boolean throwable) implements AbstractPotionHandler {

	@Override
	public boolean isThrowable() {
		return throwable;
	}

	@Override
	public boolean isDrinkable() {
		return !throwable;
	}

	@Override
	public boolean copiesPotion() {
		return true;
	}

	@Override
	public boolean potionTexture() {
		return true;
	}

	@Override
	public PotionData defaultPotion() {
		return PotionData.of(throwable ? Potions.POISON : Potions.SWIFTNESS);
	}

	@Override
	public void build(ItemBuilder<HexBrewBottleItem, L2Registrate> builder) {
		builder.color(() -> () -> WitchHandler::getTintColor);
		builder.tab(GLItems.TAB.key(), (x, m) ->
				m.accept(withDefaultDataForDisplay(x.get().getDefaultInstance())));
	}

	public ItemStack withDefaultDataForDisplay(ItemStack stack) {
		defaultPotion().write(stack);
		return stack;
	}

	public static int getTintColor(ItemStack stack, int layer) {
		if (layer != 1) return -1;
		var potion = PotionData.read(stack);
		if (potion.isEmpty()) return -1;
		return potion.getColor();
	}

}

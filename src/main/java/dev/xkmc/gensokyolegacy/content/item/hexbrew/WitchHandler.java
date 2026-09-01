package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import com.tterrag.registrate.builders.ItemBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.MutableDataComponentHolder;

import java.util.List;

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
	public List<DataComponentType<?>> getComponentsToCopy() {
		return List.of(DataComponents.POTION_CONTENTS);
	}

	@Override
	public boolean potionTexture() {
		return true;
	}

	public <T extends MutableDataComponentHolder> T withDefaultDataForDisplay(T stack) {
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(throwable ? Potions.POISON : Potions.SWIFTNESS));
		return stack;
	}

	@Override
	public void build(ItemBuilder<HexBrewBottleItem, L2Registrate> builder) {
		builder.color(() -> () -> WitchHandler::getTintColor);
		builder.tab(GLItems.TAB.key(), (x, m) ->
				m.accept(withDefaultDataForDisplay(x.get().getDefaultInstance())));
	}

	public static int getTintColor(ItemStack stack, int layer) {
		if (layer != 1) return -1;
		var potion = stack.get(DataComponents.POTION_CONTENTS);
		if (potion == null) return -1;
		return potion.getColor();
	}

}

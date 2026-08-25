package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.world.item.ItemStack;

public record GiftItemData(int favor, int cooldown, GiftType type) {

	public int getFavor(ItemStack stack, YoukaiEntity self) {
		var pref = GLMeta.GIFT_PREFERENCE.get(self.registryAccess(), self.getType().builtInRegistryHolder());
		if (pref == null) return favor;
		if (pref.override().containsKey(stack.getItem()))
			return (int) (pref.override().get(stack.getItem()) * favor);
		return (int) (pref.preference().getOrDefault(type, 1d) * favor);
	}

}

package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.world.item.ItemStack;

/**
 * @see "doc/design/gift.md"
 */
public record GiftItemData(int favor, int cooldown, GiftType type) {

	/**
	 * What the item is worth to this character: its base favor times the factor she has for this
	 * category, or 0 when she will not take it at all.
	 * <p>
	 * Two tiers, each overriding only what the one above leaves out: the per-item override, then the
	 * category she has an opinion about in her own {@code gift_preference} datamap entry, then
	 * {@link GiftType#neutral()} for a category nobody has stated an opinion about.
	 */
	public int getFavor(ItemStack stack, YoukaiEntity self) {
		var pref = GLMeta.GIFT_PREFERENCE.get(self.registryAccess(), self.getType().builtInRegistryHolder());
		if (pref == null) return (int) (type.neutral() * favor);
		if (pref.override().containsKey(stack.getItem()))
			return (int) (pref.override().get(stack.getItem()) * favor);
		return (int) (pref.preference().getOrDefault(type, type.neutral()) * favor);
	}

}
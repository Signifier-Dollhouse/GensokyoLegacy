package dev.xkmc.gensokyolegacy.content.item.gift;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.world.item.ItemStack;

/**
 * @see "doc/design/gift.md"
 */
public record GiftItemData(int favor, int cooldown, GiftType type) {

	/**
	 * How much she thinks this item is worth to her, as a multiplier on its base {@code favor}: 1.0 is
	 * neutral, above it she is glad to be handed one, 0 is a refusal.
	 * <p>
	 * Two tiers, each overriding only what the one above leaves out: the per-item override, then the
	 * category she has an opinion about in her own {@code gift_preference} datamap entry, then
	 * {@link GiftType#neutral()} for a category nobody has stated an opinion about.
	 * <p>
	 * Returned raw rather than folded into the favor because {@code GiftModule} reads it twice over: the
	 * favor is {@code factor * favor}, and only a factor of exactly 0 turns into the refusal reaction.
	 */
	public double getFactor(ItemStack stack, YoukaiEntity self) {
		var pref = GLMeta.GIFT_PREFERENCE.get(self.registryAccess(), self.getType().builtInRegistryHolder());
		if (pref == null) return type.neutral();
		var single = pref.override().get(stack.getItem());
		if (single != null) return single;
		return pref.preference().getOrDefault(type, type.neutral());
	}

}
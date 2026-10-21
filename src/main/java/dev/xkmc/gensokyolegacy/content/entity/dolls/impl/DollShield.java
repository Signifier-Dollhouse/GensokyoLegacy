package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.MutableDollInventory;
import dev.xkmc.gensokyolegacy.content.item.doll.DollShieldItem;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

/**
 * Reactive off-hand shield block, running through the regular vanilla pipeline — blocking state and
 * wear here, bypass/pierce gating and front-hemisphere checks in vanilla. All shield logic is a
 * default; the entity keeps only the three vanilla overrides, which must call {@code super}.
 */
public interface DollShield extends DollLoadout {

	default boolean hasReadyShield() {
		if (DollShieldItem.isShield(loadout().get(DollSlot.OFF_HAND)) ||
				DollShieldItem.isShield(loadout().get(DollSlot.MAIN_HAND)))
			return asDoll().actions.isShieldReady(asDoll().level().getGameTime());
		return false;
	}

	default void wearShield() {
		MutableDollInventory inv = loadout();
		DollSlot slot = DollSlot.OFF_HAND;
		ItemStack shield = inv.get(slot);
		if (!DollShieldItem.isShield(shield)) {
			slot = DollSlot.MAIN_HAND;
			shield = inv.get(slot);
			if (!DollShieldItem.isShield(shield))
				return;
		}

		// no cooldown for now.
		// asDoll().actions.stampShieldBlock(asDoll().level().getGameTime(), 100);

		// the mini shield has no durability to spend, so it blocks forever
		asDoll().playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + asDoll().level().random.nextFloat() * 0.4F);
		if (shield.getMaxDamage() > 0) {
			int wear = shield.getDamageValue() + 1;
			if (wear >= shield.getMaxDamage()) {
				inv.set(slot, ItemStack.EMPTY);
			} else {
				shield.setDamageValue(wear);
			}
		}
		asDoll().syncLoadoutMirror();
	}

}

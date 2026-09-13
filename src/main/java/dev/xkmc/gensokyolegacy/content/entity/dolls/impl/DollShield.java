package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.MutableDollInventory;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * Reactive off-hand shield block, running through the regular vanilla pipeline —
 * blocking state and wear here, bypass/pierce gating and front-hemisphere checks in
 * vanilla. The entity keeps only the three vanilla overrides (which must call
 * {@code super} and therefore cannot move here); all shield logic is a default.
 */
public interface DollShield extends DollLoadout {

	int SHIELD_BLOCK_COOLDOWN = 100;

	default boolean hasReadyShield() {
		ItemStack shield = asDoll().loadout().get(DollSlot.OFF_HAND);
		if (shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) return false;
		return asDoll().actions.isShieldReady(asDoll().level().getGameTime());
	}

	default boolean isProjectileDamage(DamageSource damageSource) {
		// dolls only block projectiles; everything else resolves in vanilla
		return damageSource.getDirectEntity() instanceof Projectile;
	}

	default void wearShield() {
		MutableDollInventory inv = asDoll().loadout();
		ItemStack shield = inv.get(DollSlot.OFF_HAND);
		if (shield.isEmpty() || !(shield.getItem() instanceof ShieldItem)) return;
		asDoll().actions.stampShieldBlock(asDoll().level().getGameTime(), SHIELD_BLOCK_COOLDOWN);
		int wear = shield.getDamageValue() + 1;
		if (wear >= shield.getMaxDamage()) {
			inv.set(DollSlot.OFF_HAND, ItemStack.EMPTY);
		} else {
			shield.setDamageValue(wear);
		}
		asDoll().syncLoadoutMirror();
	}

}

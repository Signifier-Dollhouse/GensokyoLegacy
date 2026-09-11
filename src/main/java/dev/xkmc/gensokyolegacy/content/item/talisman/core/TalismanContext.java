package dev.xkmc.gensokyolegacy.content.item.talisman.core;

import dev.xkmc.gensokyolegacy.content.item.talisman.pocket.TalismanPocketData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Runtime context for one activated talisman. For a folded talisman worn directly
 * (see {@link FoldedPaperTalisman}) the {@link #pocketStack} is the folded stack
 * itself and the pocket data is {@code null}, so {@link #hurtItem()} mutates the
 * worn stack in place. For a talisman stored inside a pocket the folded stack is a
 * private copy, and every change made by {@link #hurtItem()} is written back into
 * the pocket's {@link TalismanPocketData} component.
 */
public record TalismanContext(
		LivingEntity target,
		ItemStack pocketStack,
		int index,
		ItemStack foldedStack,
		TalismanPaperItem paper
) {

	private static final String COOLDOWN_KEY = "gensokyolegacy:talisman_cooldowns";

	public void hurtItem() {
		Integer left = GLTalismans.DC_TALISMAN_DURABILITY.get(foldedStack);
		if (left != null) {
			if (left <= 1) {
				foldedStack.shrink(1);
			} else {
				GLTalismans.DC_TALISMAN_DURABILITY.set(foldedStack, left - 1);
			}
		} else if (foldedStack.isDamageableItem()) {
			foldedStack.setDamageValue(foldedStack.getDamageValue() + 1);
			if (foldedStack.getDamageValue() >= foldedStack.getMaxDamage()) {
				foldedStack.shrink(1);
			}
		}
		var data = GLTalismans.DC_TALISMAN_POCKET.get(pocketStack);
		if (data != null) {
			GLTalismans.DC_TALISMAN_POCKET.set(pocketStack, data.withFolded(index, foldedStack));
		}
	}

	public boolean isOnCooldown() {
		if (target instanceof ServerPlayer sp) {
			return sp.getCooldowns().isOnCooldown(paper);
		}
		long cd = target.getPersistentData().getCompound(COOLDOWN_KEY).getLong(cooldownId());
		return target.level().getGameTime() < cd;
	}

	public void addCooldown(int ticks) {
		if (target instanceof ServerPlayer sp) {
			sp.getCooldowns().addCooldown(paper, ticks);
			return;
		}
		CompoundTag tag = target.getPersistentData().getCompound(COOLDOWN_KEY).copy();
		tag.putLong(cooldownId(), target.level().getGameTime() + ticks);
		target.getPersistentData().put(COOLDOWN_KEY, tag);
	}

	private String cooldownId() {
		return BuiltInRegistries.ITEM.getKey(paper).toString();
	}

}
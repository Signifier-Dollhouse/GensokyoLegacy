package dev.xkmc.gensokyolegacy.content.item.talisman;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class BasePaperTalisman extends Item {

	protected final int durability;

	public BasePaperTalisman(Properties p, int durability) {
		super(p);
		this.durability = durability;
	}

	public final void tickTalisman(ItemStack stack, ServerPlayer player) {
		if (player.getCooldowns().isOnCooldown(this))
			return;
		if (!test(player))
			return;
		trigger(stack, player);
	}

	public boolean test(ServerPlayer le) {
		return false;
	}

	public void trigger(ItemStack stack, ServerPlayer le) {

	}

	public boolean onAttacked(ItemStack stack, ServerPlayer sp, DamageData.Attack event) {
		return false;
	}

	public void onDamaged(ItemStack stack, ServerPlayer sp, DamageData.Defence event) {

	}

	protected void applyEffect(ItemStack stack, ServerPlayer le, Holder<MobEffect> eff, int amp) {
		var old = le.getEffect(eff);
		if (old == null || old.getAmplifier() != amp || old.getDuration() <= 20) {
			le.addEffect(new MobEffectInstance(eff, 39, amp, true, false, true));
			hurtItem(stack);
		}
	}

	protected void hurtItem(ItemStack stack) {
		if (stack.isDamageableItem()) {
			stack.setDamageValue(stack.getDamageValue() + 1);
			if (stack.getDamageValue() >= stack.getMaxDamage()) {
				stack.shrink(1);
			}
		}
	}

}

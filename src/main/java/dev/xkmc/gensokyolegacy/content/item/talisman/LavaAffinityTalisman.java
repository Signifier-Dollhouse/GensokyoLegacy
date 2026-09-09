package dev.xkmc.gensokyolegacy.content.item.talisman;

import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LavaAffinityTalisman extends BasePaperTalisman {

	public LavaAffinityTalisman(Properties p) {
		super(p, 180);
	}


	@Override
	public boolean test(ServerPlayer le) {
		return le.isOnFire() || le.isInLava();
	}

	@Override
	public void trigger(ItemStack stack, ServerPlayer le) {
		applyEffect(stack, le, MobEffects.FIRE_RESISTANCE, 1);
	}

	@Override
	public boolean onAttacked(ItemStack stack, ServerPlayer sp, DamageData.Attack event) {
		if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
			trigger(stack, sp);
			return true;
		}
		return super.onAttacked(stack, sp, event);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {

	}

}

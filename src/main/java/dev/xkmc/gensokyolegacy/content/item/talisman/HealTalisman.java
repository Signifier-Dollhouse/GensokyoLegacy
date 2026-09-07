package dev.xkmc.gensokyolegacy.content.item.talisman;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HealTalisman extends BasePaperTalisman {

	public HealTalisman(Properties p) {
		super(p.stacksTo(16));
	}

	@Override
	public boolean test(ServerPlayer le) {
		return le.getHealth() < le.getMaxHealth() && le.isAlive();
	}

	@Override
	public void trigger(ItemStack stack, ServerPlayer le) {
		le.heal(le.getMaxHealth() * 0.3f);
		le.getCooldowns().addCooldown(this, 100);
		hurtItem(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {

	}

}

package dev.xkmc.gensokyolegacy.content.item.talisman;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HydrophobicTalisman extends BasePaperTalisman {

	public HydrophobicTalisman(Properties p) {
		super(p.durability(180));
	}

	@Override
	public boolean test(ServerPlayer le) {
		return le.getAirSupply() < le.getMaxAirSupply() * 0.7f;
	}

	@Override
	public void trigger(ItemStack stack, ServerPlayer le) {
		le.setAirSupply(le.getAirSupply() + 1);
		hurtItem(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {

	}

}

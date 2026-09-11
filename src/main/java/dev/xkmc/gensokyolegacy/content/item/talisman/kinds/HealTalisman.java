package dev.xkmc.gensokyolegacy.content.item.talisman.kinds;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HealTalisman extends TalismanPaperItem {

	public HealTalisman(Properties p, int durability, int color, GLLang.LangEntry name) {
		super(p, durability, color, name);
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
	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {
		list.add(GLLang.Talisman.HEAL.get(30));
		list.add(GLLang.Talisman.EQUIP.get());
	}

}
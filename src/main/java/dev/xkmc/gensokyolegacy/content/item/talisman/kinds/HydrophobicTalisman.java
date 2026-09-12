package dev.xkmc.gensokyolegacy.content.item.talisman.kinds;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanContext;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HydrophobicTalisman extends TalismanPaperItem {

	public HydrophobicTalisman(Properties p, int durability, int color, GLLang.LangEntry name) {
		super(p, durability, color, name);
	}

	@Override
	public boolean test(LivingEntity le) {
		return le.getAirSupply() < le.getMaxAirSupply() * 0.7f;
	}

	@Override
	public void trigger(TalismanContext ctx) {
		LivingEntity le = ctx.target();
		le.setAirSupply(le.getAirSupply() + 40);
		ctx.hurtItem();
	}

	@Override
	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {
		list.add(GLLang.Talisman.HYDROPHOBIC.get());
		list.add(GLLang.Talisman.EQUIP.get());
	}

}
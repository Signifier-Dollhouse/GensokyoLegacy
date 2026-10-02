package dev.xkmc.gensokyolegacy.content.item.talisman.kinds;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanContext;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HealTalisman extends TalismanPaperItem {

	public HealTalisman(Properties p, int durability, int color, GLLang.LangEntry name) {
		super(p, durability, color, name);
	}

	@Override
	public boolean test(LivingEntity le) {
		return needsHeal(le);
	}

	/**
	 * What a heal talisman is for: an entity that is alive and missing health.
	 * <p>
	 * Static and paper-free on purpose — a doll host has to answer "is anyone
	 * worth handing a talisman to?" before it has one in hand, and that question
	 * is about the target, not about the paper.
	 */
	public static boolean needsHeal(LivingEntity le) {
		return le.getHealth() < le.getMaxHealth() && le.isAlive();
	}

	@Override
	public void trigger(TalismanContext ctx) {
		LivingEntity le = ctx.target();
		le.heal(le.getMaxHealth() * 0.3f);
		ctx.addCooldown(100);
		ctx.hurtItem();
	}

	@Override
	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {
		list.add(GLLang.Talisman.HEAL.get(30));
		list.add(GLLang.Talisman.EQUIP.get());
	}

}
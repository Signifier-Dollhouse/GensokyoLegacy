package dev.xkmc.gensokyolegacy.content.item.talisman.kinds;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanContext;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SpeedTalisman extends TalismanPaperItem {

	public SpeedTalisman(Properties p, int durability, int color, GLLang.LangEntry name) {
		super(p, durability, color, name);
	}

	@Override
	public boolean test(ServerPlayer le) {
		return le.isSprinting();
	}

	@Override
	public void trigger(TalismanContext ctx) {
		applyEffect(ctx, MobEffects.MOVEMENT_SPEED, 1);
	}

	@Override
	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {
		list.add(GLLang.Talisman.SPEED.get());
		list.add(GLLang.Talisman.EQUIP.get());
	}

}
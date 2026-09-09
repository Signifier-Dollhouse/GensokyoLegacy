package dev.xkmc.gensokyolegacy.content.item.talisman;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SpeedTalisman extends TalismanPaperItem {

	public SpeedTalisman(Properties p) {
		super(p, 180);
	}

	@Override
	public int getColor() {
		return 0x55FF7F;
	}

	@Override
	public String getTexture() {
		return "speed";
	}

	@Override
	public boolean test(ServerPlayer le) {
		return le.isSprinting();
	}

	@Override
	public void trigger(ItemStack stack, ServerPlayer le) {
		applyEffect(stack, le, MobEffects.MOVEMENT_SPEED, 1);
	}

	@Override
	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {
		list.add(GLLang.Talisman.SPEED.get());
		list.add(GLLang.Talisman.EQUIP.get());
	}

}
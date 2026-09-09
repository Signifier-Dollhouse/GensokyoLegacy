package dev.xkmc.gensokyolegacy.content.item.talisman;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LavaAffinityTalisman extends TalismanPaperItem {

	public LavaAffinityTalisman(Properties p) {
		super(p, 180);
	}

	@Override
	public int getColor() {
		return 0xFFB37F;
	}

	@Override
	public String getTexture() {
		return "attack";
	}

	@Override
	public GLLang.LangEntry kindName() {
		return GLLang.Talisman.KIND_LAVA;
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
	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {
		list.add(GLLang.Talisman.LAVA.get());
		list.add(GLLang.Talisman.EQUIP.get());
	}

}
package dev.xkmc.gensokyolegacy.content.item.talisman.kinds;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ShelterTalisman extends TalismanPaperItem {

	public ShelterTalisman(Properties p) {
		super(p, 16);
	}

	@Override
	public int getColor() {
		return 0xFFFFD5;
	}

	@Override
	public String getTexture() {
		return "life";
	}

	@Override
	public GLLang.LangEntry kindName() {
		return GLLang.Talisman.KIND_SHELTER;
	}

	@Override
	public void onDamaged(ItemStack stack, ServerPlayer sp, DamageData.Defence event) {
		if (sp.getCooldowns().isOnCooldown(this)) return;
		event.addDealtModifier(DamageModifier.nonlinearFinal(614, f -> {
			if (f >= event.getTarget().getHealth() * 0.2) {
				sp.getCooldowns().addCooldown(this, 100);
				hurtItem(stack);
				return f * 0.2f;
			}
			return f;
		}, GensokyoLegacy.loc("shelter_talisman")));
	}

	@Override
	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {
		list.add(GLLang.Talisman.SHELTER.get());
		list.add(GLLang.Talisman.EQUIP.get());
	}

}
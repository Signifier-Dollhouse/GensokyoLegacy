package dev.xkmc.gensokyolegacy.content.item.talisman.kinds;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanContext;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LavaAffinityTalisman extends TalismanPaperItem {

	public LavaAffinityTalisman(Properties p, int durability, int color, GLLang.LangEntry name) {
		super(p, durability, color, name);
	}

	@Override
	public boolean test(ServerPlayer le) {
		return le.isOnFire() || le.isInLava();
	}

	@Override
	public void trigger(TalismanContext ctx) {
		applyEffect(ctx, GLEffects.LAVA_AFFINITY, 0);
	}

	@Override
	public boolean onAttacked(TalismanContext ctx, DamageData.Attack event) {
		if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
			trigger(ctx);
			return true;
		}
		return super.onAttacked(ctx, event);
	}

	@Override
	protected void appendTalismanDesc(ItemStack stack, List<Component> list) {
		list.add(GLLang.Talisman.LAVA.get());
		list.add(GLLang.Talisman.EQUIP.get());
	}

}
package dev.xkmc.gensokyolegacy.content.item.talisman;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2damagetracker.contents.attack.DamageData;
import dev.xkmc.l2damagetracker.contents.attack.DamageModifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ShelterTalisman extends BasePaperTalisman {

	public ShelterTalisman(Properties p) {
		super(p);
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

}

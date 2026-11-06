package dev.xkmc.gensokyolegacy.content.item.ghost;

import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.content.spell.item.YuyukoScatterItemSpell;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Casts the butterfly storm: 100 butterflies per tick for 20 ticks, each spiralling away from the
 * caster around the caster's own centre.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
final class ScatterFanHandler implements YuyukoFanModeHandler {

	private static final int COST = 10;

	@Override
	public boolean use(ServerPlayer sp, ItemStack stack, DollAttachment att) {
		if (SpiritualPower.get(stack) < COST) {
			sp.displayClientMessage(GLLang.Ghost.NEED_POWER.get(COST), true);
			return false;
		}
		SpiritualPower.add(stack, -COST);
		SpellContainer.castSpell(sp, YuyukoScatterItemSpell::new, null);
		return true;
	}

}

package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * A dual ghost spends itself in place of its owner.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GhostEventHandlers {

	/**
	 * Runs before the doll ledger's own death pass, so a saved death never parks the remaining
	 * ghosts: canceling the event keeps the rest of the ledger summoned.
	 */
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onPlayerDeath(LivingDeathEvent event) {
		if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer sp)) return;
		DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
		for (GhostEntity ghost : att.summonedGhosts(sp)) {
			if (!ghost.getVariant().handler.wardDeath(sp, ghost, att)) continue;
			event.setCanceled(true);
			return;
		}
	}

}

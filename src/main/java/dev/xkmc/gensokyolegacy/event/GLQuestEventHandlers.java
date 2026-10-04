package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.rpg.network.QuestLootToClient;
import dev.xkmc.gensokyolegacy.content.rpg.reward.LootReader;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.KillTrigger;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GLQuestEventHandlers {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void triggerDeath(LivingDeathEvent event) {
		if (event.getEntity().getKillCredit() instanceof ServerPlayer sp) {
			GLMeta.QUEST.type().getOrCreate(sp).dispatch(sp, new KillTrigger(sp, event.getEntity()));
		}
	}

	/**
	 * Loot tables are not synced, so a client cannot work out a quest's reward on its own; it is
	 * told instead. This event is the one moment a client is known to want a fresh copy of
	 * anything server-held: on joining, and again after a {@code /reload}.
	 */
	@SubscribeEvent
	public static void syncQuestLoot(OnDatapackSyncEvent event) {
		ServerPlayer sp = event.getPlayer();
		if (sp == null) return;
		GensokyoLegacy.HANDLER.toClientPlayer(new QuestLootToClient(LootReader.questRewards(sp.getServer())), sp);
	}

}

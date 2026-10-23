package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * The foodie ghost's second effect: every use tick is spent twice, so eating finishes in half the
 * time. The event fires on both sides, keeping the client's use animation in step.
 *
 * @see "doc/design/doll/stray.md"
 */
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GhostEventHandlers {

	@SubscribeEvent
	public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
		if (!(event.getEntity() instanceof Player player)) return;
		if (!player.hasEffect(GLEffects.FOODIE)) return;
		if (!event.getItem().has(DataComponents.FOOD)) return;
		int duration = event.getDuration();
		if (duration > 1) event.setDuration(duration - 1);
	}

}

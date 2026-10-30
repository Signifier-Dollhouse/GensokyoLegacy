package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.entity.characters.maiden.MaidenEntity;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouHatItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.util.LavaEffectsHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class MobEffectEventHandlers {

	/** Foodie */
	@SubscribeEvent
	public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
		if (!(event.getEntity() instanceof Player player)) return;
		if (!player.hasEffect(GLEffects.FOODIE)) return;
		if (!event.getItem().has(DataComponents.FOOD)) return;
		int duration = event.getDuration();
		if (duration > 1) event.setDuration(duration - 1);
	}

	/** Lava Affinity */
	@SubscribeEvent
	public static void immunityCheck(EntityInvulnerabilityCheckEvent event) {
		if (LavaEffectsHelper.fireImmune(event.getEntity(), event.getSource())) {
			event.setInvulnerable(true);
		}
	}

	/** Koishi */
	@SubscribeEvent
	public static void onAttack(LivingIncomingDamageEvent event) {
		if (event.getSource().getEntity() instanceof LivingEntity le) {
			if (le instanceof Player player) {
				removeKoishi(player);
			}
		}
	}

	/** Koishi */
	@SubscribeEvent
	public static void onTick(PlayerTickEvent.Post event) {
		var e = event.getEntity();
		if (e.getLastHurtMob() instanceof MaidenEntity ||
				e.getLastHurtByMob() instanceof MaidenEntity) {
			removeKoishi(e);
		}
	}

	public static void removeKoishi(LivingEntity le) {
		if (le instanceof Player player) {
			if (player.hasEffect(GLEffects.UNCONSCIOUS)) {
				player.removeEffect(GLEffects.UNCONSCIOUS);
			}
			var hat = GLItems.KOISHI_HAT.get();
			if (TouhouHatItem.getEquippedHats(player).stream().anyMatch(e -> e.is(hat))) {
				if (player.getCooldowns().getCooldownPercent(hat, 0) < 0.5)
					player.getCooldowns().addCooldown(hat, 200);
			}
		}
	}

}

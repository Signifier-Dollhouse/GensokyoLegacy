package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Dual ghost: no upkeep and no aura, spent once to cancel the owner's death.
 */
final class DualGhostHandler implements GhostVariantHandler {

	@Override
	public Component getDisplayName() {
		return GLLang.Ghost.DUAL.get();
	}

	@Override
	public boolean wild() {
		return false;
	}

	@Override
	public boolean leavesWhenWeak() {
		return false;
	}

	@Override
	public boolean wardDeath(ServerPlayer sp, GhostEntity ghost, DollAttachment att) {
		if (att.absorb(sp, ghost)) {
			sp.setHealth(1.0F);
			sp.removeAllEffects();
			sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
			sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
			sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
			sp.displayClientMessage(GLLang.Ghost.DUAL_WARD.get(), true);
			sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
					SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
			return true;
		}
		return false;
	}

}

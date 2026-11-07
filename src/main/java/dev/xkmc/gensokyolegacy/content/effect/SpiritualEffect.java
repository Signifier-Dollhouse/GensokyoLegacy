package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Once a second, restores {@code 10 << amplifier} spiritual power to the items the holder carries,
 * spread over the whole inventory.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class SpiritualEffect extends EmptyEffect {

	public SpiritualEffect(MobEffectCategory category, int color) {
		super(category, color);
	}

	/**
	 * Adds {@code amount} power as an effect: a fresh instance lasts long enough to restore it, an
	 * existing one is extended by the same power at its current amplifier.
	 */
	public static void addSpiritual(Player player, int amount) {
		if (amount <= 0) return;
		int duration = amount * 2;
		MobEffectInstance old = player.getEffect(GLEffects.SPIRITUAL);
		if (old == null) {
			player.addEffect(new MobEffectInstance(GLEffects.SPIRITUAL, duration, 0));
		} else if (!old.isInfiniteDuration()) {
			int extended = old.getDuration() + (duration >> old.getAmplifier());
			player.addEffect(new MobEffectInstance(GLEffects.SPIRITUAL, extended, old.getAmplifier(),
					old.isAmbient(), old.isVisible(), old.showIcon()));
		}
	}

	@Override
	public boolean applyEffectTick(LivingEntity entity, int amplifier) {
		if (entity instanceof Player player && !player.level().isClientSide())
			SpiritualPower.restore(player, 10 << amplifier);
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}

}

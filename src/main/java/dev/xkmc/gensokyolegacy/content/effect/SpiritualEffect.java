package dev.xkmc.gensokyolegacy.content.effect;

import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import net.minecraft.world.effect.MobEffectCategory;
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

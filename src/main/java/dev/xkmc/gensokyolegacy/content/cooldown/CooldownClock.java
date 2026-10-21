package dev.xkmc.gensokyolegacy.content.cooldown;

import dev.xkmc.gensokyolegacy.mixin.ItemCooldownsAccessor;
import net.minecraft.world.entity.player.Player;

/**
 * The player's cooldown clock, run faster than vanilla by the ticks its rate buys.
 * <p>
 * The clock is advanced before vanilla ticks it, so a cooldown that runs out during this tick is
 * removed in it rather than the next one.
 *
 * @see "doc/design/cooldown.md"
 */
public class CooldownClock {

	/**
	 * The ticks the clock gains on the given tick of the player's life.
	 * <p>
	 * The rate is a fraction, so the gain is the difference between two whole numbers rather
	 * than a fraction of a tick: the clock gains exactly what the rate says over any run of
	 * ticks, and the phase comes from the tick number rather than from a carried remainder.
	 * <p>
	 * What is gained is the bonus over vanilla's own tick, so the rate enters as {@code rate - 1}:
	 * a rate of 1.2 is a fifth more ticks, not a fifth more on top of the tick vanilla already
	 * gives.
	 */
	public static int extra(float rate, long tick) {
		double bonus = rate - 1.0;
		return (int) (bonus * tick) - (int) (bonus * (tick - 1));
	}

	/** Advances the player's cooldown clock by the ticks its rate buys this tick. */
	public static void advance(Player player) {
		float rate = CooldownRates.of(player);
		if (rate <= 1.0F) return;
		var clock = (ItemCooldownsAccessor) (Object) player.getCooldowns();
		clock.gensokyolegacy$setTickCount(clock.gensokyolegacy$getTickCount()
				+ extra(rate, player.tickCount));
	}

}

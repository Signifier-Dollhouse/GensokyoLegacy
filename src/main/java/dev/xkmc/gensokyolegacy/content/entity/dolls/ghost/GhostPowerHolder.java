package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Power accessor shared by the wild and owned ghosts, so the fan and the renderers never care
 * which of the two they hold.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public interface GhostPowerHolder {

	int getPower();

	void setPower(int power);

	default void addPower(int delta) {
		setPower(getPower() + delta);
	}

	/**
	 * Opaque fraction for rendering: 1 at full power, {@link GhostPower#MIN_ALPHA} at zero.
	 */
	default float powerAlpha() {
		float fraction = Mth.clamp(getPower() / (float) GhostPower.MAX, 0, 1);
		return GhostPower.MIN_ALPHA + (1 - GhostPower.MIN_ALPHA) * fraction;
	}

	static int wildPower(RandomSource random) {
		return Mth.nextInt(random, GhostPower.WILD_MIN, GhostPower.WILD_MAX);
	}

}

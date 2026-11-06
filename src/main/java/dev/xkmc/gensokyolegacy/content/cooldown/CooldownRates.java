package dev.xkmc.gensokyolegacy.content.cooldown;

import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The cooldown speed of a player: the config baseline times every registered source, capped.
 * <p>
 * The sources multiply, so each one is a factor of the whole rather than a share of it, and the
 * cap is what keeps a pile of them from running the clock away.
 *
 * @see "doc/design/cooldown.md"
 */
public class CooldownRates {

	private static final List<CooldownSource> SOURCES = new ArrayList<>();

	public static void register(CooldownSource source) {
		SOURCES.add(source);
	}

	/**
	 * The player's cooldown clock speed. 1.0 is vanilla speed, and the result never drops under
	 * it: the clock only ever runs forward.
	 */
	public static float of(Player player) {
		float rate = 1;
		for (CooldownSource source : SOURCES) rate *= source.rate(player);
		return Mth.clamp(rate, 1.0F, GLModConfig.SERVER.cooldownMaxRate.get().floatValue());
	}

}

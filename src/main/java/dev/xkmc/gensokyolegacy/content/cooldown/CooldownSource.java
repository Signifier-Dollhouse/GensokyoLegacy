package dev.xkmc.gensokyolegacy.content.cooldown;

import net.minecraft.world.entity.player.Player;

/**
 * One source of cooldown speed for a player.
 * <p>
 * A source is asked once per player tick, so it should read state that is already there rather
 * than compute anything expensive. What it returns is a clock speed, not a duration.
 *
 * @see "doc/design/cooldown.md"
 */
public interface CooldownSource {

	/**
	 * The clock speed this source gives the player: 1.0 is vanilla speed and 2.0 is twice as fast.
	 * Values under 1.0 are ignored, since the clock only ever runs forward.
	 */
	float rate(Player player);

}

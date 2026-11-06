package dev.xkmc.gensokyolegacy.content.cooldown;

import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import net.minecraft.world.entity.player.Player;

/**
 * The lively effect: 20% more cooldown speed per level, added up rather than compounded.
 * <p>
 * Level one is 1.2 and level two is 1.4, so a second level is worth as much as the first.
 *
 * @see "doc/design/cooldown.md"
 */
public class LivelySource implements CooldownSource {

	/**
	 * How much speed one level of lively adds.
	 */
	private static final float PER_LEVEL = 0.2F;

	@Override
	public float rate(Player player) {
		var effect = player.getEffect(GLEffects.LIVELY);
		if (effect == null) return 1.0F;
		return 1.0F + PER_LEVEL * (effect.getAmplifier() + 1);
	}

}

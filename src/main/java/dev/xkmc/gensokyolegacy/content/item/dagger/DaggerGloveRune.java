package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * An extra effect a {@link DaggerGloveItem} dagger gains on hitting something, paid for in extra cooldown
 * instead of extra daggers: a stateless server-side singleton, added as an implementation here plus a
 * {@link DaggerGloveRunes} line.
 *
 * @see "doc/design/dagger_glove.md"
 */
public interface DaggerGloveRune {

	/** Ticks this rune adds to the shot's cooldown, on top of the mode's own. Zero for a free rune. */
	int cooldownCost();

	/**
	 * Runs when a dagger carrying this rune hits an entity, before the dagger is handed back
	 * ({@link IronDaggerBulletEntity#giveBack} runs either way) and before the base class applies damage.
	 *
	 * @param level  the level the hit happened in
	 * @param owner  the player who fired the dagger, or null if it no longer has one
	 * @param target the entity hit, already unwrapped from any multipart entity
	 * @param bullet the dagger itself, for anything that needs its position or velocity
	 */
	default void onHit(ServerLevel level, LivingEntity owner, LivingEntity target, IronDaggerBulletEntity bullet) {
	}

}
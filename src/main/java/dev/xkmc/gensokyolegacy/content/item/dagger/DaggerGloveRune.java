package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * An extra effect a {@link DaggerGloveItem} dagger gains on hitting something, paid for in extra
 * cooldown instead of extra daggers.
 * <p>
 * The port exists before any rune does: nothing in the game registers one yet. The point of the
 * interface is that {@link #cooldownCost()} is the <em>only</em> thing a rune has to say about
 * the glove's balance — see {@link DaggerGloveMode#cooldown(net.minecraft.world.item.ItemStack)},
 * which adds it to the mode's own cooldown and is otherwise untouched by runes. A new rune is a
 * new implementation of this interface plus a line in {@link DaggerGloveRunes}.
 * <p>
 * Runes are stateless singletons (see {@link DaggerGloveRunes}) and act server-side only: a rune
 * is reached through the bullet that was fired with it, and the bullet only calls back on a
 * server-side entity hit.
 */
public interface DaggerGloveRune {

	/**
	 * Ticks this rune adds to the shot's cooldown, on top of the mode's own cooldown. Zero for a
	 * rune that is meant to be free, which makes the whole rune system a plain cost knob.
	 */
	int cooldownCost();

	/**
	 * Runs when a dagger carrying this rune hits an entity, before the dagger is handed back
	 * ({@link IronDaggerBulletEntity#giveBack} runs either way) and before the damage is applied
	 * by the danmaku base class.
	 *
	 * @param level  the level the hit happened in
	 * @param owner  the player who fired the dagger, or null if it no longer has one
	 * @param target the entity the dagger hit, already unwrapped from any multipart entity
	 * @param bullet the dagger itself, for anything that needs its position or velocity
	 */
	default void onHit(ServerLevel level, LivingEntity owner, LivingEntity target, IronDaggerBulletEntity bullet) {
	}

}
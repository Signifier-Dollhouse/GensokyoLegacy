package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Anything that hosts doll data and pairs it with the deployed {@link BaseDollEntity}.
 * <ul>
 *   <li>{@link DollAttachment} — the player capability ledger (doc/design/doll/pairing.md).</li>
 *   <li>{@link dev.xkmc.gensokyolegacy.content.block.functional.doll.DollControllerBlockEntity}
 *       — a block that hosts one resident doll and can deploy it (doc/design/doll/controller.md).</li>
 *   <li>{@link dev.xkmc.gensokyolegacy.content.entity.characters.magician.AliceDollHost} —
 *       a character that conjures her own retinue (doc/design/doll/host.md).</li>
 * </ul>
 * The DollData key is the entity's own game UUID (never serialized on the entity or the item),
 * so entity-side lookups always hit the real living entity and the inverse check
 * (world → host) is identical for every host type (§8.2).
 * <p>
 * The second half of the interface is the <b>command surface</b>: everything the doll entity
 * asks its host about commanding. Dolls never branch on the concrete host type — they call
 * these methods and a host that never commands (a controller block, a stray) simply keeps the
 * inert defaults. {@link DollLedger} forwards them to its {@link DollCommander}.
 */
public interface DollHost {

	/**
	 * A summoned doll that has drifted at least this far from its host is discarded
	 * and conjured again nearby. Every ledger applies it the same way; the value
	 * matches the follow range the dolls are deployed with.
	 */
	double PULLBACK_DISTANCE = 48.0;

	@Nullable
	DollData findSummoned(UUID uuid);

	void update(BaseDollEntity doll);

	/**
	 * Cuts a summoned doll from pairing without producing an item (stray): removes
	 * and returns the entry, or null when absent. The entity keeps going on its own
	 * from then on and drops its item form on death.
	 */
	@Nullable
	DollData detach(UUID uuid);

	/**
	 * Death hook. Only {@link StrayHost} acts here (drops the item form); ledgers
	 * recover entries through their own deferred paths and ignore it.
	 */
	default void onDeath(BaseDollEntity doll) {
	}

	// ---------- command surface ----------
	// Inert by default: a host that never issues orders (controller block, stray)
	// answers with "nothing to follow, nobody to hand off to, nobody commanded anything".

	/**
	 * The yaw the follow formation is anchored on, in degrees. A host that keeps
	 * summoned dolls in a formation latches it here (see {@code DollAttachment}).
	 */
	default float getFormationYaw() {
		return 0;
	}

	/**
	 * Fellow summoned dolls of this host, excluding the given one. Used for
	 * ally-aware firing lanes, so it may skip anything far away or unloaded.
	 */
	default List<DollEntity> summonedAllies(DollEntity doll) {
		return List.of();
	}

	/**
	 * Whether this host has commanded an attack against the given entity. Bullets
	 * outlive their tickets, so commanded targets stay enemies afterwards.
	 */
	default boolean isCommandedTarget(LivingEntity target) {
		return false;
	}

	/**
	 * The action type this doll already completed in a live iterative volley, if any.
	 */
	default Optional<DollActionType> doneType(UUID uuid) {
		return Optional.empty();
	}

	/**
	 * Tells the next available doll to start the action ahead of the given one,
	 * without releasing the given doll's own wait and without touching the
	 * iterative done-set. No-op for hosts without iterative orders.
	 */
	default void handAhead(DollEntity doll, DollAction action) {
	}

	/**
	 * Chain-passing for iterative actions: start the same action (shared done-set)
	 * on the next available doll of this host. Returns false when the iteration ends.
	 */
	default boolean handOff(DollEntity doll, DollAction action) {
		return false;
	}

}

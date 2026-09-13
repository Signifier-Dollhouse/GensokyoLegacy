package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Anything that hosts doll data and pairs it with the deployed {@link BaseDollEntity}.
 * <ul>
 *   <li>{@link DollAttachment} — the player capability ledger (doc/design/doll/pairing.md).</li>
 *   <li>{@link dev.xkmc.gensokyolegacy.content.block.functional.doll.DollControllerBlockEntity}
 *       — a block that hosts one resident doll and can deploy it (doc/design/doll/controller.md).</li>
 * </ul>
 * The DollData key is the entity's own game UUID (never serialized on the entity or the item),
 * so entity-side lookups always hit the real living entity and the inverse check
 * (world → host) is identical for every host type (§8.2).
 */
public interface DollHost {

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

}
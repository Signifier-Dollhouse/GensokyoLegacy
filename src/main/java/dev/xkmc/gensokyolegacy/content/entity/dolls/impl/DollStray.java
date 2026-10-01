package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollState;
import dev.xkmc.gensokyolegacy.content.attachment.doll.StrayHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Stray cut: detaches the ledger entry (no item produced) and hosts it transiently.
 * From here commands can't reach the doll, itemize no-ops, and only death ends it
 * (dropping the item form). The detached inventory is already current — all server
 * mutations are ledger-direct. Fully defaulted — the entity inherits it as-is.
 */
public interface DollStray extends DollBaseImpl {

	default void becomeStray() {
		DollEntity doll = asDoll();
		if (doll.strayHost() != null) return;
		DollHost host = doll.getHost();
		DollData data = host == null ? null : host.detach(doll.getUUID());
		if (data == null) {
			data = new DollData();
			data.uuid = doll.getUUID();
			data.state = DollState.SUMMONED;
			for (DollSlot slot : DollSlot.values())
				data.inventory.set(slot, doll.getLoadoutItem(slot));
		}
		doll.writeValuesTo(data);
		doll.setStrayHost(new StrayHost(data));
	}

	/**
	 * Reverse of {@link #becomeStray()}: hands the detached entry back to the
	 * owner's ledger, which resummons the doll from the parked TEMP entry on its
	 * next tick. Server-only. A stray whose owner is offline stays a stray — the
	 * ledger lives on the player, so there is nowhere to hand the entry back to.
	 */
	default boolean rejoinOwner() {
		DollEntity doll = asDoll();
		UUID owner = doll.getOwnerUUID();
		if (owner == null || !(doll.level() instanceof ServerLevel level)) return false;
		ServerPlayer sp = level.getServer().getPlayerList().getPlayer(owner);
		if (sp == null) return false;
		return GLMeta.DOLL.type().getOrCreate(sp).rejoin(doll);
	}

	/** Idle rejoin period, in ticks (one second). */
	int REJOIN_INTERVAL = 20;

	/**
	 * Idle fallback for the stray cut, ticked once per {@link #REJOIN_INTERVAL}:
	 * a stray with nothing to do (its dive aborted while the owner was logged
	 * out, say) tries to rejoin every second until its owner is back, so the
	 * cut can never leave a permanently ownerless doll. No-op while the doll
	 * holds a ticket — a dive in flight is never yanked.
	 */
	default void maybeRejoin() {
		DollEntity doll = asDoll();
		if (!doll.isStray() || doll.actions.isActive()) return;
		if (doll.tickCount % REJOIN_INTERVAL != 0) return;
		rejoinOwner();
	}

}

package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionStatus;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import net.minecraft.network.syncher.SynchedEntityData;

import java.util.Optional;

/**
 * Client-visible action status for the attack-glove sidebar. Ephemeral: synced
 * to clients via entity data, never persisted to {@link dev.xkmc.gensokyolegacy.content.attachment.doll.DollData}.
 * Recomputed server-side every tick — the ticket itself stays server-only.
 */
public interface DollStatus extends DollBaseImpl {

	default DollActionStatus getActionStatus() {
		int i = getEntityData().get(DollEntity.DATA_ACTION_STATE);
		DollActionStatus[] all = DollActionStatus.values();
		return i < 0 || i >= all.length ? DollActionStatus.IDLE : all[i];
	}

	/**
	 * Ordinal of the current (or just-finished iterative) action type, -1 when idle.
	 */
	default int getActionStatusType() {
		return getEntityData().get(DollEntity.DATA_ACTION_TYPE);
	}

	/**
	 * Whether the doll can perform the given action type with its current
	 * loadout. Server-computed into {@code DATA_VALID_MASK} from the
	 * authoritative ledger; the sidebar only tests the bit.
	 */
	default boolean isValidFor(DollActionType type) {
		return (getEntityData().get(DollEntity.DATA_VALID_MASK) & (1 << type.ordinal())) != 0;
	}

	/**
	 * Server-side recompute: holding a ticket but not yet executing is preparing,
	 * executing is attacking, sitting in a live volley's done-set with no ticket
	 * is done, otherwise idle. Done only applies while the volley is still
	 * in-flight, so no latch needs clearing. The validity mask is recomputed
	 * alongside from the authoritative ledger loadout.
	 */
	default void syncActionStatus() {
		DollEntity doll = asDoll();
		DollAction current = doll.actions.getCurrent();
		int type;
		DollActionStatus status;
		if (current != null) {
			type = current.type().ordinal();
			status = doll.isExecutingCommand() ? DollActionStatus.ATTACKING : DollActionStatus.PREPARING;
		} else {
			Optional<DollActionType> done = doneType(doll);
			if (done.isPresent()) {
				type = done.get().ordinal();
				status = DollActionStatus.DONE;
			} else {
				type = -1;
				status = DollActionStatus.IDLE;
			}
		}
		if (getEntityData().get(DollEntity.DATA_ACTION_STATE) != status.ordinal())
			getEntityData().set(DollEntity.DATA_ACTION_STATE, status.ordinal());
		if (getEntityData().get(DollEntity.DATA_ACTION_TYPE) != type)
			getEntityData().set(DollEntity.DATA_ACTION_TYPE, type);
		int mask = 0;
		for (DollActionType action : DollActionType.values()) {
			if (DollBehaviorRegistry.findHand(doll, action).isPresent())
				mask |= 1 << action.ordinal();
		}
		if (getEntityData().get(DollEntity.DATA_VALID_MASK) != mask)
			getEntityData().set(DollEntity.DATA_VALID_MASK, mask);
	}

	private static Optional<DollActionType> doneType(DollEntity doll) {
		if (doll.getHost() instanceof DollAttachment att)
			return att.commands.doneType(doll.getUUID());
		return Optional.empty();
	}

	final class DollStatusModule implements DollModule {

		@Override
		public void defineSynchedData(SynchedEntityData.Builder builder) {
			builder.define(DollEntity.DATA_ACTION_STATE, DollActionStatus.IDLE.ordinal());
			builder.define(DollEntity.DATA_ACTION_TYPE, -1);
			builder.define(DollEntity.DATA_VALID_MASK, 0);
		}

	}

}

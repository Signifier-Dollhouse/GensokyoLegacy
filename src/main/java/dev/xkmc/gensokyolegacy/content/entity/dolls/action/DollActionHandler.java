package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

/**
 * Transient per-doll ticket state: at most one current action. Server-only, never
 * persisted — commands are ephemeral and die with the entity.
 * <p>
 * Ticket rules: a doll starts another action only after finishing or aborting the
 * current one. Only a running {@code REGULAR_ATTACK} or a running {@code AUTO}
 * ticket can be aborted; anything else holds its ticket until it completes (or
 * {@link #stop} clears it). Aborts run the same path as completions (done-set +
 * handoff for iterative), just without performing — the delegating goal releases
 * stuck tickets through it.
 * Execution itself lives in the behaviors, driven by the delegating goal — this
 * class only holds the ticket.
 */
public class DollActionHandler {

	private final EnumMap<DollActionType, Long> cooldowns = new EnumMap<>(DollActionType.class);

	@Nullable
	private DollAction current;

	private long acceptedAt;
	private boolean handAheadSent;
	private long suppressUntil;
	private long shieldBlockUntil;
	private boolean kamikaze;

	public boolean isActive() {
		return current != null;
	}

	@Nullable
	public DollAction getCurrent() {
		return current;
	}

	public boolean isKamikaze() {
		return kamikaze;
	}

	public void setKamikaze(boolean kamikaze) {
		this.kamikaze = kamikaze;
	}

	public long acceptedAt() {
		return acceptedAt;
	}

	public boolean handAheadSent() {
		return handAheadSent;
	}

	public void markHandAheadSent() {
		handAheadSent = true;
	}

	/**
	 * Whether the held ticket yields to a new order: a running
	 * {@code REGULAR_ATTACK} or any running {@code AUTO} ticket is aborted;
	 * anything else running refuses.
	 */
	public static boolean abortable(@Nullable DollAction current) {
		return current != null &&
				(current.type() == DollActionType.REGULAR_ATTACK || current.mode() == DollActionMode.AUTO);
	}

	/**
	 * Takes the ticket: idle dolls accept; an abortable running ticket (regular
	 * attack or auto) is aborted for the new order; anything else running
	 * refuses. Re-issuing the current order is a no-op success. The dropped
	 * behavior is stopped by the delegating goal on its next pass; the orphaned
	 * action (if iterative) is picked back up by the stall guard.
	 */
	public boolean tryStart(DollAction action, long gameTime) {
		if (current != null) {
			if (current.sameOrder(action)) return true;
			if (!abortable(current)) return false;
			current = null;
		}
		current = action;
		acceptedAt = gameTime;
		handAheadSent = false;
		return true;
	}

	/**
	 * Cancel the ticket (glove stop). Suicide tickets are never passed here —
	 * {@code stopAll} skips them. Also stamps a short suppress so a scheduled
	 * heal doesn't re-fire on the very next tick against a still-valid target.
	 */
	public void stop(long gameTime) {
		current = null;
		acceptedAt = 0;
		handAheadSent = false;
		kamikaze = false;
		suppressUntil = gameTime + 100;
	}

	public boolean isSuppressed(long gameTime) {
		return gameTime < suppressUntil;
	}

	/**
	 * Capability + availability check for command fan-out. An off-hand hit implies
	 * the sticky swap at start. A ticket held by anything but an abortable regular
	 * refuses; iterative actions skip dolls already in {@code done}.
	 */
	public boolean canAccept(DollEntity doll, DollAction action) {
		if (current != null) {
			if (current.sameOrder(action)) return false;
			if (!abortable(current)) return false;
		}
		if (action.mode() == DollActionMode.ITERATIVE && action.done().contains(doll.getUUID())) return false;
		return DollBehaviorRegistry.findHand(doll, action.type()).isPresent();
	}

	/**
	 * Identity check: does this doll currently hold the action (running, not yet
	 * started)? Used by the iterative stall guard — record equality is unstable
	 * because the shared done-set mutates.
	 */
	public boolean holds(DollAction action) {
		return current == action;
	}

	/**
	 * Finish the current attempt: iterative actions record the doll and hand off,
	 * then the ticket releases. Also the abort path for unstarted waits (start
	 * timeouts) — giving up records done the same way performing does.
	 */
	public void complete(DollEntity doll) {
		DollAction finished = current;
		current = null;
		acceptedAt = 0;
		handAheadSent = false;
		if (finished != null && finished.mode() == DollActionMode.ITERATIVE) {
			finished.done().add(doll.getUUID());
			DollHost host = doll.getHost();
			if (host instanceof DollAttachment att) {
				att.commands.handOff(doll, finished);
			}
		}
	}

	public boolean ready(DollActionType type, int cooldownTicks, long gameTime) {
		return gameTime - cooldowns.getOrDefault(type, -1_000_000_000L) >= cooldownTicks;
	}

	public void stamp(DollActionType type, long gameTime) {
		cooldowns.put(type, gameTime);
	}

	public boolean isShieldReady(long gameTime) {
		return gameTime >= shieldBlockUntil;
	}

	public void stampShieldBlock(long gameTime, int cooldownTicks) {
		shieldBlockUntil = gameTime + cooldownTicks;
	}

}

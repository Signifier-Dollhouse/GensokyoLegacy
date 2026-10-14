package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

/**
 * Transient per-doll ticket state: at most one current action. Server-only, never persisted.
 * A doll starts another only after finishing or aborting the current one, and only a running
 * {@code REGULAR_ATTACK} or {@code AUTO} ticket can be aborted. Aborts run the completion
 * path without performing. Execution lives in the behaviors (control.md §4).
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
	 * Whether the held ticket yields to a new order: a running {@code REGULAR_ATTACK} or any
	 * running {@code AUTO} ticket is aborted; anything else running refuses.
	 */
	public static boolean abortable(@Nullable DollAction current) {
		return current != null &&
				(current.type() == DollActionType.REGULAR_ATTACK || current.mode() == DollActionMode.AUTO);
	}

	/**
	 * Takes the ticket: idle dolls accept, an abortable running ticket is dropped for the new
	 * order, anything else running refuses, and re-issuing the current order is a no-op success.
	 * The dropped behavior is stopped by the delegating goal; an orphaned iterative action is
	 * picked back up by the stall guard.
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
	 * Cancel the ticket (glove stop). Suicide tickets never reach here — {@code stopAll} skips
	 * them. Also stamps a short suppress so a scheduled heal doesn't re-fire next tick.
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
	 * Capability + availability check for command fan-out; an off-hand hit implies the sticky
	 * swap. Only an abortable regular yields, and iterative actions skip dolls in {@code done}.
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
	 * Identity check: does this doll currently hold the action (running, not yet started)? Used
	 * by the stall guard and the goal's stop path — record equality is unstable, the shared
	 * done-set mutates.
	 */
	public boolean holds(DollAction action) {
		return current == action;
	}

	/**
	 * Finish the current attempt: iterative actions record the doll and hand off, then the ticket
	 * releases. Also the abort path for unstarted waits, which records done the same way.
	 */
	public void complete(DollEntity doll) {
		DollAction finished = current;
		current = null;
		acceptedAt = 0;
		handAheadSent = false;
		if (finished != null && finished.mode() == DollActionMode.ITERATIVE) {
			finished.done().add(doll.getUUID());
			DollHost host = doll.getHost();
			if (host != null) host.handOff(doll, finished);
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

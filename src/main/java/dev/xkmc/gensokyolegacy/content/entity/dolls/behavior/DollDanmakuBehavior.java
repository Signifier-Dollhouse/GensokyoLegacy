package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionMode;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollCardHolder;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.util.DollShootUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Regular attack: one danmaku, item not consumed. Fires on start, then a 10-tick wind-down;
 * never chases and out-of-range targets abort immediately. Aimed with laser-style flight-time
 * prediction at fixed shot speed. Sidesteps while an ally blocks the lane, skips the shot
 * when the lane stays blocked (control.md §5.1).
 */
public class DollDanmakuBehavior extends DollBehavior {

	private static final int WINDDOWN_TICKS = 10;
	private static final int SHOT_LIFE = 60;
	private static final double RANGE = 48.0;
	private static final int COOLDOWN_TICKS = 20;
	private static final int BLOCKED_SKIP_TICKS = 40;

	/**
	 * Fired shot speed in blocks per tick; the aim prediction assumes it.
	 */
	private static final double SHOT_SPEED = 2;

	/**
	 * Strafing this long hands ahead (the same 1s as an unstarted wait): the next doll
	 * goes while this one keeps sidestepping. Shared flag, fires at most once.
	 */
	private static final int HAND_AHEAD_TICKS = 20;

	private boolean fired;
	private int winddown;
	private int blockedTicks;
	private int navCooldown;

	@Override
	public DollActionType type() {
		return DollActionType.REGULAR_ATTACK;
	}

	@Override
	public boolean canUse(DollEntity doll) {
		if (!isServer(doll)) return false;
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		if (target == null || !inRange(doll, target, RANGE)) return false;
		if (hand(doll).isEmpty()) return false;
		return doll.actions.ready(type(), COOLDOWN_TICKS, doll.level().getGameTime());
	}

	@Override
	public boolean canContinueToUse(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		return target != null && inRange(doll, target, RANGE) && hand(doll).isPresent();
	}

	@Override
	public void start(DollEntity doll) {
		fired = false;
		winddown = 0;
		blockedTicks = 0;
		navCooldown = 0;
		halt(doll);
	}

	@Override
	public void stop(DollEntity doll) {
		halt(doll);
		fired = false;
		winddown = 0;
		blockedTicks = 0;
		navCooldown = 0;
	}

	@Override
	public void tick(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return;
		LivingEntity target = resolveTarget(doll, action);
		if (target == null || !inRange(doll, target, RANGE)) {
			doll.actions.complete(doll);
			return;
		}
		if (fired) {
			faceTarget(doll, target);
			if (++winddown >= WINDDOWN_TICKS) doll.actions.complete(doll);
			return;
		}
		faceTarget(doll, target);
		Vec3 aim = targetCenter(target);
		Optional<LivingEntity> blocker = DollFriendlyFire.findBlocker(doll, aim, target);
		if (blocker.isPresent()) {
			blockedTicks++;
			if (action.mode() == DollActionMode.ITERATIVE && blockedTicks >= HAND_AHEAD_TICKS &&
					!doll.actions.handAheadSent()) {
				doll.actions.markHandAheadSent();
				DollHost host = doll.getHost();
				if (host != null) host.handAhead(doll, action);
			}
			if (blockedTicks > BLOCKED_SKIP_TICKS) {
				doll.actions.complete(doll);
				return;
			}
			Vec3 dest = DollFriendlyFire.strafeDest(doll, doll.position(), aim, blocker.get());
			if (dest != null && --navCooldown <= 0) {
				navCooldown = 10;
				doll.getNavigation().moveTo(dest.x, dest.y, dest.z, doll.speedModifier);
			}
			return;
		}
		Optional<HandMatch> match = hand(doll);
		// the card is taken off the match rather than re-read from the main hand after the
		// swap below: an intrinsic binding names one for a timed summon that holds nothing, and
		// the swap may have brought a different hand's danmaku forward than the match did
		if (match.isEmpty() || match.get().danmaku() == null) {
			doll.actions.complete(doll);
			return;
		}
		ensureMainHand(doll, match.get());
		DollCardHolder holder = new DollCardHolder(doll, aim);
		Vec3 dir = DollShootUtils.predictShotDir(target, holder.center(), SHOT_SPEED);
		holder.shoot(holder.prepareDanmaku(SHOT_LIFE, dir.scale(SHOT_SPEED), match.get().danmaku()));
		doll.broadcastBowAnim();

		doll.actions.stamp(type(), doll.level().getGameTime());
		if (blockedTicks > HAND_AHEAD_TICKS) {
			doll.actions.complete(doll);
			return;
		}
		fired = true;
	}

}

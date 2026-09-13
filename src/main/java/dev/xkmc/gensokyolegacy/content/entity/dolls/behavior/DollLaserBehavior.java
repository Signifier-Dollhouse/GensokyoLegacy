package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.danmakuapi.content.item.LaserItem;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollCardHolder;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.util.DollShootUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Super attack, laser variant: setup → emit (ammo committed) → hold → close.
 * One laser, consuming the stack at emission. Sidesteps while an ally blocks the
 * lane during setup, firing anyway past the strafe budget.
 */
public class DollLaserBehavior extends DollBehavior {

	private static final int SETUP_TICKS = 5;
	private static final int DURATION_TICKS = 40;
	private static final int CLOSE_TICKS = 10;
	private static final float LASER_LENGTH = 40.0F;
	private static final double RANGE = 40.0;

	/**
	 * Strafe budget: an ally-blocked lane sidesteps this long, then the check is
	 * skipped and the shot fires anyway.
	 */
	private static final int STRAFE_SKIP_TICKS = 20;

	private static final int SETUP = 0;
	private static final int DURATION = 1;
	private static final int CLOSE = 2;

	private int phase;
	private int ticks;
	private boolean emitted;
	private int blockedTicks;
	private int navCooldown;

	@Override
	public DollActionType type() {
		return DollActionType.SUPER_ATTACK;
	}

	@Override
	public boolean canUse(DollEntity doll) {
		if (!isServer(doll)) return false;
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		if (target == null || !inRange(doll, target, RANGE)) return false;
		Optional<HandMatch> match = hand(doll);
		return match.isPresent() && match.get().stack().getItem() instanceof LaserItem;
	}

	@Override
	public boolean canContinueToUse(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		if (target == null || !inRange(doll, target, RANGE)) return false;
		if (emitted) return true;
		Optional<HandMatch> match = hand(doll);
		return match.isPresent() && match.get().stack().getItem() instanceof LaserItem;
	}

	@Override
	public void start(DollEntity doll) {
		phase = SETUP;
		ticks = 0;
		emitted = false;
		blockedTicks = 0;
		navCooldown = 0;
		halt(doll);
	}

	@Override
	public void stop(DollEntity doll) {
		halt(doll);
		phase = SETUP;
		ticks = 0;
		emitted = false;
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
		faceTarget(doll, target);
		if (phase == SETUP) {
			Vec3 aim = targetCenter(target);
			Optional<LivingEntity> blocker = DollFriendlyFire.findBlocker(doll, aim, target);
			if (blocker.isPresent() && blockedTicks <= STRAFE_SKIP_TICKS) {
				blockedTicks++;
				Vec3 dest = DollFriendlyFire.strafeDest(doll, doll.position(), aim, blocker.get());
				if (dest != null && --navCooldown <= 0) {
					navCooldown = 10;
					doll.getNavigation().moveTo(dest.x, dest.y, dest.z, doll.speedModifier);
				}
				return;
			}
		}
		if (++ticks < phaseLength()) return;
		ticks = 0;
		if (phase == SETUP) {
			emit(doll, target);
			phase = DURATION;
		} else if (phase == DURATION) {
			phase = CLOSE;
		} else {
			doll.actions.complete(doll);
		}
	}

	private int phaseLength() {
		return phase == SETUP ? SETUP_TICKS : phase == DURATION ? DURATION_TICKS : CLOSE_TICKS;
	}

	private void emit(DollEntity doll, LivingEntity target) {
		Optional<HandMatch> match = hand(doll);
		if (match.isEmpty() || !(match.get().stack().getItem() instanceof LaserItem)) {
			doll.actions.complete(doll);
			return;
		}
		ensureMainHand(doll, match.get());
		ItemStack stack = doll.ledgerStack(DollSlot.MAIN_HAND);
		if (!(stack.getItem() instanceof LaserItem item)) {
			doll.actions.complete(doll);
			return;
		}
		Vec3 aim = DollShootUtils.predictCenter(target, SETUP_TICKS);
		DollCardHolder holder = new DollCardHolder(doll, aim);
		Vec3 dir = aim.subtract(holder.center()).normalize();
		holder.shoot(holder.prepareLaser(DURATION_TICKS, holder.center(), dir, LASER_LENGTH, item.type, item.color));
		if (doll.consumeLoadoutItem(DollSlot.MAIN_HAND, 1)) {
			emitted = true;
		} else {
			doll.actions.complete(doll);
		}
	}

}

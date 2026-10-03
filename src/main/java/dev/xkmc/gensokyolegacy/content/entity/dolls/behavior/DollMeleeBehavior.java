package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.content.item.doll.DollLanceItem;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Regular attack, melee variant: charge the target at twice the movement cap, cut
 * the swing on contact, recoil off it, hold the spot for {@link #HOLD_TICKS} ticks,
 * then fly back to where the charge began.
 * {@link DollLanceItem} only ({@code DollBehaviors}).
 * <p>
 * The ticket leaves <b>on impact</b> — that is the point of charging inside a
 * volley: {@code complete()} hands the attack to the next doll while this one is
 * already sitting on its target. The behavior outlives its ticket through
 * {@link #selfDriven} — the hold always runs out, and the flight is released once
 * the doll is at least half way back, or {@link #RETURN_TICKS} runs out for a
 * return stalled against a wall — after which follow takes the doll over again.
 * <p>
 * The charge and the return are plain velocity rather than a path: a charge has to
 * be a straight lunge, not a detour, and it has to outrun
 * {@link BaseDollEntity#MAX_SPEED}, so the behavior raises the movement cap for
 * the charge and drops it back the moment the swing lands ({@link #stop} is the
 * backstop). The charge is exempt from the owner leash for its length — it is
 * a bounded dash that ends back on the exact spot it left, so {@link #CHARGE_REACH}
 * is what bounds it instead, and what stops a target from dragging a doll across a
 * field it was never meant to leave.
 */
public class DollMeleeBehavior extends DollBehavior {

	/** Targets further away than this are left to the ranged behaviors. */
	private static final double ENGAGE_RANGE = 16.0;

	/** Contact range that lands the swing (bounding-box distance). */
	private static final double MELEE_RANGE = 2.5;

	/**
	 * How far the charge may carry the doll from where it began — the charge's own
	 * leash, standing in for the owner ring while it lasts (§5). Generous enough that
	 * every target the engage range admits can still be reached (16 + contact), and
	 * tight enough that a charging doll is never further than this plus its formation
	 * radius from its owner: 26 blocks, against a 48-block ledger pullback.
	 */
	private static final double CHARGE_REACH = 20.0;

	/** Charge speed: twice the movement cap, which the charge raises to match. */
	private static final double CHARGE_SPEED = BaseDollEntity.MAX_SPEED * 2;

	/** Return speed: ordinary flight, i.e. the cap again. */
	private static final double RETURN_SPEED = BaseDollEntity.MAX_SPEED;

	/** Fraction of the charge velocity the impact bounce kicks back with. */
	private static final double RECOIL = 0.5;

	/** Blocks per tick the struck target is pushed, vanilla's default punch. */
	private static final double KNOCKBACK = 0.5;

	private static final int COOLDOWN_TICKS = 20;

	/**
	 * Charge budget, and the backstop for a target the doll can never actually catch
	 * — one circling inside {@link #CHARGE_REACH}, say. A plain run away is caught by
	 * the reach first.
	 */
	private static final int CHARGE_TICKS = 40;

	/**
	 * Ticks of pressing into something before the charge gives up — see the stall
	 * guard in {@link #tick}.
	 */
	private static final int STALL_TICKS = 10;

	/**
	 * Ticks the doll hangs on the spot the swing landed on before it turns for
	 * home. The lunge ends on contact, so without a beat here the doll snaps
	 * straight from "in the target's face" to "flying away" and the swing never
	 * reads; the hold is what makes the strike land as a hit rather than a pass-by.
	 * Fixed rather than budgeted — it ends on its own, so it cannot strand a doll
	 * the way a stalled return would.
	 */
	private static final int HOLD_TICKS = 6;

	/** Return budget: the way home is straight and at full flight speed. */
	private static final int RETURN_TICKS = 60;

	private static final int CHARGE = 0;
	private static final int HOLD = 1;
	private static final int RETURN = 2;

	private int phase;
	private int ticks;
	private int stalled;

	/** Where the charge began, and the return destination. Set in {@link #start}. */
	private Vec3 home = Vec3.ZERO;

	/** Half of the impact → home leg: the release point of the return flight. */
	private double halfLeg;

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
		if (target == null || !inRange(doll, target, ENGAGE_RANGE)) return false;
		if (hand(doll).isEmpty()) return false;
		return doll.actions.ready(type(), COOLDOWN_TICKS, doll.level().getGameTime());
	}

	@Override
	public boolean canContinueToUse(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return false;
		// A ticket that showed up after the swing preempts the tail: neither the
		// hold nor the flight home ever outlives an order, so the goal re-evaluates
		// and this one steps aside.
		if (phase != CHARGE) return false;
		LivingEntity target = resolveTarget(doll, action);
		return target != null && hand(doll).isPresent();
	}

	@Override
	public boolean selfDriven(DollEntity doll) {
		if (phase == CHARGE) return false;
		// The hold ends on its own tick count, so it needs no release test.
		if (phase == HOLD) return true;
		return ticks < RETURN_TICKS && doll.position().distanceToSqr(home) > halfLeg * halfLeg;
	}

	/**
	 * A charge has to begin from inside {@link #ENGAGE_RANGE}, and a doll standing
	 * in formation does not close that gap by waiting — so a target past it is one
	 * this doll can never go and get. That is why the wait gives the ticket up at
	 * once instead of sitting on it (the delegating goal asks this), and why a
	 * volley at such a target is reported as too far rather than ordered at all.
	 */
	@Override
	public boolean canReach(DollEntity doll, LivingEntity target) {
		return inRange(doll, target, ENGAGE_RANGE);
	}

	@Override
	public void start(DollEntity doll) {
		phase = CHARGE;
		ticks = 0;
		stalled = 0;
		home = doll.position();
		halfLeg = 0;
		halt(doll);
		doll.setSpeedCap(CHARGE_SPEED);
	}

	@Override
	public void stop(DollEntity doll) {
		halt(doll);
		doll.setSpeedCap(BaseDollEntity.MAX_SPEED);
		phase = CHARGE;
		ticks = 0;
		stalled = 0;
		halfLeg = 0;
	}

	@Override
	public void tick(DollEntity doll) {
		if (phase == HOLD) {
			hold(doll);
			return;
		}
		if (phase == RETURN) {
			flyHome(doll);
			return;
		}
		DollAction action = current(doll);
		if (action == null) return;
		LivingEntity target = resolveTarget(doll, action);
		if (target == null || ++ticks > CHARGE_TICKS) {
			abort(doll);
			return;
		}
		faceTarget(doll, target);
		Vec3 toTarget = targetCenter(target).subtract(doll.position());
		double dist = toTarget.length();
		if (dist <= MELEE_RANGE) {
			strike(doll, target, toTarget);
			return;
		}
		// Contact first, stall second: a doll that scraped a wall on the way in
		// still swings when it arrives. A lunge is a straight line, so a wall is a
		// wall — no extra budget gets past it.
		if (doll.horizontalCollision) {
			if (++stalled >= STALL_TICKS) abort(doll);
			return;
		}
		stalled = 0;
		// Past its own reach the charge is over — a target that keeps pulling away
		// gets dropped here instead of towing the doll off after it.
		if (doll.distanceToSqr(home) > CHARGE_REACH * CHARGE_REACH) {
			abort(doll);
			return;
		}
		Vec3 step = toTarget.scale(CHARGE_SPEED / dist);
		faceDir(doll, step);
		doll.setDeltaMovement(step);
	}

	/**
	 * The contact tick: swing, and hand the attack on. The ticket goes out here so
	 * the volley moves on while the doll is still flying; the bounce is the charge
	 * velocity reverted, which is what reads as recoil.
	 */
	private void strike(DollEntity doll, LivingEntity target, Vec3 toTarget) {
		Optional<HandMatch> match = hand(doll);
		if (match.isEmpty()) {
			abort(doll);
			return;
		}
		ensureMainHand(doll, match.get());
		swing(doll, doll.ledgerStack(DollSlot.MAIN_HAND), target, toTarget);
		doll.broadcastAttackAnim();
		doll.actions.stamp(type(), doll.level().getGameTime());
		doll.actions.complete(doll);
		beginHold(doll);
		doll.setDeltaMovement(doll.getDeltaMovement().scale(-RECOIL));
	}

	private void swing(DollEntity doll, ItemStack weapon, LivingEntity target, Vec3 toTarget) {
		// Plain mob_attack; the doll's melee source is turned into a bypass-cooldown
		// variant in GLAttackListener#onCreateSource, which covers every doll melee
		// rather than only this behavior.
		DamageSource source = doll.damageSources().mobAttack(doll);
		if (!target.hurt(source, (float) doll.getAttributeValue(Attributes.ATTACK_DAMAGE))) return;
		Vec3 push = new Vec3(toTarget.x, 0, toTarget.z);
		if (push.lengthSqr() < 1e-6) return;
		push = push.normalize();
		target.knockback(KNOCKBACK, push.x, push.z);
	}

	/** No hit, but the ticket still goes out — a charge given up costs nothing. */
	private void abort(DollEntity doll) {
		doll.actions.complete(doll);
		doll.setSpeedCap(RETURN_SPEED);
		beginReturn(doll);
	}

	/**
	 * The strike landed: the charge is over, so the doubled cap goes back to the
	 * ordinary one here rather than at the end of the tail.
	 */
	private void beginHold(DollEntity doll) {
		phase = HOLD;
		ticks = 0;
		doll.setSpeedCap(RETURN_SPEED);
	}

	/**
	 * Park on the spot for {@link #HOLD_TICKS}, facing whatever the body was
	 * turned onto at impact (the target), then turn for home. Velocity is pinned
	 * rather than merely not set: the recoil from the swing still has to be eaten
	 * here, and a doll is no-gravity, so an unpinned tick drifts off the mark the
	 * strike just made.
	 */
	private void hold(DollEntity doll) {
		doll.setDeltaMovement(Vec3.ZERO);
		if (++ticks >= HOLD_TICKS) beginReturn(doll);
	}

	private void beginReturn(DollEntity doll) {
		phase = RETURN;
		ticks = 0;
		halfLeg = home.subtract(doll.position()).length() * 0.5;
	}

	private void flyHome(DollEntity doll) {
		ticks++;
		Vec3 back = home.subtract(doll.position());
		double dist = back.length();
		if (dist < 0.4) {
			doll.setDeltaMovement(Vec3.ZERO);
			return;
		}
		faceDir(doll, back);
		doll.setDeltaMovement(back.scale(RETURN_SPEED / dist));
	}

}
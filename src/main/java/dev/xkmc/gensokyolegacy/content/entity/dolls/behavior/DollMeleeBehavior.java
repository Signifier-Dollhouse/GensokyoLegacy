package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.content.item.doll.DollLanceItem;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Melee regular attack: charge at twice the movement cap, swing on contact, recoil, hold, fly home. Bare hands use
 * toy_slapattack. The ticket releases on impact, so the tail runs through {@link #selfDriven}. Charge is plain
 * velocity, not a path, so it lifts the speed cap and is leash-exempt, bounded by {@link #CHARGE_REACH} instead.
 */
public class DollMeleeBehavior extends DollBehavior {

	/**
	 * Targets further away than this are left to the ranged behaviors.
	 */
	private static final double ENGAGE_RANGE = 16.0;

	/**
	 * Contact range that lands the swing (bounding-box distance).
	 */
	private static final double MELEE_RANGE = 2.5;

	/**
	 * The charge's own leash, standing in for the owner ring while it lasts: past everything the engage range admits
	 * (16 + contact) and under the 48-block ledger pullback once the formation radius is added.
	 */
	private static final double CHARGE_REACH = 20.0;

	/**
	 * Charge speed: twice the movement cap, which the charge raises to match.
	 */
	private static final double CHARGE_SPEED = BaseDollEntity.MAX_SPEED * 2;

	/**
	 * Return speed: ordinary flight, i.e. the cap again.
	 */
	private static final double RETURN_SPEED = BaseDollEntity.MAX_SPEED;

	/**
	 * Fraction of the charge velocity the impact bounce kicks back with.
	 */
	private static final double RECOIL = 0.5;

	/**
	 * Blocks per tick the struck target is pushed, vanilla's default punch.
	 */
	private static final double KNOCKBACK = 0.5;

	private static final int COOLDOWN_TICKS = 20;

	/**
	 * Charge budget, and the backstop for a target circling inside {@link #CHARGE_REACH} that can never be caught.
	 */
	private static final int CHARGE_TICKS = 40;

	/**
	 * Ticks of pressing into something before the charge gives up — see the stall
	 * guard in {@link #tick}.
	 */
	private static final int STALL_TICKS = 10;

	/**
	 * Hang on the spot the swing landed on before turning for home; without the beat the doll snaps from "in the
	 * target's face" straight to "flying away" and the swing never reads. Fixed, not budgeted, so it cannot strand a
	 * doll.
	 */
	private static final int HOLD_TICKS = 6;
	private static final int LANCE_HOLD_TICKS = 9;

	/**
	 * Return budget: the way home is straight and at full flight speed.
	 */
	private static final int RETURN_TICKS = 60;

	private static final int CHARGE = 0;
	private static final int HOLD = 1;
	private static final int RETURN = 2;

	private int phase;
	private int ticks;
	private int stalled;
	private int holdTicks;

	/**
	 * Where the charge began, and the return destination. Set in {@link #start}.
	 */
	private Vec3 home = Vec3.ZERO;

	/**
	 * Half of the impact → home leg: the release point of the return flight.
	 */
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
		// a ticket that showed up after the swing preempts the tail: neither the hold
		// nor the flight home ever outlives an order
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
	 * Waiting in formation never closes the gap to {@link #ENGAGE_RANGE}, so a target past it is one this doll can
	 * never go and get: the wait gives the ticket up at once instead of sitting on it.
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
		// contact first, stall second: a doll that scraped a wall on the way in still
		// swings when it arrives, and a straight line has no detour to give
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
	 * The contact tick: swing and hand the attack on, the ticket going out so the volley moves on while the doll still
	 * flies; the bounce is the charge velocity reverted, which reads as recoil. A bare hand arrives here as the
	 * registry's fallback naming the empty hand, so {@link #ensureMainHand} frees the striking hand.
	 */
	private void strike(DollEntity doll, LivingEntity target, Vec3 toTarget) {
		Optional<HandMatch> match = hand(doll);
		if (match.isEmpty()) {
			abort(doll);
			return;
		}
		boolean bareHand = match.get().bareHand();
		ensureMainHand(doll, match.get());
		swing(doll, doll.ledgerStack(DollSlot.MAIN_HAND), target, toTarget);
		if (bareHand) doll.broadcastSlapAnim();
		else doll.broadcastAttackAnim();
		doll.actions.stamp(type(), doll.level().getGameTime());
		doll.actions.complete(doll);
		beginHold(doll, bareHand);
		doll.setDeltaMovement(doll.getDeltaMovement().scale(-RECOIL));
	}

	private void swing(DollEntity doll, ItemStack weapon, LivingEntity target, Vec3 toTarget) {
		// The doll is the direct source, its owner the causing one; GLAttackListener#onCreateSource
		// turns every doll melee into a bypass-cooldown variant.
		LivingEntity owner = doll.getOwner();
		DamageSource source = owner == null
				? doll.damageSources().mobAttack(doll)
				: doll.damageSources().source(DamageTypes.MOB_ATTACK, doll, owner);
		if (!target.hurt(source, meleeDamage(doll, weapon))) return;
		Vec3 push = new Vec3(toTarget.x, 0, toTarget.z);
		if (push.lengthSqr() < 1e-6) return;
		push = push.normalize();
		target.knockback(KNOCKBACK, push.x, push.z);
	}

	/**
	 * Base {@link Attributes#ATTACK_DAMAGE} plus the ledger weapon's main-hand modifiers — the price of the loadout not
	 * being vanilla equipment (loadout.md §2): nothing feeds the held stack's {@link ItemAttributeModifiers} into the
	 * attribute map, so a bare {@link DollLanceItem} swing is 5 (1 + its 4) and an empty-handed slap the bare 1.
	 */
	private float meleeDamage(DollEntity doll, ItemStack weapon) {
		double weaponDamage = weapon.getAttributeModifiers().modifiers().stream()
				.filter(entry -> Attributes.ATTACK_DAMAGE.equals(entry.attribute()))
				.filter(entry -> entry.slot().test(EquipmentSlot.MAINHAND))
				.mapToDouble(entry -> entry.modifier().amount())
				.sum();
		return (float) (doll.getAttributeValue(Attributes.ATTACK_DAMAGE) + weaponDamage);
	}

	/**
	 * No hit, but the ticket still goes out — a charge given up costs nothing.
	 */
	private void abort(DollEntity doll) {
		doll.actions.complete(doll);
		doll.setSpeedCap(RETURN_SPEED);
		beginReturn(doll);
	}

	/**
	 * The strike landed: the charge is over, so the doubled cap drops back here.
	 */
	private void beginHold(DollEntity doll, boolean bareHand) {
		phase = HOLD;
		ticks = 0;
		holdTicks = bareHand ? HOLD_TICKS : LANCE_HOLD_TICKS;
		doll.setSpeedCap(RETURN_SPEED);
	}

	/**
	 * Pin velocity rather than merely not setting it: the swing recoil still has to be eaten and a doll is no-gravity,
	 * so an unpinned tick drifts off the mark the strike just made.
	 */
	private void hold(DollEntity doll) {
		doll.setDeltaMovement(Vec3.ZERO);
		if (++ticks >= holdTicks) beginReturn(doll);
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

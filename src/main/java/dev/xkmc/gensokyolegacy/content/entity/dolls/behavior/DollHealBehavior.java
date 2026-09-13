package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.attachment.doll.MutableDollInventory;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Heal: approach to ~2 blocks (self needs no move) and trigger the held folded
 * heal talisman on the target, wearing the live ledger stack in place. Issued as
 * a one-time order by the scheduler; it holds its ticket to completion and only
 * stop() interrupts it.
 */
public class DollHealBehavior extends DollBehavior {

	private static final double APPROACH_DIST = 2.0;

	/** Scheduling radius for marked targets (scheduling filters here; this only enforces leash + arrival). */
	public static final double MARK_RANGE = 16.0;

	private int navCooldown;

	@Override
	public DollActionType type() {
		return DollActionType.HEAL;
	}

	@Override
	public boolean canUse(DollEntity doll) {
		if (!isServer(doll)) return false;
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		return usableTarget(doll, target) && hand(doll).isPresent();
	}

	@Override
	public boolean canContinueToUse(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		return usableTarget(doll, target) && hand(doll).isPresent();
	}

	private boolean usableTarget(DollEntity doll, LivingEntity target) {
		if (target == null) return false;
		if (target == doll) return true;
		Optional<HandMatch> match = hand(doll);
		if (match.isEmpty()) return false;
		Optional<DollBehaviors.HealSetup> setup = DollBehaviors.findHealPaper(match.get().stack());
		if (setup.isEmpty()) return false;
		if (!setup.get().paper().test(target)) return false;
		return doll.distanceTo(target) <= MARK_RANGE || arrivedAt(doll, target.position(), APPROACH_DIST);
	}

	@Override
	public void start(DollEntity doll) {
		halt(doll);
		navCooldown = 0;
	}

	@Override
	public void stop(DollEntity doll) {
		halt(doll);
		navCooldown = 0;
	}

	@Override
	public void tick(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return;
		LivingEntity target = resolveTarget(doll, action);
		if (!usableTarget(doll, target)) {
			doll.actions.complete(doll);
			return;
		}
		if (target != doll) {
			faceTarget(doll, target);
			Vec3 dest = target.position();
			if (!arrivedAt(doll, dest, APPROACH_DIST)) {
				if (!destWithinLeash(doll, dest)) {
					doll.actions.complete(doll);
					return;
				}
				if (--navCooldown <= 0) {
					navCooldown = 10;
					doll.getNavigation().moveTo(dest.x, dest.y, dest.z, doll.speedModifier);
				}
				return;
			}
			halt(doll);
		}
		trigger(doll, target);
	}

	private boolean arrivedAt(DollEntity doll, Vec3 dest, double dist) {
		return doll.distanceToSqr(dest.x, dest.y, dest.z) <= dist * dist;
	}

	private void trigger(DollEntity doll, LivingEntity target) {
		Optional<HandMatch> match = hand(doll);
		if (match.isEmpty()) {
			doll.actions.complete(doll);
			return;
		}
		ensureMainHand(doll, match.get());
		MutableDollInventory inv = doll.loadout();
		ItemStack live = inv.get(DollSlot.MAIN_HAND);
		Optional<DollBehaviors.HealSetup> setup = DollBehaviors.findHealPaper(live);
		if (setup.isEmpty() || !setup.get().paper().test(target)) {
			doll.actions.complete(doll);
			return;
		}
		setup.get().paper().trigger(new TalismanContext(target, live, 0, live, setup.get().paper()));
		if (live.isEmpty()) inv.set(DollSlot.MAIN_HAND, ItemStack.EMPTY);
		doll.syncLoadoutMirror();
		doll.actions.complete(doll);
	}

}

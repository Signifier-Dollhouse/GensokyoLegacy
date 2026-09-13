package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.content.entity.misc.HexBrewBottleEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrewBottleItem;
import dev.xkmc.gensokyolegacy.util.DollShootUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Super attack, hexbrew variant: face the target, throw one bottle, done.
 * Only runs when no laser is held anywhere (registry priority decides). Sidesteps
 * while an ally blocks the lane, throwing anyway past the strafe budget.
 */
public class DollThrowBehavior extends DollBehavior {

	private static final double RANGE = 16.0;
	private static final float THROW_VELOCITY = 1.5F;

	/**
	 * Strafe budget: an ally-blocked lane sidesteps this long, then the check is
	 * skipped and the bottle flies anyway.
	 */
	private static final int STRAFE_SKIP_TICKS = 20;

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
		return match.isPresent() && match.get().stack().getItem() instanceof HexBrewBottleItem;
	}

	@Override
	public boolean canContinueToUse(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		if (target == null || !inRange(doll, target, RANGE)) return false;
		Optional<HandMatch> match = hand(doll);
		return match.isPresent() && match.get().stack().getItem() instanceof HexBrewBottleItem;
	}

	@Override
	public void start(DollEntity doll) {
		blockedTicks = 0;
		navCooldown = 0;
		halt(doll);
	}

	@Override
	public void stop(DollEntity doll) {
		halt(doll);
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
		Optional<HandMatch> match = hand(doll);
		if (match.isEmpty() || !(match.get().stack().getItem() instanceof HexBrewBottleItem)) {
			doll.actions.complete(doll);
			return;
		}
		ensureMainHand(doll, match.get());
		ItemStack stack = doll.ledgerStack(DollSlot.MAIN_HAND);
		if (!(stack.getItem() instanceof HexBrewBottleItem)) {
			doll.actions.complete(doll);
			return;
		}
		Vec3 eye = doll.getEyePosition();
		HexBrewBottleEntity bottle = new HexBrewBottleEntity(doll.level(), doll);
		bottle.setPos(eye);
		bottle.setItem(stack.copy());
		DollShootUtils.shootAimHelper(target, bottle, THROW_VELOCITY, 0.03f);
		doll.level().addFreshEntity(bottle);
		doll.consumeLoadoutItem(DollSlot.MAIN_HAND, 1);
		doll.actions.complete(doll);
	}

}

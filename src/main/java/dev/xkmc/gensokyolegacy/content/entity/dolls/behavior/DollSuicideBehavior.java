package dev.xkmc.gensokyolegacy.content.entity.dolls.behavior;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollAction;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry.HandMatch;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Suicide attack: go stray (cut from pairing, no leash), dive at the target and
 * explode — entities only, terrain never breaks. The TNT is consumed, then the
 * doll is guaranteed dead so the stray death drop returns the item form with all
 * other gear intact.
 */
public class DollSuicideBehavior extends DollBehavior {

	private static final double BLAST_RANGE = 2.0;

	private int navCooldown;

	@Override
	public DollActionType type() {
		return DollActionType.SUICIDE_ATTACK;
	}

	@Override
	public boolean canUse(DollEntity doll) {
		if (!isServer(doll)) return false;
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		if (target == null) return false;
		return hand(doll).isPresent();
	}

	@Override
	public boolean canContinueToUse(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return false;
		LivingEntity target = resolveTarget(doll, action);
		return target != null && hand(doll).isPresent();
	}

	@Override
	public void start(DollEntity doll) {
		Optional<HandMatch> match = hand(doll);
		match.ifPresent(m -> ensureMainHand(doll, m));
		doll.becomeStray();
		doll.actions.setKamikaze(true);
		navCooldown = 0;
	}

	@Override
	public void stop(DollEntity doll) {
		halt(doll);
		navCooldown = 0;
		doll.actions.setKamikaze(false);
	}

	@Override
	public void tick(DollEntity doll) {
		DollAction action = current(doll);
		if (action == null) return;
		LivingEntity target = resolveTarget(doll, action);
		if (target == null) return;
		faceTarget(doll, target);
		Vec3 dest = target.position();
		if (doll.distanceTo(target) <= BLAST_RANGE) {
			detonate(doll, dest);
			return;
		}
		if (--navCooldown <= 0) {
			navCooldown = 10;
			doll.getNavigation().moveTo(dest.x, dest.y, dest.z, doll.speedModifier);
		}
	}

	private void detonate(DollEntity doll, Vec3 dest) {
		if (!(doll.level() instanceof ServerLevel level)) return;
		Optional<HandMatch> match = DollBehaviorRegistry.findHand(doll, DollActionType.SUICIDE_ATTACK);
		if (match.isEmpty()) {
			doll.actions.complete(doll);
			return;
		}
		// same blast as an explosive hexbrew: power 4, terrain kept, thrower and
		// allies excluded — the doll itself is excluded as thrower, so death below
		// is guaranteed by the fallback, not the blast.
		HexBrew.EXPLOSIVE_HEXBREW.handler.onHit(level, dest, doll, match.get().stack());
		doll.consumeLoadoutItem(match.get().hand(), 1);
		if (!doll.isDeadOrDying()) doll.hurt(doll.damageSources().genericKill(), Float.MAX_VALUE);
		doll.actions.complete(doll);
	}

}

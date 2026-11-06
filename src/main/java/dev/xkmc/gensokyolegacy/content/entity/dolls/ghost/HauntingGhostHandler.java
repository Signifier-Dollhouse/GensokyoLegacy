package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.spell.mover.PolarMover;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.fastprojectileapi.collision.EntityStorageHelper;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollRig;
import dev.xkmc.gensokyolegacy.content.entity.misc.ButterflyDanmakuBulletEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The haunting ghost: an ordinary ledger ghost that ignores its follow goals while it has a target,
 * flies straight at it through blocks like a vex, and volleys butterflies. With no target it does
 * nothing and the base ghost follows.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
final class HauntingGhostHandler implements GhostVariantHandler {

	private static final int ATTACK_LOCK = 40;
	private static final int ATTACK_INVISIBLE = 40;
	private static final int ATTACK_COOLDOWN = 20;
	private static final double ATTACK_RANGE = 3;

	@Override
	public Component getDisplayName() {
		return GLLang.Ghost.HAUNTING.get();
	}

	@Override
	public boolean wild() {
		return false;
	}

	@Override
	public boolean summonable() {
		return false;
	}

	@Override
	public boolean releasable() {
		return false;
	}

	@Override
	public boolean leavesWhenWeak() {
		return false;
	}

	@Override
	public boolean followsOwner(GhostEntity ghost) {
		LivingEntity target = ghost.getTarget();
		return target == null || !target.isAlive() || target.level() != ghost.level();
	}

	@Override
	public boolean isStationary(GhostEntity ghost) {
		return state(ghost).lockTicks > 0;
	}

	@Override
	public boolean phasesThroughBlocks(GhostEntity ghost) {
		return true;
	}

	@Override
	public boolean fliesDirectly(GhostEntity ghost) {
		return true;
	}

	@Override
	public void hunt(GhostEntity ghost, LivingEntity target) {
		HauntingGhostModule state = state(ghost);
		state.lockTicks = 0;
		state.invisibleTicks = 0;
		state.attackCooldown = 0;
		ghost.setInvisible(false);
		ghost.setTarget(target);
		ghost.getNavigation().stop();
		ghost.stopMove();
	}

	@Override
	public void tick(GhostEntity ghost) {
		HauntingGhostModule state = state(ghost);
		if (ghost.getPower() <= 0) {
			ghost.vanish();
			return;
		}
		if (state.invisibleTicks > 0 && --state.invisibleTicks == 0) ghost.setInvisible(false);
		if (state.attackCooldown > 0) state.attackCooldown--;
		if (state.lockTicks > 0) {
			// the cooldown is counted from the end of the lock, not from the volley
			if (--state.lockTicks == 0) state.attackCooldown = ATTACK_COOLDOWN;
			ghost.stopMove();
			LivingEntity target = ghost.getTarget();
			if (target != null) ghost.getLookControl().setLookAt(target, 30, 30);
			return;
		}
		LivingEntity target = ghost.getTarget();
		if (target == null || !target.isAlive() || target.level() != ghost.level()) {
			ghost.setTarget(null);
			ghost.stopMove();
			return;
		}
		ghost.getLookControl().setLookAt(target, 30, 30);
		if (ghost.distanceToSqr(target) <= ATTACK_RANGE * ATTACK_RANGE) {
			ghost.stopMove();
			if (state.attackCooldown <= 0) attack(ghost, target);
			return;
		}
		// straight at the target, through any block: no path to be blocked or rebuilt
		Vec3 eye = target.getEyePosition();
		ghost.getMoveControl().setWantedPosition(eye.x, eye.y, eye.z, 1);
	}

	private void attack(GhostEntity ghost, LivingEntity target) {
		int cost = GLModConfig.SERVER.hauntingSkillCost.get();
		if (ghost.getPower() < cost) {
			ghost.vanish();
			return;
		}
		ghost.addPower(-cost);
		spawnButterflies(ghost, target);
		HauntingGhostModule state = state(ghost);
		state.lockTicks = ATTACK_LOCK;
		state.invisibleTicks = ATTACK_INVISIBLE;
		ghost.setInvisible(true);
		ghost.level().broadcastEntityEvent(ghost, DollRig.EVENT_SKILL);
		if (ghost.getPower() <= 0) ghost.vanish();
	}

	/**
	 * A clockwise and a counterclockwise butterfly for every centre in three concentric rings. The
	 * plane is tilted through the target, so the whole flower spreads around it; every butterfly
	 * starts and ends at the ghost because each orbit radius matches its ring radius.
	 */
	private void spawnButterflies(GhostEntity ghost, LivingEntity target) {
		ServerLevel level = (ServerLevel) ghost.level();
		Vec3 origin = ghost.position().add(0, ghost.getBbHeight() / 2, 0);
		Vec3 aim = target.getBoundingBox().getCenter().subtract(origin);
		Vec3 dir = aim.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : aim.normalize();
		var ori = DanmakuHelper.getOrientation(dir);
		var nor0 = ori.rotateDegrees(90, 70);
		var nor1 = ori.rotateDegrees(-90, 70);
		var pl0 = DanmakuHelper.getOrientation(dir, nor0);
		var pl1 = DanmakuHelper.getOrientation(dir, nor1);

		int rings = 3;
		float r0 = 1, dr = 0.5f;
		for (int layer = 0; layer < rings; layer++) {
			double radius = r0 + dr * layer;
			int m = 6 + layer * 2;
			spawn(level, ghost, origin, pl0, radius, m, 1);
			spawn(level, ghost, origin, pl1, radius, m, -1);
		}
	}

	private void spawn(ServerLevel level, GhostEntity ghost, Vec3 origin, DanmakuHelper.Orientation plane, double radius, int m, int alt) {
		double w = Math.PI * 2 / ATTACK_LOCK;
		var normal = plane.normal();
		var item = (alt > 0 ? GLItems.PINK_BUTTERFLY : GLItems.BLUE_BUTTERFLY).asStack();
		for (int i = 0; i < m; i++) {
			Vec3 out = plane.rotateDegrees(i * 360.0 / m);
			Vec3 center = origin.add(out.scale(radius));
			Vec3 back = out.scale(-1);
			Vec3 tangent = back.cross(normal).normalize().scale(w * radius * alt);
			ItemBulletEntity danmaku = new ButterflyDanmakuBulletEntity(GLEntities.BUTTERFLY_DANMAKU.get(), ghost, level);
			danmaku.setItem(item);
			danmaku.setup(DanmakuItems.Bullet.BUTTERFLY.damage(), ATTACK_LOCK, true, true, tangent);
			danmaku.setPos(origin);
			danmaku.mover = PolarMover.ofPlane(center, back, normal)
					.radial(radius, 0, 0)
					.angular(0, w * alt, 0);
			EntityStorageHelper.fastAdd(level, danmaku);
		}
	}

	private static HauntingGhostModule state(GhostEntity ghost) {
		return ghost.getModule(HauntingGhostModule.class);
	}

}

package dev.xkmc.gensokyolegacy.content.entity.misc.anchor;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.spell.mover.RectMover;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.fastprojectileapi.collision.EntityStorageHelper;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Wetness, trail, burst and area damage, shared by the bounded hook and the unbound bullets.
 * The bounded anchor is a real entity and reads its fluid state; a danmaku bullet's is a no-op
 * and samples the block it is in plus its owner.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public final class AnchorStorm {

	private static final int MAX_TRAIL = 160;
	private static final double TRAIL_SPEED = 0.7;
	private static final int TRAIL_LIFE = 40;
	private static final int BURST_COUNT = 100;
	private static final double BURST_RADIUS = 4;
	private static final float BURST_DAMAGE = 6;

	private AnchorStorm() {
	}

	public static boolean wetAt(Level level, Vec3 pos) {
		BlockPos bp = BlockPos.containing(pos);
		if (level.getFluidState(bp).is(FluidTags.WATER)) return true;
		if (level.getBlockState(bp).is(Blocks.BUBBLE_COLUMN)) return true;
		return level.isRainingAt(bp);
	}

	public static boolean isWet(Entity self) {
		return self.isInWaterRainOrBubble() || wetAt(self.level(), self.position());
	}

	/**
	 * Sheds the four-bullet cross while the storm is wet; returns the new trail budget.
	 */
	public static int shedTrail(Entity self, Entity owner, boolean wet, int spent) {
		if (!wet || spent >= MAX_TRAIL || !stormDanmaku()) return spent;
		Vec3 vec = self.getDeltaMovement();
		if (vec.lengthSqr() < 1e-6) return spent;
		var orientation = DanmakuHelper.getOrientation(vec.normalize());
		for (int i = 0; i < 4; i++) {
			Vec3 dir = orientation.rotateDegrees(90 * i + self.tickCount * 15);
			spawn(self, owner, dir, TRAIL_LIFE + self.getRandom().nextInt(11), null);
		}
		return spent + 4;
	}

	/**
	 * Area damage on impact, plus the gravitational burst when the impact was wet.
	 */
	public static void impact(Entity self, Entity owner, boolean wet) {
		areaDamage(self, owner);
		if (wet && stormDanmaku()) burst(self, owner);
	}

	private static void areaDamage(Entity self, Entity owner) {
		if (owner == null) return;
		Vec3 at = self.position();
		DamageSource source = self.damageSources().thrown(self, owner);
		AABB box = new AABB(at.subtract(BURST_RADIUS, BURST_RADIUS, BURST_RADIUS),
				at.add(BURST_RADIUS, BURST_RADIUS, BURST_RADIUS));
		for (LivingEntity target : self.level().getEntitiesOfClass(LivingEntity.class, box)) {
			if (target == owner) continue;
			if (!IDanmakuEntity.canHurt(owner, target)) continue;
			if (target.position().distanceToSqr(at) > BURST_RADIUS * BURST_RADIUS) continue;
			target.hurt(source, BURST_DAMAGE);
		}
	}

	private static void burst(Entity self, Entity owner) {
		Vec3 forward = self.getDeltaMovement().normalize();
		if (forward.lengthSqr() < 1e-6) forward = new Vec3(0, 1, 0);
		for (int i = 0; i < BURST_COUNT; i++) {
			Vec3 dir = new Vec3(self.getRandom().nextGaussian(), self.getRandom().nextGaussian(),
					self.getRandom().nextGaussian()).add(forward.scale(1.2));
			if (dir.lengthSqr() < 1e-6) dir = forward;
			spawn(self, owner, dir.normalize(), TRAIL_LIFE + self.getRandom().nextInt(11),
					new Vec3(0, -0.05, 0));
		}
	}

	private static void spawn(Entity self, Entity owner, Vec3 dir, int life, Vec3 accel) {
		if (!(self.level() instanceof ServerLevel sl)) return;
		ItemStack stack = new ItemStack(DanmakuItems.Bullet.CIRCLE.get(DyeColor.BLUE).get());
		ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), 0, 0, 0, sl);
		if (owner != null) danmaku.setOwner(owner);
		danmaku.setItem(stack);
		danmaku.setup(1, life, false, false, dir.scale(TRAIL_SPEED));
		danmaku.setPos(self.position());
		if (accel != null) danmaku.mover = new RectMover(self.position(), dir.scale(TRAIL_SPEED), accel);
		EntityStorageHelper.fastAdd(sl, danmaku);
	}

	private static boolean stormDanmaku() {
		return GLModConfig.SERVER.anchorStormDanmaku.get();
	}

}

package dev.xkmc.gensokyolegacy.content.entity.misc.anchor;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorThrow;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One unbound storm anchor: flies straight to a fixed reach, sheds a falling danmaku every tick,
 * bursts on impact and vanishes. Every storm throw is this entity.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
@SerialClass
public class AnchorBulletEntity extends ItemBulletEntity {

	@SerialField
	private Vec3 origin = Vec3.ZERO;
	@SerialField
	private int trail = 0;

	public AnchorBulletEntity(EntityType<? extends ItemBulletEntity> type, Level level) {
		super(type, level);
	}

	public AnchorBulletEntity(EntityType<? extends ItemBulletEntity> type, LivingEntity shooter, Level level) {
		super(type, shooter, level);
	}

	public void configure(Vec3 origin) {
		this.origin = origin;
	}

	/**
	 * Always the water anchor: an unbound anchor is water-borne and always sheds its trail.
	 */
	@Override
	public ItemStack getItem() {
		return GLItems.ANCHOR_WET.get().getDefaultInstance();
	}

	@Override
	public AABB getBoundingBoxForEntityHit() {
		return super.getBoundingBoxForEntityHit().inflate(1);
	}

	@Override
	public void tick() {
		super.tick();
		if (isRemoved()) return;
		if (level().isClientSide) {
			spawnWaterSwirl();
			return;
		}
		trail = AnchorStorm.shedTrail(this, getOwner(), trail);
		if (position().distanceTo(origin) > AnchorThrow.STORM.reach()) markErased(false);
	}

	private void spawnWaterSwirl() {
		Vec3 motion = getDeltaMovement();
		Vec3 forward = motion.normalize();
		if (forward.lengthSqr() == 0) return;
		var ring = DanmakuHelper.getOrientation(forward).asNormal();
		Vec3 center = position().add(0, getBbHeight() / 2, 0);
		for (int sample = 0; sample < 2; sample++) {
			double step = sample * 0.5;
			double angle = (tickCount - step) * Math.PI / 3;
			Vec3 at = center.subtract(motion.scale(step));
			for (int side = 0; side < 2; side++) {
				Vec3 offset = ring.rotate(angle + side * Math.PI).scale(0.75);
				Vec3 drop = at.add(offset);
				level().addParticle(ParticleTypes.SPLASH, drop.x, drop.y, drop.z, offset.x * 0.1, 0, offset.z * 0.1);
			}
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (level().isClientSide) return;
		AnchorStorm.smash(level(), result.getBlockPos());
		AnchorStorm.impact(this, getOwner());
		markErased(false);
	}

	@Override
	public void onHitEntity(EntityHitResult result) {
		if (level().isClientSide) return;
		AnchorStorm.smashAt(level(), result.getEntity().position());
		AnchorStorm.impact(this, getOwner());
		markErased(false);
	}

}

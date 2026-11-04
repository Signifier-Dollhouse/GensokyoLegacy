package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.effect.HauntingEffect;
import dev.xkmc.gensokyolegacy.content.item.ghost.ButterflyDanmakuItem;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A butterfly danmaku that leaves a trail of short-lived, shrinking petals. The particle comes
 * from the item, so the pink butterfly trails pink petals and the blue one purple.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
@SerialClass
public class ButterflyDanmakuBulletEntity extends ItemBulletEntity {

	/**
	 * Only the Yuyuko fan's butterflies carry the haunting effect; the ghost's own volley must not
	 * inflict what it is born from.
	 */
	@SerialField
	private boolean haunting = false;

	public ButterflyDanmakuBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public ButterflyDanmakuBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		super(pEntityType, pShooter, pLevel);
	}

	public void setHaunting(boolean haunting) {
		this.haunting = haunting;
	}

	@Override
	public void onHitEntity(EntityHitResult result) {
		LivingEntity owner = !level().isClientSide && haunting && getOwner() instanceof LivingEntity living ? living : null;
		super.onHitEntity(result);
		if (owner != null && result.getEntity() instanceof LivingEntity target)
			HauntingEffect.inflict(target, owner);
	}

	/**
	 * One petal every other tick: a single butterfly is subtle, while the fan's hundred-odd
	 * butterflies would otherwise put thousands of petals in the air at once.
	 */
	@Override
	public void tick() {
		super.tick();
		if (!(getItem().getItem() instanceof ButterflyDanmakuItem item)) return;
		Vec3 pos = position().add(0, getBbHeight() / 2, 0);
		level().addParticle(item.trailParticle(), pos.x, pos.y, pos.z, 0, 0, 0);
	}

}

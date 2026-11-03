package dev.xkmc.gensokyolegacy.content.entity.misc.anchor;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorThrow;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One unbound storm anchor: flies straight to a fixed reach, sheds the wet trail, bursts on impact
 * and vanishes. The only anchor throw that stays a danmaku.
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
	 * The water-anchor twin while wet; the real stack never changes.
	 */
	@Override
	public ItemStack getItem() {
		return isWet() ? GLItems.ANCHOR_WET.get().getDefaultInstance() : super.getItem();
	}

	/**
	 * A danmaku's own fluid state never updates, so the block sample and the thrower stand in.
	 */
	public boolean isWet() {
		return AnchorStorm.isWet(this) ||
				getOwner() instanceof LivingEntity owner && owner.isInWaterRainOrBubble();
	}

	@Override
	public AABB getBoundingBoxForEntityHit() {
		return super.getBoundingBoxForEntityHit().inflate(1);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide || isRemoved()) return;
		trail = AnchorStorm.shedTrail(this, getOwner(), isWet(), trail);
		if (position().distanceTo(origin) > AnchorThrow.STORM_UNBOUND.reach()) markErased(false);
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (level().isClientSide) return;
		AnchorStorm.impact(this, getOwner(), isWet());
		markErased(false);
	}

	@Override
	public void onHitEntity(EntityHitResult result) {
		if (level().isClientSide) return;
		AnchorStorm.impact(this, getOwner(), isWet());
		markErased(false);
	}

}

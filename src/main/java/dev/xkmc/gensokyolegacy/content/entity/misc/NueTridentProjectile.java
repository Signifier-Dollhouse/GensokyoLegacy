package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.fastprojectileapi.entity.ProjectileMovement;
import dev.xkmc.gensokyolegacy.content.item.nue.UfoItem;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

/**
 * The copy Nue's trident throws: it flies the vanilla trident arc, hits once, and leaves a UFO in
 * the target's hand where its item used to be.
 *
 * @see "doc/design/nue_trident.md"
 */
@SerialClass
public class NueTridentProjectile extends ItemBulletEntity {

	private static final float DAMAGE = 8;

	@SerialField
	private boolean dealtDamage = false;
	/**
	 * The thrower's attack damage at the throw; the constant is the fallback for a copy that lost it.
	 */
	@SerialField
	private float damage = DAMAGE;

	public NueTridentProjectile(EntityType<? extends ItemBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public NueTridentProjectile(EntityType<? extends ItemBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		super(pEntityType, pShooter, pLevel);
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	@Override
	protected ProjectileMovement updateVelocity(Vec3 vec, Vec3 pos) {
		return ProjectileMovement.of(vec.scale(0.99).subtract(0, 0.05, 0));
	}

	@Override
	public AABB getBoundingBoxForEntityHit() {
		return super.getBoundingBoxForEntityHit().inflate(0.2);
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (!level().isClientSide) {
			level().playSound(null, this, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1F, 1F);
			markErased(false);
		}
	}

	@Override
	public void onHitEntity(EntityHitResult result) {
		if (level().isClientSide || dealtDamage) return;
		dealtDamage = true;
		Entity target = result.getEntity();
		while (target instanceof PartEntity<?> part) target = part.getParent();
		if (target instanceof LivingEntity living) {
			Entity owner = getOwner();
			living.hurt(damageSources().thrown(this, owner), damage);
			level().playSound(null, this, SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1F, 1F);
			convert(living);
		}
		markErased(false);
	}

	/**
	 * Puts a UFO in the hand the target was using, content and count intact, and makes that hand
	 * drop it on death. Empty hands, a target already holding a UFO, and non-living targets are
	 * damaged only.
	 */
	private static void convert(LivingEntity target) {
		InteractionHand hand = InteractionHand.MAIN_HAND;
		ItemStack held = target.getMainHandItem();
		if (held.isEmpty()) {
			hand = InteractionHand.OFF_HAND;
			held = target.getOffhandItem();
		}
		if (held.isEmpty() || held.getItem() instanceof UfoItem) return;
		target.setItemInHand(hand, UfoItem.wrap(held));
		if (target instanceof Mob mob) {
			mob.setGuaranteedDrop(hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		}
		if (target instanceof Player player) player.stopUsingItem();
	}

}

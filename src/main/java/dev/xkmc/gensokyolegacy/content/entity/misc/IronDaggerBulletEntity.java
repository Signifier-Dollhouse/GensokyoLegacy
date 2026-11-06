package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveRune;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveRunes;
import dev.xkmc.gensokyolegacy.content.item.tool.IronDaggerItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The thrown form of {@link IronDaggerItem}: hands the dagger back instead of just vanishing. Whether it comes
 * back is settled by the thrower via {@link #setReturnable}, the only moment that knows whether it was paid for.
 *
 * @see "doc/design/dagger_glove.md"
 */
@SerialClass
public class IronDaggerBulletEntity extends ItemBulletEntity {

	@SerialField
	private boolean returnable = false;
	@SerialField
	private boolean givenBack = false;
	/**
	 * Whether this dagger has already handed its return to a successor, and so must not hand it
	 * back itself. See {@link #handOffTo}.
	 */
	@SerialField
	private boolean handedOff = false;
	@SerialField
	private ResourceLocation rune = null;

	public IronDaggerBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public IronDaggerBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		super(pEntityType, pShooter, pLevel);
	}

	/**
	 * Declares up front that this dagger must come back, i.e. that its throw was paid for. An unflagged
	 * dagger is lost when it lands — what a creative throw or one cleared by {@link DanmakuUseEvent#consume()} wants.
	 */
	public void setReturnable(boolean pReturnable) {
		returnable = pReturnable;
	}

	/**
	 * Gives this dagger a {@link DaggerGloveRune} to run when it hits something, by id. The id is stored
	 * rather than the rune, so a rune since removed reads as no rune at the moment of the hit.
	 */
	public void setRune(@Nullable ResourceLocation pRune) {
		rune = pRune;
	}

	/**
	 * Claims this dagger's return for a successor: a homing shot is two entities for one spent dagger (dagger_glove.md
	 * §4).
	 *
	 * @return true if this dagger may now stop returning itself; false if the claim was taken or the dagger is already
	 * *       gone
	 */
	public boolean handOffTo(boolean successorSpawned) {
		if (!successorSpawned || !ownsReturn()) return false;
		handedOff = true;
		return true;
	}

	/**
	 * Whether this dagger is still the one that owes the thrower a return: it was thrown for a
	 * price, and neither it nor a successor it has spawned has handed that price back.
	 */
	private boolean ownsReturn() {
		return returnable && !givenBack && !handedOff;
	}

	@Override
	protected void onHitBlock(BlockHitResult pResult) {
		impact(SoundEvents.TRIDENT_HIT_GROUND);
		giveBack();
		super.onHitBlock(pResult);
	}

	@Override
	public void onHitEntity(EntityHitResult pResult) {
		impact(SoundEvents.TRIDENT_HIT);
		runRune(pResult);
		giveBack();
		super.onHitEntity(pResult);
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 vec3 = this.getDeltaMovement();
		double d5 = vec3.x;
		double d6 = vec3.y;
		double d1 = vec3.z;
		for (int i = 0; i < 4; ++i) {
			this.level().addParticle(GLParticles.KNIFE.get(), this.getX() + d5 * (double) i / (double) 4.0F, this.getY() + d6 * (double) i / (double) 4.0F, this.getZ() + d1 * (double) i / (double) 4.0F, -d5, -d6 + 0.2, -d1);
		}
	}

	/**
	 * Runs the carried rune against whatever was hit. The owner is passed as found: a dagger whose
	 * player has logged out still carries its rune, and a rune needing an owner simply does nothing.
	 */
	private void runRune(EntityHitResult pResult) {
		if (rune == null || level().isClientSide) return;
		Entity entity = pResult.getEntity();
		while (entity instanceof PartEntity<?> part) entity = part.getParent();
		if (!(entity instanceof LivingEntity target)) return;
		DaggerGloveRunes.get(rune).onHit((ServerLevel) level(),
				getOwner() instanceof LivingEntity owner ? owner : null, target, this);
	}

	@Override
	public void markErased(boolean kill) {
		giveBack();
		super.markErased(kill);
	}

	/**
	 * Puts the dagger in its owner's inventory, or at their feet when there is no room. Paying the return
	 * disarms {@code afterExpiry}: {@code BaseProjectile#tick} resolves the hit before expiry (dagger_glove.md §4a).
	 */
	private void giveBack() {
		if (givenBack || !returnable || handedOff || level().isClientSide) return;
		givenBack = true;
		afterExpiry = null;
		if (!(getOwner() instanceof Player player)) return;
		if (!player.isAlive() || player.level() != level()) return;
		ItemStack dagger = getItem();
		if (dagger.isEmpty()) return;
		if (!player.getInventory().add(dagger)) player.drop(dagger, false);
	}

	/**
	 * Plays the vanilla trident impact for whatever the dagger just struck. Deliberately kept out
	 * of {@link #giveBack}: a dagger that is lost rather than returned still landed with a clang.
	 */
	private void impact(SoundEvent pSound) {
		if (level().isClientSide) return;
		level().playSound(null, this, pSound, SoundSource.PLAYERS, 1F, 1F);
	}

}
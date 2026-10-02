package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.tool.IronDaggerItem;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * The thrown form of {@link IronDaggerItem}: an {@link ItemBulletEntity} that hands the dagger
 * back instead of just vanishing when it stops existing.
 * <p>
 * The dagger is consumed from the stack by the throw (see {@link IronDaggerItem#use}), so handing
 * it back is what makes the throw reusable instead of a lost item. A dagger with no player to
 * return to is gone for good, which is the only way to lose one.
 * <p>
 * Whether a given throw is to be handed back at all is settled by the thrower through
 * {@link #setReturnable} and then carried by the entity, rather than worked out again from the
 * item and the owner once the dagger has landed: the throw is the only moment that knows whether
 * the dagger was paid for, and a throw that was not (a creative player, or another mod clearing
 * the consume flag of the {@link DanmakuUseEvent} the throw posts) must not conjure one out of
 * thin air on the way back.
 * <p>
 * All three ways out are overridden rather than only the hit ones: landing on a block or on an
 * entity discards straight from {@code DanmakuBulletEntity}, while a dagger that flies its full
 * two seconds is erased through {@link #markErased} without ever hitting anything.
 * {@link #givenBack} keeps a dagger that goes out through both on the same tick (a hit on the
 * tick its life runs out) from being handed over twice.
 */
@SerialClass
public class IronDaggerBulletEntity extends ItemBulletEntity {

	@SerialField
	private boolean returnable = false;
	@SerialField
	private boolean givenBack = false;

	public IronDaggerBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public IronDaggerBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		super(pEntityType, pShooter, pLevel);
	}

	/**
	 * Declares up front that this dagger is one that must come back, i.e. that its throw was paid
	 * for out of the owner's stack. A dagger left unflagged is simply lost when it lands, which is
	 * what any throw that cost nothing wants.
	 */
	public void setReturnable(boolean pReturnable) {
		returnable = pReturnable;
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
		giveBack();
		super.onHitEntity(pResult);
	}

	@Override
	public void markErased(boolean kill) {
		giveBack();
		super.markErased(kill);
	}

	/**
	 * Puts the dagger in its owner's inventory, or at their feet when there is no room for it.
	 * <p>
	 * Nothing is handed back when there is no one to hand it to, and the dagger then vanishes as
	 * any other danmaku would: the throw was never flagged returnable (see {@link #setReturnable}),
	 * the owner is not a player (a youkai or doll throwing the item as danmaku), the player is dead
	 * or has logged out, or they left the level while the dagger was still in the air.
	 */
	private void giveBack() {
		if (givenBack || !returnable || level().isClientSide) return;
		givenBack = true;
		if (!(getOwner() instanceof Player player)) return;
		if (!player.isAlive() || player.level() != level()) return;
		ItemStack dagger = getItem();
		if (dagger.isEmpty()) return;
		if (!player.getInventory().add(dagger)) player.drop(dagger, false);
	}

	/**
	 * Plays the vanilla trident impact for whatever the dagger just struck, so that the throw
	 * reads as the weapon it imitates. Deliberately kept out of {@link #giveBack}: a dagger that is
	 * lost rather than returned still landed with a clang.
	 */
	private void impact(SoundEvent pSound) {
		if (level().isClientSide) return;
		level().playSound(null, this, pSound, SoundSource.PLAYERS, 1F, 1F);
	}

}
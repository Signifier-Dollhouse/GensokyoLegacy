package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.tool.IronDaggerItem;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
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
 * All three ways out are overridden rather than only the hit ones: landing on a block or on an
 * entity discards straight from {@code DanmakuBulletEntity}, while a dagger that flies its full
 * two seconds is erased through {@link #markErased} without ever hitting anything.
 * {@link #givenBack} keeps a dagger that goes out through both on the same tick (a hit on the
 * tick its life runs out) from being handed over twice.
 */
@SerialClass
public class IronDaggerBulletEntity extends ItemBulletEntity {

	@SerialField
	private boolean givenBack = false;

	public IronDaggerBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public IronDaggerBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		super(pEntityType, pShooter, pLevel);
	}

	@Override
	protected void onHitBlock(BlockHitResult pResult) {
		giveBack();
		super.onHitBlock(pResult);
	}

	@Override
	public void onHitEntity(EntityHitResult pResult) {
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
	 * any other danmaku would: the owner is not a player (a youkai or doll throwing the item as
	 * danmaku), the player is dead or has logged out, or they left the level while the dagger was
	 * still in the air. Creative throws are skipped too, as those never cost a dagger.
	 */
	private void giveBack() {
		if (givenBack || level().isClientSide) return;
		givenBack = true;
		if (!(getItem().getItem() instanceof IronDaggerItem)) return;
		if (!(getOwner() instanceof Player player) || player.getAbilities().instabuild) return;
		if (!player.isAlive() || player.level() != level()) return;
		ItemStack dagger = getItem();
		if (!player.getInventory().add(dagger)) player.drop(dagger, false);
	}

}
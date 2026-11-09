package dev.xkmc.gensokyolegacy.content.entity.misc;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.currency.CoinItem;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * The directly thrown form of {@link CoinItem}: instead of vanishing, it leaves the coin that
 * paid for it on the ground. Dolls shoot coins through the shared bullet, so their shots drop nothing.
 */
@SerialClass
public class CoinDanmakuBulletEntity extends ItemBulletEntity {

	@SerialField
	private boolean droppable = false;
	@SerialField
	private boolean dropped = false;

	public CoinDanmakuBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, Level pLevel) {
		super(pEntityType, pLevel);
	}

	public CoinDanmakuBulletEntity(EntityType<? extends ItemBulletEntity> pEntityType, LivingEntity pShooter, Level pLevel) {
		super(pEntityType, pShooter, pLevel);
	}

	/**
	 * Whether this throw actually spent a coin. A creative throw, or one a listener cleared
	 * {@link dev.xkmc.danmakuapi.api.DanmakuUseEvent#consume()} on, must not create one.
	 */
	public void setDroppable(boolean pDroppable) {
		droppable = pDroppable;
	}

	@Override
	protected void onHitBlock(BlockHitResult pResult) {
		dropCoin();
		super.onHitBlock(pResult);
	}

	@Override
	public void onHitEntity(EntityHitResult pResult) {
		dropCoin();
		super.onHitEntity(pResult);
	}

	@Override
	public void markErased(boolean kill) {
		dropCoin();
		super.markErased(kill);
	}

	private void dropCoin() {
		if (dropped || !droppable || level().isClientSide) return;
		dropped = true;
		ItemStack stack = getItem();
		if (stack.isEmpty()) return;
		ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), stack.copy());
		item.setDefaultPickUpDelay();
		level().addFreshEntity(item);
	}

}

package dev.xkmc.gensokyolegacy.content.entity.foundation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/**
 * Hook for the sleeping position of a {@link LivingEntity}.
 * <p>
 * Vanilla keeps {@code LivingEntity#setPosToBed} private and hardcodes the mattress
 * height into it, so it cannot be overridden; {@code LivingEntityMixin} injects into it
 * and delegates here instead.
 */
public interface ISleepOffsetEntity {

	/**
	 * Y offset from the sleeping {@link BlockPos} to the entity origin.
	 * Vanilla beds use 0.6875 (9/16 mattress top + 2/16 gap), ours are flat.
	 */
	double getSleepOffset(BlockPos pos);

	/**
	 * Move the entity onto the bed at {@link BlockPos}, using {@link #getSleepOffset(BlockPos)}.
	 */
	default void setPosToBed(BlockPos pos) {
		LivingEntity self = (LivingEntity) this;
		self.setPos(pos.getX() + 0.5, pos.getY() + getSleepOffset(pos), pos.getZ() + 0.5);
	}
}

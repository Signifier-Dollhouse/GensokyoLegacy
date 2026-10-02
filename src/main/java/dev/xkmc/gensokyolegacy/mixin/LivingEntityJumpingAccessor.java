package dev.xkmc.gensokyolegacy.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code jumping} is protected and has no public getter, but a riding vehicle has
 * to read the jump key to climb.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityJumpingAccessor {

	@Accessor("jumping")
	boolean gensokyolegacy$isJumping();

}
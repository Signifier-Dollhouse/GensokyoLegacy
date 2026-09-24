package dev.xkmc.gensokyolegacy.content.entity.youkai;

import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Shared GeckoLib one-shot for characters: the {@code 使用主手物品} clip,
 * fired by a server-to-client entity event, same pattern as the doll rig.
 * Dolls use 66-69; characters use 70.
 */
public interface UseMainhandAnim extends GeoEntity {

	RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");

	byte EVENT_USE_MAINHAND = 70;

	String ANIM_CONTROLLER = "main";
	String USE_TRIGGER = "use_mainhand";

	default void broadcastUseMainhandAnim() {
		LivingEntity self = (LivingEntity) this;
		if (!self.level().isClientSide())
			self.level().broadcastEntityEvent(self, EVENT_USE_MAINHAND);
	}

	/**
	 * Client-side: play the one-shot. Returns true when the id was consumed.
	 */
	default boolean handleUseMainhandEvent(byte id) {
		if (id != EVENT_USE_MAINHAND) return false;
		triggerAnim(ANIM_CONTROLLER, USE_TRIGGER);
		return true;
	}

}

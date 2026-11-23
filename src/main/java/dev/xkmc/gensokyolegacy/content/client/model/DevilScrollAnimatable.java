package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.Blaze3D;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * What {@link DevilScrollLayer} runs its geckolib animation against. Not an entity, so it has no tick
 * of its own: the layer hands an {@link AnimationState} built from the wearer each frame.
 */
public class DevilScrollAnimatable implements GeoAnimatable {

	private static final RawAnimation IDLE = RawAnimation.begin()
			.thenLoop("animation.devils_scroll_superhuman.idle");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "idle", 0, this::idle));
	}

	private PlayState idle(AnimationState<DevilScrollAnimatable> state) {
		return state.setAndContinue(IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	/**
	 * The geckolib clock, as there is no entity to tick this animatable.
	 */
	@Override
	public double getTick(Object object) {
		return Blaze3D.getTime() * 20;
	}

}

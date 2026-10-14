package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.Blaze3D;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/** What {@link IceWingsLayer} runs its geckolib animation against. Not an entity, so it has no tick of
 * its own: the layer hands an {@code AnimationState} built from the wearer each frame, keyed per wearer,
 * so one player in mid-air does not drag everyone else into the hover clip.
 */
public class IceWingsAnimatable implements GeoAnimatable {

	/** Cirno's standing flap: {@code 待机}, a 13 degree flutter at 0.4583s a cycle. */
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ice_wings.idle");
	/** Cirno's airborne flap: {@code 漂浮}, the faster one that also swings the wings in Y. */
	private static final RawAnimation HOVER = RawAnimation.begin().thenLoop("animation.ice_wings.hover");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this, false);

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// zero transition ticks: her clips are cut to the same pose at the seam, so there is
		// nothing to blend, and a blend would only smear the flap as the wearer leaves the ground
		controllers.add(new AnimationController<>(this, "flap", 0, this::flap));
	}

	/** Cirno swaps to {@code 漂浮} the moment she leaves the ground, so the wings do the same: the layer
	 * feeds {@link AnimationState#isMoving()} from that test, not from walking — still in mid-air flaps hardest.
	 */
	private PlayState flap(AnimationState<IceWingsAnimatable> state) {
		return state.setAndContinue(state.isMoving() ? HOVER : IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	/**
	 * What {@code GeoItem} answers for an item that has no tick either: geckolib's own clock,
	 * which is the same one {@code handleAnimations} falls back to for a plain animatable.
	 */
	@Override
	public double getTick(Object object) {
		return Blaze3D.getTime() * 20;
	}

}
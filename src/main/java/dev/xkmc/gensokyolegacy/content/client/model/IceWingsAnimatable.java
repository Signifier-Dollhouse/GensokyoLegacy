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

/**
 * What {@link IceWingsLayer} runs its geckolib animation against.
 *
 * <p>Not an entity, so it has no tick of its own: geckolib falls back to
 * {@code RenderUtil.getCurrentTick()} for a plain {@code GeoAnimatable}, and the layer passes an
 * {@code AnimationState} built from the wearer each frame. There is one of these per layer but
 * one controller state per wearer, keyed off the id the layer hands to
 * {@code handleAnimations} - so two players seen by the same renderer flap on their own clocks,
 * and one in mid-air does not drag everyone else into the hover clip with them.
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

	/**
	 * Cirno swaps to {@code 漂浮} the moment she is off the ground, so the wings do the same.
	 * The layer feeds {@link AnimationState#isMoving()} from exactly that test rather than from
	 * walking, because a player standing still in mid-air is the case that should flap hardest.
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
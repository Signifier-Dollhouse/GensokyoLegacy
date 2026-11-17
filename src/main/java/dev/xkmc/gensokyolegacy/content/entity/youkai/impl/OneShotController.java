package dev.xkmc.gensokyolegacy.content.entity.youkai.impl;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

/**
 * The overlay controller of a youkai rig: one triggered clip at a time, reporting to the loop controller
 * when that clip runs out.
 * <p>
 * A controller plays one animation at a time and a GeckoLib transition lerps towards frame 0 of the
 * incoming clip, so this controller has nothing to fade back to on its own. The loop, registered before
 * it, fades back in instead, from the clip's last pose.
 *
 * @see "doc/design/youkai/anim.md"
 */
public class OneShotController extends AnimationController<GeoEntity> {

	private boolean finished;

	public OneShotController(GeoEntity animatable, int transitionTicks, AnimationStateHandler<GeoEntity> handler) {
		super(animatable, GeoYoukaiAnim.ONE_SHOT_CONTROLLER, transitionTicks, handler);
	}

	@Override
	public OneShotController receiveTriggeredAnimations() {
		super.receiveTriggeredAnimations();
		return this;
	}

	/**
	 * True once, on the frame after a clip ran to its end; the loop controller consumes it.
	 */
	public boolean consumeFinished() {
		boolean flag = finished;
		finished = false;
		return flag;
	}

	/**
	 * End a clip that holds its last pose ({@code thenPlayAndHold}), so the loop can fade back in. Ignores
	 * a later clip that already took the controller, which is why it asks about the loop type.
	 */
	public boolean releaseHeld() {
		RawAnimation anim = this.triggeredAnimation;
		if (anim == null || anim.getAnimationStages().isEmpty()) return false;
		if (anim.getAnimationStages().getLast().loopType() != Animation.LoopType.HOLD_ON_LAST_FRAME)
			return false;
		stopTriggeredAnimation();
		this.finished = true;
		return true;
	}

	@Override
	public void process(GeoModel<GeoEntity> model, AnimationState<GeoEntity> state, Map<String, GeoBone> bones,
						Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
		super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
		// the end is only known at the end of the frame, and the loop has already run by then
		if (this.triggeredAnimation != null && hasAnimationFinished())
			this.finished = true;
	}

}

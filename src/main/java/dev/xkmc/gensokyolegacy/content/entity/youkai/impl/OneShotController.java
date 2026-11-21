package dev.xkmc.gensokyolegacy.content.entity.youkai.impl;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.state.BoneSnapshot;

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
	private RawAnimation held;

	public OneShotController(GeoEntity animatable, int transitionTicks, AnimationStateHandler<GeoEntity> handler) {
		super(animatable, GeoYoukaiAnim.ONE_SHOT_CONTROLLER, transitionTicks, handler);
	}

	@Override
	public OneShotController receiveTriggeredAnimations() {
		super.receiveTriggeredAnimations();
		return this;
	}

	/**
	 * True once, on the frame after a clip ran out; the loop controller consumes it and drops the hold the
	 * clip was kept on for that frame, unless something else has taken the controller since.
	 */
	public boolean consumeFinished() {
		if (!finished) return false;
		finished = false;
		// only the clip we held: a trigger that arrived after the latch owns the controller now
		if (held != null && held == triggeredAnimation) stopTriggered();
		held = null;
		return true;
	}

	/**
	 * End a clip that holds its last pose ({@code thenPlayAndHold}), so the loop can fade back in. Ignores
	 * a later clip that already took the controller, which is why it asks about the loop type.
	 */
	public boolean releaseHeld() {
		RawAnimation anim = this.triggeredAnimation;
		if (anim == null || anim.getAnimationStages().isEmpty()) return false;
		var stages = anim.getAnimationStages();
		if (stages.get(stages.size() - 1).loopType() != Animation.LoopType.HOLD_ON_LAST_FRAME)
			return false;
		stopTriggered();
		this.finished = true;
		return true;
	}

	/**
	 * GeckoLib 4.4.9 has no {@code stopTriggeredAnimation}, so this is its body: drop the clip and force the
	 * loop to rebuild its queue on the next frame.
	 */
	private void stopTriggered() {
		if (currentRawAnimation == triggeredAnimation) {
			currentAnimation = null;
			currentRawAnimation = null;
		}
		triggeredAnimation = null;
		needsAnimationReload = true;
	}

	@Override
	public void process(CoreGeoModel<GeoEntity> model, AnimationState<GeoEntity> state, Map<String, CoreGeoBone> bones,
						Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
		holdLastFrame(seekTime);
		super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
	}

	/**
	 * A clip whose time is up stops writing and returns before it emits its last frame, so the pose snaps to
	 * the loop on that very frame — before the loop ever sees a pose to fade from. Catch the end one tick
	 * early and hold that frame instead: the final pose is written, {@code finished} latches, and the next
	 * frame the loop fades from it.
	 * <p>
	 * The tick is the one {@code AnimationController#process} is about to compute; it is only read here, so
	 * the controller's own clock stays the single source of truth.
	 */
	private void holdLastFrame(double seekTime) {
		if (triggeredAnimation == null || currentAnimation == null) return;
		if (currentRawAnimation != triggeredAnimation) return;
		if (animationState != State.RUNNING || !animationQueue.isEmpty()) return;
		if (currentAnimation.loopType() == Animation.LoopType.HOLD_ON_LAST_FRAME) return;
		double tick = shouldResetTick ? 0 : getAnimationSpeed() * Math.max(seekTime - tickOffset, 0);
		if (tick < currentAnimation.animation().length()) return;
		currentAnimation = new AnimationProcessor.QueuedAnimation(currentAnimation.animation(),
				Animation.LoopType.HOLD_ON_LAST_FRAME);
		held = triggeredAnimation;
		finished = true;
	}

}

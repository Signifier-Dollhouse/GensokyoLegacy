package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

/**
 * Arms a single lance flash at the attack's extension pose, after the blend.
 */
public class DollOneShotController extends AnimationController<DollEntity> {

	private static final double SPEAR_IMPACT_TICK = 0.1852 * 20;

	private boolean spearParticleReady;
	private boolean spearParticleEmitted;

	public DollOneShotController(DollEntity doll) {
		super(doll, DollGeo.ONE_SHOT_CONTROLLER, DollGeo.ANIM_TRANSITION, doll::dollOneShotAnimController);
	}

	@Override
	public boolean tryTriggerAnimation(String animName) {
		if (!super.tryTriggerAnimation(animName)) return false;
		spearParticleReady = false;
		spearParticleEmitted = false;
		return true;
	}

	@Override
	public void process(GeoModel<DollEntity> model, AnimationState<DollEntity> state, Map<String, GeoBone> bones,
						Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
		super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
		if (getAnimationState() != State.RUNNING || !DollGeo.ATTACK.equals(getTriggeredAnimation())) {
			spearParticleReady = false;
			return;
		}
		if (!spearParticleEmitted && adjustTick(seekTime) >= SPEAR_IMPACT_TICK) {
			spearParticleReady = true;
			spearParticleEmitted = true;
		}
	}

	public boolean consumeSpearParticle() {
		if (!spearParticleReady) return false;
		spearParticleReady = false;
		return true;
	}

}

package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationController;

/**
 * Arms a single lance flash 100 ms after the attack triggers, consumed by the held-item render.
 * Keeps the lance axis fixed after the entry transition while the attack moves the hand.
 */
public class DollOneShotController extends AnimationController<DollEntity> {

	private static final long SPEAR_DELAY_MILLIS = 100;

	private boolean spearParticleReady;
	private long spearParticleTime;
	@Nullable
	private Vector3f spearDirection;

	public DollOneShotController(DollEntity doll) {
		super(doll, DollGeo.ONE_SHOT_CONTROLLER, DollGeo.ANIM_TRANSITION, doll::dollOneShotAnimController);
	}

	@Override
	public boolean tryTriggerAnimation(String animName) {
		if (!super.tryTriggerAnimation(animName)) return false;
		spearDirection = null;
		spearParticleReady = DollGeo.ATTACK.equals(getTriggeredAnimation());
		spearParticleTime = Util.getMillis() + SPEAR_DELAY_MILLIS;
		return true;
	}

	@Nullable
	public Matrix4f stabilizeSpearPose(Matrix4f pose, Vector3f direction) {
		if (getAnimationState() != State.RUNNING || !DollGeo.ATTACK.equals(getTriggeredAnimation())) {
			spearDirection = null;
			return null;
		}
		if (direction.lengthSquared() < 1e-6F) return null;
		if (spearDirection == null) {
			spearDirection = new Vector3f(direction).normalize();
			return null;
		}
		var rotation = new Quaternionf().rotationTo(direction, spearDirection);
		Matrix4f corrected = new Matrix4f(pose).rotateLocal(rotation)
				.setTranslation(pose.m30(), pose.m31(), pose.m32());
		return new Matrix4f(pose).invert().mul(corrected);
	}

	public boolean consumeSpearParticle() {
		if (!spearParticleReady || Util.getMillis() < spearParticleTime) return false;
		spearParticleReady = false;
		return true;
	}

}

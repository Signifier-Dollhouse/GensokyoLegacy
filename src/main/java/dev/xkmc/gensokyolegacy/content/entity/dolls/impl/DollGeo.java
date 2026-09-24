package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * GeckoLib animation for a doll: {@code toy_idle} while still,
 * {@code toy_fly} while moving, plus one-shot {@code toy_attack} /
 * {@code toy_bomb} fired by server-to-client entity events
 * ({@link #EVENT_ATTACK}/{@link #EVENT_BOMB}). The instance cache lives in
 * {@link DollGeoModule}, fetched per entity — the entity keeps no geo state itself.
 */
public interface DollGeo extends DollBaseImpl, GeoEntity {

	RawAnimation IDLE = RawAnimation.begin().thenLoop("toy_idle");
	RawAnimation FLY = RawAnimation.begin().thenLoop("toy_fly");
	RawAnimation ATTACK = RawAnimation.begin().thenPlay("toy_attack");
	RawAnimation BOMB = RawAnimation.begin().thenPlay("toy_bomb");
	RawAnimation BOW = RawAnimation.begin().thenPlay("toy_bow");
	RawAnimation SKILL = RawAnimation.begin().thenPlay("toy_skill");
	RawAnimation WINK = RawAnimation.begin().thenLoop("toy_wink");

	String WINK_CONTROLLER = "wink";

	/**
	 * Entity event ids: vanilla {@code EntityEvent} uses up to 65, so 66+
	 * are safe. Broadcast server-side, played client-side in
	 * {@link DollEntity#handleEntityEvent}.
	 */
	byte EVENT_ATTACK = 66;
	byte EVENT_BOMB = 67;
	byte EVENT_BOW = 68;
	byte EVENT_SKILL = 69;

	private DollGeoModule geo() {
		return asDoll().getModule(DollGeoModule.class);
	}

	default PlayState dollAnimController(final AnimationState<DollEntity> event) {
		if (event.getController().isPlayingTriggeredAnimation())
			return PlayState.CONTINUE;
		return event.setAndContinue(event.isMoving() ? FLY : IDLE);
	}

	@Override
	default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Wink first: controllers tick in registration order and overwrite shared
		// bones, so the always-looping wink yields to the main controller.
		controllers.add(new AnimationController<>(asDoll(), WINK_CONTROLLER, 0, e -> e.setAndContinue(WINK)));
		controllers.add(new AnimationController<>(asDoll(), "all", 3, this::dollAnimController)
				.triggerableAnim("attack", ATTACK)
				.triggerableAnim("bomb", BOMB)
				.triggerableAnim("bow", BOW)
				.triggerableAnim("skill", SKILL));
	}

	/**
	 * Client-side: play the one-shot for an entity event id.
	 */
	default void handleDollEvent(byte id) {
		if (id == EVENT_ATTACK) triggerAnim("all", "attack");
		else if (id == EVENT_BOMB) triggerAnim("all", "bomb");
		else if (id == EVENT_BOW) triggerAnim("all", "bow");
		else if (id == EVENT_SKILL) triggerAnim("all", "skill");
	}

	default void broadcastAttackAnim() {
		if (!asDoll().level().isClientSide())
			asDoll().level().broadcastEntityEvent(asDoll(), EVENT_ATTACK);
	}

	default void broadcastBowAnim() {
		if (!asDoll().level().isClientSide())
			asDoll().level().broadcastEntityEvent(asDoll(), EVENT_BOW);
	}

	default void broadcastSkillAnim() {
		if (!asDoll().level().isClientSide())
			asDoll().level().broadcastEntityEvent(asDoll(), EVENT_SKILL);
	}

	default void broadcastBombAnim() {
		if (!asDoll().level().isClientSide())
			asDoll().level().broadcastEntityEvent(asDoll(), EVENT_BOMB);
	}

	@Override
	default AnimatableInstanceCache getAnimatableInstanceCache() {
		return geo().cache;
	}

	/**
	 * The lazily built instance cache. The cache needs the entity, which
	 * modules never store (they run before it exists) — it is built on first render.
	 * Synchronized: dolls construct on the server thread and render on the client thread.
	 */
	final class DollGeoModule implements DollModule {

		private AnimatableInstanceCache cache;

		public DollGeoModule(DollEntity entity) {
			cache = GeckoLibUtil.createInstanceCache(entity);
		}

	}

}

package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLSounds;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * GeckoLib animation for a doll, split over three controllers that run in the order they write
 * bones — wink, state loop, one-shots — so a strike shares the hover's bones instead of replacing
 * it, since a controller plays one animation at a time. The cache lives in {@link DollGeoModule} (entity.md §8).
 */
public interface DollGeo extends DollBaseImpl, GeoEntity {

	RawAnimation IDLE = RawAnimation.begin().thenLoop("toy_idle");
	RawAnimation FLY = RawAnimation.begin().thenLoop("toy_fly");
	RawAnimation ATTACK = RawAnimation.begin().thenPlay("toy_attack");
	/** The same charge, swung with an empty hand: {@code toy_slapattack}. */
	RawAnimation SLAP = RawAnimation.begin().thenPlay("toy_slapattack");
	RawAnimation BOMB = RawAnimation.begin().thenPlay("toy_bomb");
	RawAnimation BOW = RawAnimation.begin().thenPlay("toy_bow");
	RawAnimation SKILL = RawAnimation.begin().thenPlay("toy_skill");
	RawAnimation WINK = RawAnimation.begin().thenLoop("toy_wink");

	String ANIM_CONTROLLER = "all";
	String ONE_SHOT_CONTROLLER = "one_shot";
	String WINK_CONTROLLER = "wink";

	/** Blend length of the hover and of the one-shots alike, in ticks. */
	int ANIM_TRANSITION = 3;

	/**
	 * Entity event ids: vanilla {@code EntityEvent} uses up to 65, so 66+ are safe. Dolls
	 * take 66-70; characters start after them.
	 */
	byte EVENT_ATTACK = 66;
	byte EVENT_BOMB = 67;
	byte EVENT_BOW = 68;
	byte EVENT_SKILL = 69;
	byte EVENT_SLAP = 70;

	/** Volume of a strike voice, both melee shapes alike; the clips are short. */
	float ATTACK_VOLUME = 1.0F;

	private DollGeoModule geo() {
		return asDoll().getModule(DollGeoModule.class);
	}

	default PlayState dollAnimController(final AnimationState<DollEntity> event) {
		return event.setAndContinue(event.isMoving() ? FLY : IDLE);
	}

	/**
	 * The one-shot half: let the strike that fired run to its end, and otherwise write
	 * nothing at all, leaving the pose to {@link #dollAnimController}.
	 */
	default PlayState dollOneShotAnimController(AnimationState<DollEntity> event) {
		return event.getController().isPlayingTriggeredAnimation() ? PlayState.CONTINUE : PlayState.STOP;
	}

	@Override
	default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// wink first, one-shots last: controllers tick in registration order and
		// overwrite shared bones
		controllers.add(new AnimationController<>(asDoll(), WINK_CONTROLLER, 0, e -> e.setAndContinue(WINK)));
		controllers.add(new AnimationController<>(asDoll(), ANIM_CONTROLLER, ANIM_TRANSITION, this::dollAnimController));
		controllers.add(new DollOneShotController(asDoll())
				.receiveTriggeredAnimations()
				.triggerableAnim("attack", ATTACK)
				.triggerableAnim("slap", SLAP)
				.triggerableAnim("bomb", BOMB)
				.triggerableAnim("bow", BOW)
				.triggerableAnim("skill", SKILL));
	}

	/** Client-side: play the one-shot for an entity event id. */
	default void handleDollEvent(byte id) {
		if (id == EVENT_ATTACK) triggerAnim(ONE_SHOT_CONTROLLER, "attack");
		else if (id == EVENT_SLAP) triggerAnim(ONE_SHOT_CONTROLLER, "slap");
		else if (id == EVENT_BOMB) triggerAnim(ONE_SHOT_CONTROLLER, "bomb");
		else if (id == EVENT_BOW) triggerAnim(ONE_SHOT_CONTROLLER, "bow");
		else if (id == EVENT_SKILL) triggerAnim(ONE_SHOT_CONTROLLER, "skill");
	}

	/**
	 * The lance swing, animation and voice together: {@code toy_attack} is silent, so
	 * {@link GLSounds#DOLL_LANCE} makes the strike audible. The pitch is the entity's own vanilla
	 * voice jitter, which keeps a same-tick volley from stacking into one louder, phasey hit.
	 */
	default void broadcastAttackAnim() {
		broadcastStrike(EVENT_ATTACK, GLSounds.DOLL_LANCE);
	}

	/**
	 * The bare-handed swing: {@link #EVENT_SLAP} rather than the lance clip, voiced with
	 * {@link GLSounds#DOLL_SLAP} so a slap and a poke are told apart by ear alone.
	 */
	default void broadcastSlapAnim() {
		broadcastStrike(EVENT_SLAP, GLSounds.DOLL_SLAP);
	}

	/**
	 * One broadcast and one voice, together and server-side only. Shared by both melee
	 * shapes so the animation and its sound cannot come apart.
	 */
	private void broadcastStrike(byte event, SimpleEntry<SoundEvent> sound) {
		if (asDoll().level().isClientSide()) return;
		asDoll().level().broadcastEntityEvent(asDoll(), event);
		asDoll().level().playSound(null, asDoll(), sound.get(),
				SoundSource.NEUTRAL, ATTACK_VOLUME, asDoll().getVoicePitch());
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
	 * The instance cache. It needs the entity, which modules never store (they run before it
	 * exists). Synchronized: dolls construct on the server thread and render on the client thread.
	 */
	final class DollGeoModule implements DollModule {

		private AnimatableInstanceCache cache;

		public DollGeoModule(DollEntity entity) {
			cache = GeckoLibUtil.createInstanceCache(entity);
		}

	}

}

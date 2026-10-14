package dev.xkmc.gensokyolegacy.content.entity.youkai;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.Optional;

/**
 * Shared GeckoLib one-shot animations for characters, fired by server-to-client
 * entity events ({@code enum ordinal + offset}), same pattern as the doll rig.
 * <p>
 * Each character maps every {@link YoukaiAnim} slot to its own clip, possibly
 * empty when it has no such animation. Dialog animations are data-driven: each
 * {@code Dialog} carries a list of possible trigger names, one of which is
 * played once when the dialog opens (see {@code SimpleDialogSession}). The
 * greeting is the exception: it is code-triggered when a conversation starts,
 * as it is not part of any dialog.
 * <p>
 * Plus the {@code 眨眼} blink loop, which runs on its own always-on controller.
 * <p>
 * A character therefore runs three controllers, and the order they are
 * registered in is the order they write bones in:
 * <ol>
 * <li>{@link #WINK_CONTROLLER} — the blink, always looping, first so everything
 * else overwrites the bones it shares (the face).</li>
 * <li>{@link #ANIM_CONTROLLER} — the state loop, the character's own
 * {@link #stateAnimController}: idle, walk, sit, sleep, whatever she is doing.</li>
 * <li>{@link #ONE_SHOT_CONTROLLER} — every triggerable, from {@link #oneShotController}:
 * the dialog slots, the chew, the flinch, the gather. Last so it wins the bones
 * it shares with the loop.</li>
 * </ol>
 * State and one-shots are split rather than sharing one controller because a
 * GeckoLib controller plays one animation at a time: sharing one, the state
 * handler is not even consulted while a one-shot is playing, so every bone the
 * one-shot does not touch holds still mid-hover until it ends. Split, the loop
 * keeps running underneath and the one-shot simply overrides the bones it moves.
 * <p>
 * All three are wired up by {@link #registerControllers}, which is the same for
 * every character: what varies is {@link #getAnim}, the {@code has*Anim} hooks
 * and {@link #stateAnimController}.
 */
public interface GeoYoukaiAnim extends GeoEntity {

	RawAnimation BLINK = RawAnimation.begin().thenLoop("眨眼");

	byte ANIM_EVENT_BASE = 71;

	String ANIM_CONTROLLER = "main";
	String ONE_SHOT_CONTROLLER = "one_shot";
	String WINK_CONTROLLER = "wink";

	/**
	 * Blend length of the state loop and of the one-shots, in ticks. Both want
	 * the same one: a one-shot blending faster than the loop hands off visibly.
	 */
	int ANIM_TRANSITION = 5;

	/**
	 * The chew. Kept out of {@link YoukaiAnim} for the reason the clip is: it is an
	 * appetite, not dialogue, and putting it in an enum addressed by trigger name from
	 * dialog data would cost every character a dead case in an already exhaustive switch.
	 * {@code GeoYoukaiAnim} owns 71-83, so the next id up is free.
	 *
	 * <p>
	 * {@code thenPlayAndHold} rather than {@code thenPlay}, because the chew has to sit
	 * there until she reaches for the next dish, and holding the last pose means a
	 * slightly-off timing reads as her still chewing instead of snapping back to hovering
	 * mid-bite.
	 */
	RawAnimation EAT = RawAnimation.begin().thenPlayAndHold("吃");

	String EAT_TRIGGER = "eat";
	byte EAT_EVENT = 84;

	/**
	 * The flinch. Also not a {@link YoukaiAnim} slot, for the same reason as the chew:
	 * nothing in dialog data names it, it is fired by the damage path instead
	 * ({@code YoukaiEntity#postHurt}).
	 *
	 * <p>
	 * {@code thenPlay} rather than {@code thenPlayAndHold}: a hurt reaction that holds
	 * its last pose would strand her in the flinch, since the one-shot controller has
	 * nothing to hand the pose back to — the state loop is a separate controller and
	 * would just be overridden again on the next tick.
	 */
	RawAnimation HURT = RawAnimation.begin().thenPlay("受伤");

	String HURT_TRIGGER = "hurt";
	byte HURT_EVENT = 85;

	byte GATHER_EVENT = 86;

	/**
	 * The clip this character plays for the given slot, or empty when it has none.
	 */
	Optional<RawAnimation> getAnim(YoukaiAnim anim);

	/**
	 * The state loop: what she is doing when nothing is triggered on top of her. Every
	 * rig writes its own, off its own clips and its own flags — napping, sitting,
	 * flying — so this is the one piece of the rig the interface cannot assemble.
	 * <p>
	 * It runs unconditionally, every frame, including while a one-shot is playing on
	 * the other controller: that is what keeps her hovering and walking under a
	 * dialog clip rather than freezing in the pose the loop happened to be in.
	 */
	PlayState stateAnimController(AnimationState<?> event);

	/**
	 * Whether this rig ships the {@code 眨眼} blink. Opt-out per rig, for the same
	 * reason as the three below: a clip the model cannot resolve is not a missing
	 * blink but an error logged once per frame.
	 */
	default boolean hasBlinkAnim() {
		return true;
	}

	/**
	 * Whether this rig ships the {@code 吃} clip. Every rig in the mod does, so the
	 * default is yes; override to opt out.
	 */
	default boolean hasEatAnim() {
		return true;
	}

	/**
	 * Whether this rig ships the {@code 受伤} clip. Same default as the chew: only
	 * {@code PlainFairyEntity} overrides it.
	 */
	default boolean hasHurtAnim() {
		return true;
	}

	/**
	 * Assemble the rig: {@link #registerControllers} is the same three controllers for
	 * every character, and everything that varies is the {@code has*Anim} hooks above
	 * and {@link #getAnim}.
	 */
	@Override
	default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Registration order is bone-writing order, so the blink goes down first —
		// everything after it overwrites the face — and the one-shots go down last,
		// so a triggered clip covers the bones it shares with the state loop.
		if (hasBlinkAnim())
			controllers.add(new AnimationController<>(this, WINK_CONTROLLER, 0, e -> e.setAndContinue(BLINK)));
		controllers.add(new AnimationController<>(this, ANIM_CONTROLLER, ANIM_TRANSITION, this::stateAnimController));
		var oneShot = oneShotController();
		addTriggerableAnims(oneShot);
		controllers.add(oneShot);
	}

	/**
	 * Build the one-shot controller, wired to {@link #oneShotAnimController} and
	 * empty until {@link #registerControllers} fills it.
	 *
	 * <p>
	 * {@code receiveTriggeredAnimations} so the handler is consulted every frame
	 * rather than only on the frames nothing is playing: a rig that re-points its
	 * triggers from state ({@code SuikaEntity}, see {@code syncDialogAnims}) has to
	 * be able to do it in the middle of a one-shot.
	 */
	default AnimationController<GeoEntity> oneShotController() {
		AnimationController<GeoEntity> controller =
				new AnimationController<>(this, ONE_SHOT_CONTROLLER, ANIM_TRANSITION, this::oneShotAnimController);
		return controller.receiveTriggeredAnimations();
	}

	/**
	 * The one-shot half of the rig: let the trigger that fired run to its end, and
	 * otherwise write nothing at all, leaving the pose to the state controller.
	 */
	default PlayState oneShotAnimController(AnimationState<?> event) {
		return event.getController().isPlayingTriggeredAnimation() ? PlayState.CONTINUE : PlayState.STOP;
	}

	/**
	 * The chew on the one-shot controller. Gated by {@link #hasEatAnim}: a rig that
	 * does not ship {@code 吃} must not name it, or the trigger logs "Unable to find
	 * animation" and plays nothing.
	 */
	private void addEatAnim(AnimationController<?> controller) {
		controller.triggerableAnim(EAT_TRIGGER, EAT);
	}

	/**
	 * The flinch on the one-shot controller, gated by {@link #hasHurtAnim} for the
	 * same reason as the chew.
	 */
	private void addHurtAnim(AnimationController<?> controller) {
		controller.triggerableAnim(HURT_TRIGGER, HURT);
	}

	/**
	 * A triggerable for every dialog slot {@link #getAnim} actually maps, on the
	 * one-shot controller. No gate needed: an unmapped slot is one the rig has no
	 * clip for and nobody asks to play.
	 */
	default void addTriggerableAnims(AnimationController<?> controller) {
		if (hasEatAnim()) addEatAnim(controller);
		if (hasHurtAnim()) addHurtAnim(controller);
		for (var anim : YoukaiAnim.values()) {
			getAnim(anim).ifPresent(raw -> controller.triggerableAnim(anim.trigger(), raw));
		}
	}

	default void broadcastAnim(YoukaiAnim anim) {
		LivingEntity self = (LivingEntity) this;
		if (!self.level().isClientSide())
			self.level().broadcastEntityEvent(self, (byte) (ANIM_EVENT_BASE + anim.ordinal()));
	}

	/**
	 * Play one of the given dialog triggers once, chosen at random. Entries
	 * map to {@link YoukaiAnim} by trigger name (falling back to enum name);
	 * unknown entries and unmapped slots are ignored. An empty list falls
	 * back to a random talk clip.
	 */
	default void broadcastDialogAnim(List<String> animations, RandomSource random) {
		if (animations.isEmpty()) {
			var fallback = YoukaiAnim.TALKS.stream().filter(a -> getAnim(a).isPresent()).toList();
			if (fallback.isEmpty()) return;
			broadcastAnim(fallback.get(random.nextInt(fallback.size())));
			return;
		}
		var trigger = animations.get(random.nextInt(animations.size()));
		var anim = YoukaiAnim.byTrigger(trigger).or(() -> YoukaiAnim.byName(trigger));
		anim.ifPresent(e -> {
			if (getAnim(e).isPresent()) broadcastAnim(e);
		});
	}

	/**
	 * Client-side: play the one-shot. Returns true when the id was consumed.
	 */
	default boolean handleAnimEvent(byte id) {
		// the chew, the flinch and the gather, before the ordinal maths: EAT_EVENT,
		// HURT_EVENT and GATHER_EVENT sit just past the dialog range, so they cannot
		// collide
		if (id == EAT_EVENT) {
			triggerAnim(ONE_SHOT_CONTROLLER, EAT_TRIGGER);
			return true;
		}
		if (id == HURT_EVENT) {
			triggerAnim(ONE_SHOT_CONTROLLER, HURT_TRIGGER);
			return true;
		}
		int ord = id - ANIM_EVENT_BASE;
		var values = YoukaiAnim.values();
		if (ord < 0 || ord >= values.length) return false;
		var anim = values[ord];
		if (getAnim(anim).isEmpty()) return false;
		triggerAnim(anim);
		return true;
	}

	default void triggerAnim(YoukaiAnim anim) {
		triggerAnim(ONE_SHOT_CONTROLLER, anim.trigger());
	}

}

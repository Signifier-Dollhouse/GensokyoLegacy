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
 * Shared GeckoLib one-shot animations for characters, fired by server-to-client entity events
 * ({@code enum ordinal + offset}), same pattern as the doll rig. Each {@link YoukaiAnim} slot maps to the
 * character's own clip, possibly empty; dialog animations are data-driven (one of the dialog's triggers plays
 * when it opens) while the greeting, chew, flinch, skill and special talk are code-triggered.
 * <p>
 * Three controllers, registered in bone-writing order: {@link #WINK_CONTROLLER} the {@code 眨眼} blink first,
 * so everything else overwrites the face; {@link #ANIM_CONTROLLER}, the {@link #stateAnimController} loop;
 * {@link #ONE_SHOT_CONTROLLER} last, so a triggered clip wins the bones it shares. Split rather than shared
 * because a GeckoLib controller plays one animation at a time: shared, every bone a one-shot skips holds
 * still mid-hover until it ends. {@link #registerControllers} is identical for every character; only
 * {@link #getAnim} and {@link #stateAnimController} vary.
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
	 * The chew, shared because every rig that has one spells it {@code 吃} — a rig without one leaves
	 * {@link YoukaiAnim#EAT} unmapped and its trigger unregistered, which is the gate the clip needs:
	 * naming a clip the model cannot resolve logs "Unable to find animation" and plays nothing, every frame.
	 * <p>
	 * {@code thenPlayAndHold} rather than {@code thenPlay}: the chew must sit there until she reaches for the
	 * next dish, and holding the last pose makes an off timing read as her still chewing.
	 */
	RawAnimation EAT = RawAnimation.begin().thenPlayAndHold("吃");

	/**
	 * The flinch, shared for the same reason as the chew: {@link YoukaiAnim#HURT} maps straight to it.
	 * <p>
	 * {@code thenPlay} rather than {@code thenPlayAndHold}: a held last pose would strand her in the flinch,
	 * the one-shot controller having nothing to hand the pose back to — the state loop is separate and would
	 * just override it again next tick.
	 */
	RawAnimation HURT = RawAnimation.begin().thenPlay("受伤");

	/**
	 * The clip this character plays for the given slot, or empty when it has none.
	 * <p>
	 * Empty by default and every rig's switch ends in a {@code default} of its own, so a slot added later is
	 * unmapped everywhere until somebody gives it a clip, rather than a compile error in eight rigs. The cost
	 * is that a typo'd case label is also just unmapped - the same silence {@link Optional#empty()} means here.
	 */
	default Optional<RawAnimation> getAnim(YoukaiAnim anim) {
		return Optional.empty();
	}

	/**
	 * The state loop: what she is doing when nothing is triggered on top of her. Every rig writes its own,
	 * off its own clips and flags, so this is the one piece of the rig the interface cannot assemble.
	 * <p>
	 * It runs unconditionally every frame, including under a one-shot on the other controller - that is what
	 * keeps her hovering and walking under a dialog clip instead of freezing in the loop's last pose.
	 */
	PlayState stateAnimController(AnimationState<?> event);

	/**
	 * Whether this rig ships the {@code 眨眼} blink - the one capability {@link #getAnim} cannot express: it
	 * loops forever rather than being triggered, so it has no trigger to hang off and gets its own controller.
	 */
	default boolean hasBlinkAnim() {
		return true;
	}

	/**
	 * Assemble the rig: the same three controllers for every character; what varies is {@link #getAnim},
	 * {@link #hasBlinkAnim} and {@link #stateAnimController}.
	 */
	@Override
	default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// Registration order is bone-writing order: the blink goes down first so everything after it
		// overwrites the face, and the one-shots go down last so they cover the bones they share.
		if (hasBlinkAnim())
			controllers.add(new AnimationController<>(this, WINK_CONTROLLER, 0, e -> e.setAndContinue(BLINK)));
		controllers.add(new AnimationController<>(this, ANIM_CONTROLLER, ANIM_TRANSITION, this::stateAnimController));
		var oneShot = oneShotController();
		addTriggerableAnims(oneShot);
		controllers.add(oneShot);
	}

	/**
	 * Build the one-shot controller, wired to {@link #oneShotAnimController} and empty until
	 * {@link #registerControllers} fills it.
	 * <p>
	 * {@code receiveTriggeredAnimations} so the handler is consulted every frame, not only on the frames
	 * nothing is playing: a rig that re-points its triggers from state ({@code SuikaEntity#triggerAnim},
	 * which swaps every dialog slot for her lying-down clip while she naps) must do it mid-one-shot.
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
	 * A triggerable for every slot {@link #getAnim} actually maps. No gate needed: an unmapped slot is one
	 * the rig has no clip for and nobody asks to play.
	 */
	default void addTriggerableAnims(AnimationController<?> controller) {
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
	 * Play one of the given dialog triggers once, chosen at random. Entries map to {@link YoukaiAnim} by
	 * trigger name (falling back to enum name); unknown entries and unmapped slots are ignored, and an
	 * empty list falls back to a random talk clip.
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

package dev.xkmc.gensokyolegacy.content.entity.youkai;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationController;
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
 * Plus the {@code 眨眼} blink loop, which runs on its own always-on controller
 * (registered before the main one so other animations win shared bones).
 */
public interface GeoYoukaiAnim extends GeoEntity {

	RawAnimation BLINK = RawAnimation.begin().thenLoop("眨眼");

	byte ANIM_EVENT_BASE = 71;

	String ANIM_CONTROLLER = "main";
	String WINK_CONTROLLER = "wink";

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
	 * its last pose would strand her in the flinch until something else took the
	 * controller, and the main controller only yields while a triggered animation is
	 * still playing.
	 */
	RawAnimation HURT = RawAnimation.begin().thenPlay("受伤");

	String HURT_TRIGGER = "hurt";
	byte HURT_EVENT = 85;

	/**
	 * The clip this character plays for the given slot, or empty when it has none.
	 */
	Optional<RawAnimation> getAnim(YoukaiAnim anim);

	/**
	 * Register the chew on the main controller.
	 *
	 * <p>
	 * Called next to {@link #addDialogAnims} by every character whose rig has a
	 * {@code 吃} clip. It is <b>not</b> folded into {@code addDialogAnims}: registering a
	 * clip the rig does not have resolves it to {@code null}, {@code buildAnimationQueue}
	 * then hands the controller an empty queue, and the next {@code process()} tick stops
	 * it without writing any bones — pinning the character in the last pose written, i.e.
	 * frozen mid-hover for the whole walk, silently. So this is opt-in per rig.
	 */
	default void addEatAnim(AnimationController<?> controller) {
		controller.triggerableAnim(EAT_TRIGGER, EAT);
	}

	/**
	 * Register the flinch on the main controller. Opt-in per rig for the same reason as
	 * {@link #addEatAnim}: a character whose rig has no {@code 受伤} clip must not
	 * register this.
	 */
	default void addHurtAnim(AnimationController<?> controller) {
		controller.triggerableAnim(HURT_TRIGGER, HURT);
	}

	/**
	 * Register a triggerable for every mapped slot on the main controller.
	 */
	default void addDialogAnims(AnimationController<?> controller) {
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
		// the chew and the flinch, before the ordinal maths: EAT_EVENT and HURT_EVENT
		// sit just past the dialog range, so the two cannot collide
		if (id == EAT_EVENT) {
			triggerAnim(ANIM_CONTROLLER, EAT_TRIGGER);
			return true;
		}
		if (id == HURT_EVENT) {
			triggerAnim(ANIM_CONTROLLER, HURT_TRIGGER);
			return true;
		}
		int ord = id - ANIM_EVENT_BASE;
		var values = YoukaiAnim.values();
		if (ord < 0 || ord >= values.length) return false;
		var anim = values[ord];
		if (getAnim(anim).isEmpty()) return false;
		triggerAnim(ANIM_CONTROLLER, anim.trigger());
		return true;
	}

}

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
 * played once when the dialog opens (see {@code SimpleDialogProvider}). The
 * greeting is the exception: it is code-triggered when a conversation starts,
 * as it is not part of any dialog.
 * <p>
 * Plus the {@code 眨眼} blink loop, which runs on its own always-on controller
 * (registered before the main one so other animations win shared bones).
 */
public interface GeoYoukaiAnim extends GeoEntity {

	RawAnimation BLINK = RawAnimation.begin().thenLoop("眨眼");

	byte ANIM_EVENT_BASE = 70;

	String ANIM_CONTROLLER = "main";
	String WINK_CONTROLLER = "wink";

	/**
	 * The clip this character plays for the given slot, or empty when it has none.
	 */
	Optional<RawAnimation> getAnim(YoukaiAnim anim);

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
	 * unknown entries and unmapped slots are ignored.
	 */
	default void broadcastDialogAnim(List<String> animations, RandomSource random) {
		if (animations.isEmpty()) return;
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
		triggerAnim(ANIM_CONTROLLER, anim.trigger());
		return true;
	}

}

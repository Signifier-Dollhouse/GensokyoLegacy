package dev.xkmc.gensokyolegacy.content.entity.youkai;

import java.util.Locale;
import java.util.Optional;

/**
 * Standard one-shot animation slots for characters. Each slot has a trigger
 * name used in dialog data and as the GeckoLib triggerable key. Characters
 * map each slot to their own clip (possibly empty) via
 * {@link GeoYoukaiAnim#getAnim(YoukaiAnim)}.
 * <p>
 * Network sync uses {@code ordinal + offset} as the entity event id, so the
 * declaration order must stay stable. Dolls use 66-69; characters use 70-78.
 */
public enum YoukaiAnim {

	USE_MAINHAND("use_mainhand"),
	GREET("dialog_greet"),
	TALK_01("dialog_talk_01"),
	TALK_02("dialog_talk_02"),
	THINK("dialog_think"),
	AGREE("dialog_agree"),
	DECLINE("dialog_decline"),
	OUTDOOR_IDLE("idle_outdoor"),
	TALK_03("dialog_talk_03");

	private final String trigger;

	YoukaiAnim(String trigger) {
		this.trigger = trigger;
	}

	public String trigger() {
		return trigger;
	}

	/**
	 * Play this one-shot on the entity if it supports it. Unmapped slots
	 * are silently ignored.
	 */
	public void play(YoukaiEntity e) {
		if (e instanceof GeoYoukaiAnim anim && anim.getAnim(this).isPresent())
			anim.broadcastAnim(this);
	}

	public static Optional<YoukaiAnim> byTrigger(String trigger) {
		for (var e : values()) {
			if (e.trigger.equals(trigger)) return Optional.of(e);
		}
		return Optional.empty();
	}

	public static Optional<YoukaiAnim> byName(String name) {
		try {
			return Optional.of(valueOf(name.toUpperCase(Locale.ROOT)));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}

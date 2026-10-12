package dev.xkmc.gensokyolegacy.content.entity.youkai;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Standard one-shot animation slots for characters. Each slot has a trigger
 * name used in dialog data and as the GeckoLib triggerable key. Characters
 * map each slot to their own clip (possibly empty) via
 * {@link GeoYoukaiAnim#getAnim(YoukaiAnim)}.
 * <p>
 * Network sync uses {@code ordinal + offset} as the entity event id, so the
 * declaration order must stay stable — new slots go at the end, never in the
 * middle. Dolls use 66-70; characters use 71-83, then the chew and the hurt
 * reaction take 84-85 (see {@link GeoYoukaiAnim#EAT_EVENT}).
 * <p>
 * The thinking, talk and puzzled slots come in pairs because the current rigs
 * ship two variants of each ({@code 思考中_01}/{@code 思考中_02},
 * {@code 交流_01}..{@code 交流_05}, {@code 疑惑}); only one is ever played per
 * dialog, and which one is left to {@code GeoYoukaiAnim#broadcastDialogAnim}.
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
	TALK_03("dialog_talk_03"),
	TALK_04("dialog_talk_04"),
	TALK_05("dialog_talk_05"),
	THINK_02("dialog_think_02"),
	CONFUSED("dialog_confused");

	private final String trigger;

	YoukaiAnim(String trigger) {
		this.trigger = trigger;
	}

	public String trigger() {
		return trigger;
	}

	/**
	 * Every {@code 交流_*} slot, in order. Used by
	 * {@link GeoYoukaiAnim#broadcastDialogAnim} to pick a talk clip when the
	 * dialog names none, so a character shipping five of them gets all five
	 * in rotation rather than the first three.
	 */
	public static final List<YoukaiAnim> TALKS = List.of(TALK_01, TALK_02, TALK_03, TALK_04, TALK_05);

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

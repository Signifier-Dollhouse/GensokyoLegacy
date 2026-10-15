package dev.xkmc.gensokyolegacy.content.entity.youkai.facet;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Every triggerable one-shot a character can play, in one vocabulary. Each slot has a trigger name used
 * in dialog data and as the GeckoLib triggerable key; characters map each slot to their own clip
 * (possibly empty) via {@link GeoYoukaiAnim#getAnim(YoukaiAnim)}.
 * <p>
 * Network sync uses {@code ordinal + offset} as the entity event id.
 * <p>
 * {@link #EAT} and {@link #HURT} name clips the whole mod agrees on, so they can be named outright.
 * {@link #TALK_SPECIAL_01} and {@link #SKILL_01} are numbered instead, because what they hold is up to the
 * rig: Suika's are her lying-down talk ({@code 侧卧_交流}) and the sweep her spell card pulls with
 * ({@code 聚集}), and a slot shared by every character cannot be named after one of them. The next rig to
 * want a second special talk or a second skill takes {@code 02}.
 */
public enum YoukaiAnim {

	// common animations
	USE_MAINHAND("use_mainhand"),
	EAT("eat"),
	HURT("hurt"),
	OUTDOOR_IDLE("idle_outdoor"),
	SKILL_01("skill_01"),
	SKILL_02("skill_02"),
	SKILL_03("skill_03"),
	SKILL_04("skill_04"),
	SKILL_05("skill_05"),

	// dialog animations
	GREET("dialog_greet"),
	TALK_01("dialog_talk_01"),
	TALK_02("dialog_talk_02"),
	TALK_03("dialog_talk_03"),
	TALK_04("dialog_talk_04"),
	TALK_05("dialog_talk_05"),
	THINK("dialog_think"),
	THINK_02("dialog_think_02"),
	AGREE("dialog_agree"),
	DECLINE("dialog_decline"),
	CONFUSED("dialog_confused"),
	TALK_SPECIAL_01("dialog_talk_special_01"),
	TALK_SPECIAL_02("dialog_talk_special_02");

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
	 * Whether this slot is part of a conversation, as opposed to a thing the game asks for: the hand use
	 * comes from a behaviour task, the outdoor idle from the wander task, the skills from spell cards, the
	 * chew and the flinch from the eating and damage paths. Everything else is something she says.
	 * <p>
	 * A character whose dialog body animation changes with her state uses this to decide which triggers to
	 * re-point - see {@code SuikaEntity}, which talks in a different clip while she is lying down.
	 */
	public boolean isDialog() {
		return ordinal() >= GREET.ordinal();
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

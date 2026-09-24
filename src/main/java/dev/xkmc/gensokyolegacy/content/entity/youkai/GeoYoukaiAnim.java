package dev.xkmc.gensokyolegacy.content.entity.youkai;

import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Shared GeckoLib one-shot animations for characters, all fired by
 * server-to-client entity events, same pattern as the doll rig.
 * <ul>
 * <li>{@code 使用主手物品}: work swing.</li>
 * <li>{@code 招呼}: plays once when the player starts a conversation (dialog open).</li>
 * <li>{@code 交流_01} / {@code 交流_02}: one of the two plays once, chosen at
 * random, when the player enters a regular chat dialog.</li>
 * <li>{@code 思考中}: plays once when the player enters a quest start or follow-up dialog.</li>
 * <li>{@code 肯定}: plays once when the player enters a quest completion dialog.</li>
 * <li>{@code 拒绝}: reserved for quest rejection.</li>
 * </ul>
 * All dialog animations are one-time (non-loop).
 * Plus the {@code 眨眼} blink loop, which runs on its own always-on controller
 * (registered before the main one so other animations win shared bones).
 * Dolls use 66-69; characters use 70-76.
 */
public interface GeoYoukaiAnim extends GeoEntity {

	RawAnimation USE_MAINHAND = RawAnimation.begin().thenPlay("使用主手物品");
	RawAnimation GREET = RawAnimation.begin().thenPlay("招呼");
	RawAnimation TALK_01 = RawAnimation.begin().thenPlay("交流_01");
	RawAnimation TALK_02 = RawAnimation.begin().thenPlay("交流_02");
	RawAnimation THINK = RawAnimation.begin().thenPlay("思考中");
	RawAnimation AGREE = RawAnimation.begin().thenPlay("肯定");
	RawAnimation DECLINE = RawAnimation.begin().thenPlay("拒绝");
	RawAnimation BLINK = RawAnimation.begin().thenLoop("眨眼");

	byte EVENT_USE_MAINHAND = 70;
	byte EVENT_THINK = 71;
	byte EVENT_AGREE = 72;
	byte EVENT_DECLINE = 73;
	byte EVENT_TALK_01 = 74;
	byte EVENT_TALK_02 = 75;
	byte EVENT_GREET = 76;

	String ANIM_CONTROLLER = "main";
	String WINK_CONTROLLER = "wink";
	String USE_TRIGGER = "use_mainhand";
	String GREET_TRIGGER = "dialog_greet";
	String TALK_01_TRIGGER = "dialog_talk_01";
	String TALK_02_TRIGGER = "dialog_talk_02";
	String THINK_TRIGGER = "dialog_think";
	String AGREE_TRIGGER = "dialog_agree";
	String DECLINE_TRIGGER = "dialog_decline";

	default void broadcastAnim(byte event) {
		LivingEntity self = (LivingEntity) this;
		if (!self.level().isClientSide())
			self.level().broadcastEntityEvent(self, event);
	}

	default void broadcastUseMainhandAnim() {
		broadcastAnim(EVENT_USE_MAINHAND);
	}

	default void broadcastGreetAnim() {
		broadcastAnim(EVENT_GREET);
	}

	/**
	 * Play one of the two talk clips once, chosen at random.
	 */
	default void broadcastTalkAnim() {
		LivingEntity self = (LivingEntity) this;
		broadcastAnim(self.getRandom().nextBoolean() ? EVENT_TALK_01 : EVENT_TALK_02);
	}

	default void broadcastThinkAnim() {
		broadcastAnim(EVENT_THINK);
	}

	default void broadcastAgreeAnim() {
		broadcastAnim(EVENT_AGREE);
	}

	default void broadcastDeclineAnim() {
		broadcastAnim(EVENT_DECLINE);
	}

	/**
	 * Client-side: play the one-shot. Returns true when the id was consumed.
	 */
	default boolean handleAnimEvent(byte id) {
		if (id == EVENT_USE_MAINHAND) triggerAnim(ANIM_CONTROLLER, USE_TRIGGER);
		else if (id == EVENT_GREET) triggerAnim(ANIM_CONTROLLER, GREET_TRIGGER);
		else if (id == EVENT_TALK_01) triggerAnim(ANIM_CONTROLLER, TALK_01_TRIGGER);
		else if (id == EVENT_TALK_02) triggerAnim(ANIM_CONTROLLER, TALK_02_TRIGGER);
		else if (id == EVENT_THINK) triggerAnim(ANIM_CONTROLLER, THINK_TRIGGER);
		else if (id == EVENT_AGREE) triggerAnim(ANIM_CONTROLLER, AGREE_TRIGGER);
		else if (id == EVENT_DECLINE) triggerAnim(ANIM_CONTROLLER, DECLINE_TRIGGER);
		else return false;
		return true;
	}

}

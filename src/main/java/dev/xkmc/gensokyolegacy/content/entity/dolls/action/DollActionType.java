package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;

/**
 * Closed command vocabulary for doll actions. The glove speaks these; which items
 * satisfy each type is registry-open ({@link DollBehaviorRegistry}). There is no
 * priority between types — each doll holds a single ticket, and only a running
 * {@code REGULAR_ATTACK} can be aborted for a new order.
 */
public enum DollActionType {

	SUICIDE_ATTACK,
	SUPER_ATTACK,
	REGULAR_ATTACK,
	HEAL;

}

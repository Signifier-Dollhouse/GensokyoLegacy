package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;

/**
 * Closed command vocabulary for doll actions. The glove speaks these; which items satisfy
 * each type is registry-open ({@link DollBehaviorRegistry}). Each doll holds a single ticket,
 * so only a running {@code REGULAR_ATTACK} or {@code AUTO} ticket can be aborted.
 */
public enum DollActionType {

	SUICIDE_ATTACK,
	SUPER_ATTACK,
	REGULAR_ATTACK,
	HEAL

}

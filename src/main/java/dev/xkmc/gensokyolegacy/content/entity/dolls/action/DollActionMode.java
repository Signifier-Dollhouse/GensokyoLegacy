package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

/**
 * How a {@link DollAction} is issued.
 */
public enum DollActionMode {

	/** One glove command, one doll, one execution (super, suicide, scheduled heal). */
	ONE_TIME,
	/** One glove command fans across dolls, each doll at most once (regular volley). */
	ITERATIVE

}

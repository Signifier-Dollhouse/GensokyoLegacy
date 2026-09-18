package dev.xkmc.gensokyolegacy.content.entity.dolls.action;

/**
 * How a {@link DollAction} is issued.
 */
public enum DollActionMode {

	/** One glove command, one doll, one execution (super, suicide). */
	ONE_TIME,
	/** One glove command fans across dolls, each doll at most once (regular volley). */
	ITERATIVE,
	/**
	 * Behaves like {@code ONE_TIME} (one doll, one execution, no chaining) but a
	 * player command may interrupt it: a running {@code AUTO} ticket is aborted
	 * for a new order the same way a running regular attack is. Scheduler-issued
	 * heals use this mode.
	 */
	AUTO

}

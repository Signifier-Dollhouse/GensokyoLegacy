package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import java.util.ArrayList;
import java.util.List;

/**
 * Wheel visibility: rally and volley always show, skill and suicide only while a summoned doll can
 * perform them; the current mode is included, so the wheel index can never go missing.
 * @see "doc/design/doll/glove.md"
 */
public final class DollGloveModes {

	private DollGloveModes() {
	}

	public static List<DollGloveMode> available(DollGloveMode current, boolean hasSuper, boolean hasSuicide) {
		List<DollGloveMode> out = new ArrayList<>();
		for (var mode : DollGloveMode.values()) {
			if (mode == DollGloveMode.SUMMON || mode == DollGloveMode.VOLLEY ||
					mode == DollGloveMode.SUPER && hasSuper ||
					mode == DollGloveMode.SUICIDE && hasSuicide ||
					mode == current) {
				out.add(mode);
			}
		}
		return out;
	}

	/**
	 * Every mode that can ever appear on the wheel (the static side of
	 * {@link #available}): the selector list when no player is at hand.
	 */
	public static List<DollGloveMode> potentiallyVisible() {
		List<DollGloveMode> out = new ArrayList<>();
		for (var mode : DollGloveMode.values()) {
			if (mode == DollGloveMode.SUMMON || mode == DollGloveMode.VOLLEY ||
					mode == DollGloveMode.SUPER || mode == DollGloveMode.SUICIDE) {
				out.add(mode);
			}
		}
		return out;
	}

}

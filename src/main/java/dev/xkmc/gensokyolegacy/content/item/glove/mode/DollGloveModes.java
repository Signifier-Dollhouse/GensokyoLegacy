package dev.xkmc.gensokyolegacy.content.item.glove.mode;

import java.util.ArrayList;
import java.util.List;

/**
 * Wheel visibility for glove modes: rally and attack always show; skill and
 * suicide show only while a summoned doll can perform them (or while the glove
 * is already in that mode, so the holder can always switch away). A legacy
 * current mode outside the visible set is appended, so the wheel index never
 * goes missing.
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

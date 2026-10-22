package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * The three support auras a ghost can carry. The id is synced on the entity and mirrored into
 * {@code DollData.variant}, so a ghost keeps its variant across park and resummon.
 *
 * @see "doc/design/doll/stray.md"
 */
public enum GhostVariant {
	CURIOS, FOODIE, LIVELY;

	private static final GhostVariant[] VALUES = values();

	/**
	 * Texture stem under {@code textures/geo/ghost/}; the staged files use this exact spelling.
	 */
	public String texture() {
		return name().toLowerCase(Locale.ROOT) + "_ghost";
	}

	public Component getDisplayName() {
		return switch (this) {
			case CURIOS -> GLLang.Ghost.CURIOS.get();
			case FOODIE -> GLLang.Ghost.FOODIE.get();
			case LIVELY -> GLLang.Ghost.LIVELY.get();
		};
	}

	public static GhostVariant byId(int id) {
		return VALUES[Mth.clamp(id, 0, VALUES.length - 1)];
	}

	public static GhostVariant random(RandomSource random) {
		return VALUES[random.nextInt(VALUES.length)];
	}

}

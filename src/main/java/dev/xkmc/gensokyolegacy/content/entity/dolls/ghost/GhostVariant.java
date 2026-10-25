package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.Arrays;
import java.util.Locale;

/**
 * The variants a ghost can carry. The id is synced on the entity and mirrored into
 * {@code DollData.variant}, so a ghost keeps its variant across park and resummon.
 *
 * @see "doc/design/doll/stray.md"
 */
public enum GhostVariant {
	CURIOS(new CuriosGhostHandler()),
	FOODIE(new FoodieGhostHandler()),
	LIVELY(new LivelyGhostHandler()),
	/**
	 * Death ward: no upkeep and no aura. It is never rolled for a wild ghost, only summoned.
	 */
	DUAL(new DualGhostHandler());

	private static final GhostVariant[] VALUES = values();
	private static final GhostVariant[] WILD = Arrays.stream(VALUES)
			.filter(e -> e.handler.wild()).toArray(GhostVariant[]::new);

	public final GhostVariantHandler handler;

	GhostVariant(GhostVariantHandler handler) {
		this.handler = handler;
	}

	/**
	 * Texture stem under {@code textures/geo/ghost/}; the staged files use this exact spelling.
	 */
	public String texture() {
		return name().toLowerCase(Locale.ROOT) + "_ghost";
	}

	public Component getDisplayName() {
		return handler.getDisplayName();
	}

	public static GhostVariant byId(int id) {
		return VALUES[Mth.clamp(id, 0, VALUES.length - 1)];
	}

	/**
	 * The variants a wild ghost or a character may conjure on its own; dual is fan-only.
	 */
	public static GhostVariant[] wildValues() {
		return WILD.clone();
	}

	/**
	 * The wild roll never picks a variant whose handler forbids it; only the fan summons a dual.
	 */
	public static GhostVariant random(RandomSource random) {
		return WILD[random.nextInt(WILD.length)];
	}

}

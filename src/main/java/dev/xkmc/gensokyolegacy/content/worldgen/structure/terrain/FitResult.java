package dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain;

/**
 * Verdict of a {@link TerrainPredicate}: the vertical shift applied to the layout. The foundation itself is filled
 * later by {@link TerrainPredicate#afterPlace}.
 */
public record FitResult(boolean pass, int delta) {

	public static FitResult fail() {
		return new FitResult(false, 0);
	}

	public static FitResult pass(int delta) {
		return new FitResult(true, delta);
	}

}

package dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain;

/**
 * Verdict of a {@link TerrainPredicate}: the vertical shift applied to the layout, or the failure reason for the
 * diagnostics. The foundation itself is filled later by {@link TerrainPredicate#afterPlace}.
 */
public record FitResult(boolean pass, String reason, int delta) {

	public static FitResult fail(String reason) {
		return new FitResult(false, reason, 0);
	}

	public static FitResult pass(int delta) {
		return new FitResult(true, "pass", delta);
	}

}

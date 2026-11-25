package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import net.minecraft.world.level.LevelHeightAccessor;

/**
 * Snapshot of a level's vertical bounds, so a background structure query never touches the {@code ServerLevel}.
 */
public record LevelBounds(int minY, int height) implements LevelHeightAccessor {

	public static LevelBounds of(LevelHeightAccessor level) {
		return new LevelBounds(level.getMinBuildHeight(), level.getHeight());
	}

	@Override
	public int getMinBuildHeight() {
		return minY;
	}

	@Override
	public int getHeight() {
		return height;
	}

}

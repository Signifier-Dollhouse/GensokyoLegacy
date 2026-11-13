package dev.xkmc.gensokyolegacy.content.item.treasure;

import java.util.List;

/**
 * The client copy of the recipe list, filled by {@link dev.xkmc.gensokyolegacy.content.item.treasure.network.TreasureTowerToClient}.
 *
 * @see "doc/design/tower.md"
 */
public final class TreasureTowerClient {

	private static TreasureTowerData data = TreasureTowerData.EMPTY;

	private TreasureTowerClient() {
	}

	public static TreasureTowerData get() {
		return data;
	}

	public static void set(List<OreRecipe> recipes) {
		data = new TreasureTowerData(recipes);
	}

}

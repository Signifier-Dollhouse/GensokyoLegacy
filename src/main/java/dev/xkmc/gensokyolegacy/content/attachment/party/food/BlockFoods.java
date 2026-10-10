package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodHandler;
import net.neoforged.fml.ModList;

/**
 * The party system's block food handlers, in priority order.
 *
 * <p>
 * Called from the common setup once the blocks themselves are registered. One entry per
 * <em>kind</em> of food, each handler deciding for itself which blocks it covers — see
 * {@link PartyFoodHandler}.
 *
 * <p>
 * Order is load-bearing: the specific handlers come first and {@link BlockItemFood},
 * which accepts every block, comes last.
 *
 * <p>
 * Most handlers name a block class belonging to another mod, so each is only
 * <b>constructed</b> when that mod is present. A handler that was never constructed is
 * never asked, so its class is never loaded and its absent types are never resolved —
 * which is what keeps Farmer's Delight, Youkai's Feasts and Kaleidoscope Cookery
 * optional without reflection or a hard dependency.
 */
public final class BlockFoods {

	private static final String FD = "farmersdelight";
	private static final String YSF = "youkaisfeasts";
	private static final String KC = "kaleidoscope_cookery";

	/** Register every known kind of block food. Call once, after the blocks exist. */
	public static void register() {
		PartyFoodHandler.register(new CakeFood());

		if (loaded(FD)) {
			PartyFoodHandler.register(new FeastFood());
			PartyFoodHandler.register(new PieFood());
		}

		if (loaded(YSF)) {
			PartyFoodHandler.register(new SaucerFood());
			PartyFoodHandler.register(new PotFood());
			PartyFoodHandler.register(new CuisineBoardFood());
			PartyFoodHandler.register(new SteamerRackFood());
		}

		if (loaded(KC)) {
			PartyFoodHandler.register(new BiteFood());
			PartyFoodHandler.register(new PlateFood());
		}

		PartyFoodHandler.register(new BlockItemFood());
	}

	private static boolean loaded(String mod) {
		return ModList.get() == null || ModList.get().isLoaded(mod);
	}

	private BlockFoods() {
	}
}
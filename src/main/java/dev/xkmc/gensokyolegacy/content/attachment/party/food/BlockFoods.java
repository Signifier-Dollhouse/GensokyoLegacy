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
		// Vanilla cake. Registered against CakeBlock inside the handler, which is also
		// why Youkai's Feasts' YHCakeBlock and YHCandleCakeBlock need no entry here.
		PartyFoodHandler.register(new CakeFood());

		if (loaded(FD)) {
			// Youkai's Feasts' SurpriseFeastBlock extends FeastBlock and YHPieBlock
			// extends PieBlock, so these two cover those as well.
			PartyFoodHandler.register(new FeastFood());
			PartyFoodHandler.register(new PieFood());
		}

		if (loaded(YSF)) {
			PartyFoodHandler.register(new SaucerFood());
			PartyFoodHandler.register(new PotFood());
		}

		if (loaded(KC)) {
			// The mod's ~37 dishes and its one-by-two variant are all FoodBiteBlock, so
			// this one entry covers the lot — see BiteFood for why it cannot share
			// AbstractCountdownFood with the cake and pie.
			PartyFoodHandler.register(new BiteFood());
			// The eleven plates and platters are all PlateBlock, and count down like a
			// feast, so this one really is an AbstractCountdownFood.
			PartyFoodHandler.register(new PlateFood());
		}

		// The catch-all, last: it accepts every block, so anything earlier must come
		// first. This is what gives a datapack's foreign party_food entry a value.
		PartyFoodHandler.register(new BlockItemFood());
	}

	private static boolean loaded(String mod) {
		return ModList.get() == null || ModList.get().isLoaded(mod);
	}

	private BlockFoods() {
	}
}
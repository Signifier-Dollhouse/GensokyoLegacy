package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodHandler;
import net.neoforged.fml.ModList;

/**
 * Registers one handler per kind of block food, in priority order. Most name a block class
 * belonging to another mod, so each is only <b>constructed</b> when that mod is present.
 * @see "doc/design/party/food_mods.md"
 */
public final class BlockFoods {

	private static final String FD = "farmersdelight";
	private static final String YSF = "youkaisfeasts";
	private static final String KC = "kaleidoscope_cookery";
	private static final String DD = "displaydelight";
	private static final String SAR = "someassemblyrequired";

	/**
	 * Register every known kind of block food. Call once, after the blocks exist. {@link BlockItemFood} must stay last.
	 */
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

		if (loaded(DD)) {
			PartyFoodHandler.register(new DisplayFood());
		}

		if (loaded(SAR)) {
			PartyFoodHandler.register(new SandwichFood());
		}

		PartyFoodHandler.register(new BlockItemFood());
	}

	private static boolean loaded(String mod) {
		return ModList.get() == null || ModList.get().isLoaded(mod);
	}

	private BlockFoods() {
	}
}
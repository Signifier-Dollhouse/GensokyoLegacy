package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.content.party.core.BlockFoodHandler;
import dev.xkmc.gensokyolegacy.content.party.core.PartyFoodHandler;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Which block foods the party system knows how to read.
 * <p>
 * Called from the common setup once the blocks themselves are registered. Every
 * registration is gated on its mod being loaded and on its class being resolvable,
 * so the whole file can be written out unconditionally and costs nothing when a mod
 * is absent.
 * <p>
 * Registrations are by <b>class</b>, and a class covers its subclasses
 * ({@link PartyFoodHandler}), which is what keeps these lists short: one entry for
 * {@code FeastBlock} covers every Farmer's Delight feast, and one for
 * {@code BaseSaucerBlock} covers every Youkai's Feasts dish.
 */
public final class BlockFoods {

	private static final String FD = "farmersdelight";
	private static final String YSF = "youkaisfeasts";

	/** Register every known block food. Call once, after the blocks are registered. */
	public static void register() {
		vanilla();
		if (loaded(FD)) farmersDelight();
		if (loaded(YSF)) youkaisFeasts();
	}

	/**
	 * Vanilla cake: the only vanilla block food, and the one the party system has to
	 * work without any other mod installed.
	 * <p>
	 * It carries no {@link net.minecraft.world.food.FoodProperties} at all —
	 * {@code CakeBlock.eat} hands out {@code eat(2, 0.1F)} per slice from code and
	 * the block item is foodless — so its per-slice numbers are given by hand. Read
	 * from the item it would score at the minimum and be worth laying out for
	 * nothing.
	 */
	private static void vanilla() {
		PartyFoodHandler.register(Blocks.CAKE.getClass(), BlockFoodHandler.slice(2, 0.4f), null);
	}

	/**
	 * Farmer's Delight: feasts and pies.
	 * <p>
	 * Their block items carry no food either — the food lives on a serving item handed
	 * out when the dish is clicked — so each is registered against its serving item.
	 * Feasts count six servings down through {@code servings}; pies are counted in
	 * {@code bites}, four to a pie.
	 * <p>
	 * Registered per class, not per block: {@code FeastBlock} covers every feast,
	 * {@code PieBlock} every pie, including any the mod adds later. The classes are
	 * reached by name because Farmer's Delight is a compile-time dependency only.
	 */
	private static void farmersDelight() {
		registerClass("vectorwing.farmersdelight.common.block.FeastBlock",
				BlockFoodHandler.countdown("farmersdelight:roast_chicken"),
				Map.of(
						"roast_chicken_block", "farmersdelight:roast_chicken",
						"stuffed_pumpkin_block", "farmersdelight:stuffed_pumpkin",
						"shepherds_pie_block", "farmersdelight:shepherds_pie",
						"honey_glazed_ham_block", "farmersdelight:honey_glazed_ham",
						"gleaming_salad_block", "farmersdelight:gleaming_salad",
						"rice_roll_medley_block", "farmersdelight:rice_roll_medley"
				));
		registerClass("vectorwing.farmersdelight.common.block.PieBlock",
				BlockFoodHandler.countdown(null),
				Map.of(
						"apple_pie", "farmersdelight:apple_pie_slice",
						"pumpkin_pie", "farmersdelight:pumpkin_pie_slice",
						"chocolate_pie", "farmersdelight:chocolate_pie_slice",
						"sweet_berry_cheesecake", "farmersdelight:sweet_berry_cheesecake_slice"
				));
	}

	/**
	 * Youkai's Feasts, which has several distinct kinds of food and so needs one
	 * registration per shape rather than a single list:
	 *
	 * <ul>
	 *   <li><b>Saucer dishes</b> — {@code BaseSaucerBlock}. One serve, eaten whole,
	 *       food on a separate serving item, nothing tracked on the block.</li>
	 *   <li><b>Pots</b> — {@code BowlBlock}, two or four servings under its own
	 *       {@code serve} property.</li>
	 *   <li><b>Cakes</b> — {@code YHCakeBlock} extends vanilla {@code CakeBlock} and
	 *       so inherits its seven {@code bites}; registering the class here also
	 *       covers {@code YHCandleCakeBlock}, which is the same block with a candle.</li>
	 *   <li><b>Pies</b> — {@code YHPieBlock} extends Farmer's Delight's
	 *       {@code PieBlock} and so inherits its {@code bites} countdown.</li>
	 *   <li><b>Feasts</b> — {@code SurpriseFeastBlock} extends FD's
	 *       {@code FeastBlock}.</li>
	 * </ul>
	 *
	 * Because the lookup is per class, the saucer registration covers every dish
	 * including ones added later, and the cake and pie entries are inherited by
	 * subclasses rather than repeated.
	 */
	private static void youkaisFeasts() {
		registerClass("dev.xkmc.youkaishomecoming.content.block.food.BaseSaucerBlock",
				BlockFoodHandler.whole(null), Map.of());
		registerClass("dev.xkmc.youkaishomecoming.content.block.food.BowlBlock",
				BlockFoodHandler.countdown(null), Map.of());
		registerClass("dev.xkmc.youkaishomecoming.content.block.food.YHCakeBlock",
				BlockFoodHandler.slice(2, 0.4f), Map.of());
	}

	/**
	 * Register a handler for a block class named by string, so no foreign mod is a
	 * compile-time dependency.
	 *
	 * @param perBlock overrides the handler for individual blocks of that class,
	 *                 where one dish's serving item differs from the class default
	 */
	@SuppressWarnings("unchecked")
	private static void registerClass(String className, BlockFoodHandler handler,
			Map<String, String> perBlock) {
		var loaded = loadClass(className);
		if (loaded == null || !Block.class.isAssignableFrom(loaded)) return;
		var clazz = (Class<? extends Block>) loaded;
		PartyFoodHandler.register(clazz, handler, null);
		for (var entry : perBlock.entrySet()) {
			var block = byId(FD, entry.getKey());
			// per-block override, for the blocks of one class that read different
			// food: every feast is a FeastBlock but each hands out its own serving
			if (block != null) {
				PartyFoodHandler.register(block, BlockFoodHandler.countdown(entry.getValue()), FD);
			}
		}
	}

	@Nullable
	private static Class<?> loadClass(String name) {
		try {
			return Class.forName(name, false, BlockFoods.class.getClassLoader());
		} catch (ClassNotFoundException | LinkageError e) {
			return null;
		}
	}

	@Nullable
	private static Block byId(String namespace, String path) {
		var id = GensokyoLegacy.loc(namespace + ":" + path);
		var block = BuiltInRegistries.BLOCK.get(id);
		return block == Blocks.AIR ? null : block;
	}

	private static boolean loaded(String mod) {
		return ModList.get() == null || ModList.get().isLoaded(mod);
	}

	private BlockFoods() {
	}
}
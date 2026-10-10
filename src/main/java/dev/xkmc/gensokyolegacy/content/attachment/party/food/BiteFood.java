package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import com.github.ysbbbbbb.kaleidoscopecookery.block.food.FoodBiteBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Kaleidoscope Cookery dish: a placed block eaten one bite at a time, of which there
 * are many — the mod's whole Chinese-cuisine block is built from this one class.
 *
 * <p>
 * <b>Not an {@link AbstractCountdownFood}, despite the {@code bites} property.</b> The
 * counting convention is close to vanilla cake's but off by one at the top, and that
 * one step is the whole reason this handler stands on its own:
 *
 * <table border="1">
 *   <caption>Vanilla cake vs a Cookery bite block</caption>
 *   <tr><th>Block</th><th>{@code bites}</th><th>Fresh</th><th>Fully eaten</th>
 *       <th>Serves</th></tr>
 *   <tr><td>{@code CakeBlock}</td><td>{@code 0..6}</td><td>0</td><td>6</td>
 *       <td>{@code value + 1} → 7</td></tr>
 *   <tr><td>{@code FoodBiteBlock}</td><td>{@code 0..maxBites}</td><td>0</td>
 *       <td><b>{@code maxBites}</b></td><td><b>{@code maxBites - value}</b></td></tr>
 * </table>
 *
 * <p>
 * Vanilla cake runs its property one <em>past</em> the last slice — {@code BITES} 0..6
 * for seven slices — because {@code CakeBlock.eat} needs somewhere to land after the
 * seventh and removes the block from there. Cookery's {@code FoodBiteBlock} instead
 * builds {@code IntegerProperty.create("bites", 0, maxBites)} and stops counting at the
 * maximum, leaving an emptied block standing rather than removing it. So a fresh dish
 * holds {@code maxBites} bites, and the shared "counts up, therefore {@code value + 1}"
 * reading would report one serve too many — and {@code consume} would compute
 * {@code 0 + 1 = 1 > maxBites} on the very first bite and destroy a whole untouched dish.
 * Asking the block for {@link FoodBiteBlock#getMaxBites} instead of deriving the count
 * from the property's range is what keeps this correct.
 *
 * <p>
 * <b>Where the food is.</b> Unlike a feast or a pie, a Cookery bite block is fed straight
 * from a {@link FoodProperties} held on the block, not from a serving item — and that
 * field is {@code protected} with no accessor. The public way in is
 * {@link FoodBiteRegistry#FOOD_DATA_MAP}: {@code CommonRegistry} builds every
 * {@code FoodBiteBlock} from the {@code FoodData} of the same id and registers it under
 * that id, so the map holds an entry for every bite block there is, and
 * {@code blockFood()} is the very properties instance the block feeds per bite. No
 * reflection, and nothing to break if a field is renamed.
 *
 * <p>
 * {@code itemFood()} on the same entry must <b>not</b> be used. That is the whole dish,
 * not a bite of it; valuing each serve at the entire meal would score a single
 * Dark Cuisine plate higher than a golden apple.
 *
 * <p>
 * <b>Not a support problem</b> (§2.3): {@code FoodBiteBlock} declares no
 * {@code canSurvive}, so it sits on any party table unmodified.
 *
 * <p>
 * <b>Load order:</b> references {@code FoodBiteBlock}, so it is only ever constructed
 * when Kaleidoscope Cookery is present.
 */
public class BiteFood implements BlockFoodHandler {

	@Override
	public boolean handles(Block block) {
		// Also covers FoodBiteOneByTwoBlock, whose two halves are each independently
		// eatable and so are honestly two dishes as far as a per-block scan is concerned.
		return block instanceof FoodBiteBlock;
	}

	@Override
	public int serves(BlockState state) {
		if (!(state.getBlock() instanceof FoodBiteBlock bite)) return 0;
		return Math.max(0, bite.getMaxBites() - state.getValue(bite.getBites()));
	}

	/**
	 * From the registry rather than off a serving item, because Cookery has no item for a
	 * single bite — the dish item is the whole plate, which is the thing this handler
	 * exists not to value.
	 * <p>
	 * The block's own item is what a guest is handed, so a bite block is named after its
	 * block exactly like vanilla cake, but for a different and more mundane reason: there
	 * is no bite in the game to hand over.
	 */
	@Override
	public @Nullable FoodProperties food(BlockState state) {
		var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		var data = FoodBiteRegistry.FOOD_DATA_MAP.get(id);
		if (data == null) return null;   // not registered as a bite block after all
		return data.blockFood();
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof FoodBiteBlock bite) || serves(state) <= 1) {
			// The block outlives its last bite in Cookery, standing there empty. Taking
			// the final serve removes it instead, which is what every other countdown
			// handler here does and keeps a spent dish from standing on the table forever.
			level.destroyBlock(pos, false);
			return 0;
		}
		var bites = bite.getBites();
		level.setBlock(pos, state.setValue(bites, state.getValue(bites) + 1), 3);
		return serves(state) - 1;
	}
}

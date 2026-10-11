package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyFoodValue;
import dev.xkmc.youkaishomecoming.content.pot.table.board.CuisineBoardBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.table.item.FoodTableItem;
import dev.xkmc.youkaishomecoming.content.pot.table.item.TableItem;
import dev.xkmc.youkaishomecoming.init.registrate.YHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A Youkai's Feasts cutting board: whatever is lying on it, a list rather than a count.
 *
 * <p>A stack on a board is one plate, not a number of servings — {@code addToPlayer} hands the
 * pair over whole — so a pair feeds a guest twice and attracts once.
 *
 * @see doc/design/party/food_mods.md
 */
public class CuisineBoardFood implements ContainerFoodHandler {

	/** The board's contents, or null if this build of Youkai's Feasts has no such field. */
	private static final Field CONTENTS = lookup();

	private static Field lookup() {
		for (var field : CuisineBoardBlockEntity.class.getDeclaredFields()) {
			if (!field.getName().equals("contents")) continue;
			field.setAccessible(true);
			return field;
		}
		return null;
	}

	/**
	 * The board's own list of item stacks, or null when it cannot be read. A checked exception
	 * is swallowed into a null on purpose: a renamed field should read as "no food on it"
	 * rather than take the party down from a food handler on the server tick.
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	private static List<ItemStack> of(CuisineBoardBlockEntity be) {
		if (CONTENTS == null) return null;
		try {
			return (List<ItemStack>) CONTENTS.get(be);
		} catch (IllegalAccessException | ClassCastException e) {
			return null;
		}
	}

	@Override
	public boolean handles(Block block) {
		// a DelegateBlock has no class to test for - CuisineBoardBlock is a BlockMethod, not a
		// Block - so identity is the only way to name it
		return block == YHBlocks.CUISINE_BOARD.get();
	}

	@Override
	public List<FoodResult> contents(CollisionGetter level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof CuisineBoardBlockEntity be)) return List.of();
		// resolved before anything is read out of the list, because a board whose model was
		// dropped on a chunk load rebuilds it from the contents - and may rewrite them
		var model = be.getModel();
		var raw = of(be);
		if (raw == null) return List.of();
		if (!isDish(model, raw)) return List.of();
		var ans = new ArrayList<FoodResult>();
		for (var stack : raw) {
			if (stack == null || stack.isEmpty()) continue;
			// One piece for the appeal, the whole stack for the plate: two nigiri came off the
			// board together, so they feed the guest twice but are priced once.
			var dish = Servings.of(stack);
			if (dish == null) continue;   // nothing worth eating; left on the board, just not part of the spread
			// the whole stack, at its worth: she picks up the pair, not one piece of it
			var props = Servings.of(stack, stack.getCount());
			var item = stack.getItem();
			ans.add(FoodResult.of(new ItemStack(item, stack.getCount()),
					state.getBlock(), 1, props, PartyFoodValue.appealOf(dish),
					(l, p, s) -> {
						take(be, item);
						return 0;
					}));
		}
		return ans;
	}

	/**
	 * Whether the board holds a finished dish rather than a recipe part-way through. A
	 * {@link FoodTableItem} is the mod's own answer, but a nigiri or gunkan is placed on the
	 * board as the finished item and resolves to the model used to <i>assemble</i> it, so
	 * there is no second form to ask: the answer is read off the contents instead — a dish
	 * is made of nothing but food, and a recipe in progress is not.
	 */
	private static boolean isDish(TableItem model, List<ItemStack> raw) {
		if (model instanceof FoodTableItem) return true;
		for (var stack : raw) {
			if (stack == null || stack.isEmpty()) continue;
			if (Servings.of(stack) == null) return false;
		}
		return true;
	}

	/** Only a board actually <i>on the table</i> is party food, the same furniture test every other dish uses. */
	@Override
	public boolean isPutOut(Level level, BlockPos pos, BlockState state) {
		return PartyArea.touchingFurniture(level, pos);
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof CuisineBoardBlockEntity be)) return 0;
		var raw = of(be);
		if (raw == null) return 0;
		for (var stack : raw) {
			if (stack == null || stack.isEmpty()) continue;
			// the first thing worth eating, which is not necessarily the first thing: the
			// board may be holding a husk underneath the sushi
			if (Servings.of(stack) == null) continue;
			take(be, stack.getItem());
			break;
		}
		return countItems(raw);
	}

	/**
	 * Take the whole plate of {@code item} off the board — a nigiri recipe yields two at once
	 * and {@code addToPlayer} puts both in the guest's inventory together, so shrinking by one
	 * would leave a lone piece on a board whose model still describes a pair. Emptied in place
	 * rather than replacing the list, because the board's own {@code addItem} matches against
	 * this very list.
	 */
	private void take(CuisineBoardBlockEntity be, Item item) {
		var raw = of(be);
		if (raw == null) return;
		for (var stack : raw) {
			if (stack == null || stack.isEmpty() || stack.getItem() != item) continue;
			stack.setCount(0);
			be.notifyTile();
			return;
		}
	}

	private static int countItems(List<ItemStack> raw) {
		int total = 0;
		for (var stack : raw) {
			if (stack != null && !stack.isEmpty()) total += stack.getCount();
		}
		return total;
	}
}

package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.youkaishomecoming.content.pot.table.board.CuisineBoardBlockEntity;
import dev.xkmc.youkaishomecoming.content.pot.table.item.FoodTableItem;
import dev.xkmc.youkaishomecoming.init.registrate.YHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * A Youkai's Feasts cutting board: whatever is lying on it, however much of it there is.
 *
 * <p>
 * A board is a container in the ordinary sense — you put ingredients on it and a tool
 * turns them into a dish — and that is exactly why it needs
 * {@link ContainerFoodHandler} rather than {@link BlockFoodHandler}. Its food is a list of
 * <i>items</i> with item ids and item values, not a number on a block, and none of it is
 * visible in the block state. A guest at a party eats the ingredients off the board, not
 * the recipe the board is part-way through making.
 *
 * <p>
 * Whether there is anything to eat is decided by resolving the board's
 * {@link dev.xkmc.youkaishomecoming.content.pot.table.item.TableItem} model and asking
 * whether it came back as a {@code FoodTableItem}: that is the form that carries
 * contents, and an empty board never gets one. Reading the list is then done from the
 * block entity rather than from the model, because the model's copy is the snapshot taken
 * when the recipe matched while the block entity's is live — and because a guest taking
 * one item has to take it from the board itself, or the next guest will be offered food
 * that is no longer there.
 *
 * <p>
 * <b>The contents are read reflectively, and that is forced.</b> Both the board's list and
 * the model's are private, with no accessor on either. The alternatives were a mixin
 * accessor, which would need a config plugin to survive Youkai's Feasts being absent —
 * and it <i>is</i> absent from a dev launch, where it is {@code compileOnly}, so an
 * unguarded mixin would fail the game every time rather than only when the feature is used
 * — or asking {@code getHints} for the recipe's ingredients, which describes what the
 * board could become rather than what is on it. A board holding one apple out of a
 * three-part recipe would report three ingredients and a party would be budgeted against
 * food the host never put out. The field is resolved once and never again.
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
	 * The board's own list of item stacks, or null when it cannot be read.
	 *
	 * <p>
	 * A checked exception from {@link Field#get} is swallowed into a null rather than
	 * propagated: this is called from a food handler on the server tick, and a board whose
	 * field has been renamed by an update should read as "no food on it" rather than take
	 * the party down with it.
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
		// a DelegateBlock has no class of its own to test for - CuisineBoardBlock is a
		// BlockMethod, not a Block - so identity is the only way to name it
		return block == YHBlocks.CUISINE_BOARD.get();
	}

	@Override
	public List<FoodResult> contents(Level level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof CuisineBoardBlockEntity be)) return List.of();
		// The gate. A board's own model is a bare TableItem until something on it matches a
		// food table; only then is it a FoodTableItem, and only a FoodTableItem has food
		// on it. An empty board, or one holding things that are not ingredients for any
		// registered table, is not party food — which is the right answer for both, since
		// there is nothing on it that a guest could eat.
		if (!(be.getModel() instanceof FoodTableItem)) return List.of();
		var raw = of(be);
		if (raw == null) return List.of();
		var ans = new ArrayList<FoodResult>();
		for (var stack : raw) {
			if (stack == null || stack.isEmpty()) continue;
			var props = Servings.of(stack);
			// nothing worth eating, or nothing worth eating to a guest - poison, no
			// nutrition, a husk. Left on the board; simply not part of the spread.
			if (props == null) continue;
			var item = stack.getItem();
			ans.add(new FoodResult(BuiltInRegistries.ITEM.getKey(item), stack.getCount(), props,
					(l, p, s) -> take(be, item)));
		}
		return ans;
	}

	/**
	 * Only a board actually <i>on the table</i> is party food.
	 *
	 * <p>
	 * The same furniture test every other dish uses, so a board on the floor is as much
	 * off-menu as a dropped pie. A board is flat and thin and sits at table height like
	 * any plate, so there is no reason to give it a looser rule than one.
	 */
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
			// the first item worth eating, which is not necessarily the first item: the
			// board may be holding a husk underneath the cheese
			if (Servings.of(stack) == null) continue;
			var item = stack.getItem();
			int before = stack.getCount();
			int left = take(be, item);
			if (left != before) return left;   // something was actually taken
		}
		return countItems(raw);
	}

	/**
	 * Remove one {@code item} from the board and report how many are left.
	 *
	 * <p>
	 * Shrinks the stack in place rather than replacing the list, so the board's own recipe
	 * bookkeeping keeps working — {@code addItem} matches against this very list, and
	 * swapping it out from under the block entity would leave the board believing it held
	 * something it does not.
	 */
	private int take(CuisineBoardBlockEntity be, Item item) {
		var raw = of(be);
		if (raw == null) return 0;
		for (var stack : raw) {
			if (stack == null || stack.isEmpty() || stack.getItem() != item) continue;
			stack.shrink(1);
			be.notifyTile();
			return stack.getCount();
		}
		return 0;
	}

	private static int countItems(List<ItemStack> raw) {
		int total = 0;
		for (var stack : raw) {
			if (stack != null && !stack.isEmpty()) total += stack.getCount();
		}
		return total;
	}
}

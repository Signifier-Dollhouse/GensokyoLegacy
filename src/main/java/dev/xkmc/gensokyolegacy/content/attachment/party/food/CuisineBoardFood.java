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
 * A board is a container in the ordinary sense — you put things on it and a tool turns
 * them into a dish — and that is exactly why it needs
 * {@link ContainerFoodHandler} rather than {@link BlockFoodHandler}. Its food is a list of
 * <i>items</i> with item ids and item values, not a number on a block, and none of it is
 * visible in the block state. A guest at a party eats what is on the board, not the recipe
 * the board is part-way through making — a board holding half a maki is not half a party
 * lunch.
 *
 * <p>
 * Whether there is anything to eat is decided by asking the board what it is holding —
 * see {@link #isDish}. Reading the list is then done from the block entity rather than
 * from the model, because the model's copy is the snapshot taken when the recipe matched
 * while the block entity's is live — and because a guest taking a plate has to take it
 * from the board itself, or the next guest is offered food that is no longer there.
 *
 * <p>
 * <b>A stack on a board is one plate, not a number of servings.</b> Youkai's Feasts
 * writes its sushi and gunkan recipes to yield two at a time, and a board handed a pair
 * keeps the pair as one stack that {@code addToPlayer} gives over whole — there is no
 * interaction that lifts one off and leaves the other. So each stack is reported as one
 * serving, and taking one empties it. Reporting the count as servings instead would let
 * two guests each eat one, which is not what the mod allows.
 *
 * <p>
 * <b>The plate feeds twice and is worth once.</b> A guest handed the pair really does eat
 * both, so her fulfillment is the pair's; but the party counts each food once, so the
 * board offers one nigiri's worth of appeal and no more. A spread's appeal is then a
 * function of how many <i>kinds</i> of thing a host has put out rather than of how many
 * plates of each — see {@link FoodResult#appeal()} and {@code PartyFoodValue.appealPerFood}.
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
		// resolved first and before anything is read out of the list, because a board whose
		// model was dropped on a chunk load rebuilds it from the contents - and may rewrite
		// the contents doing so
		var model = be.getModel();
		var raw = of(be);
		if (raw == null) return List.of();
		// The gate. A board is party food when what is lying on it is a dish rather than a
		// step towards one; see isDish for how that is told apart, since the two look the
		// same to a guest and very different to the mod.
		if (!isDish(model, raw)) return List.of();
		var ans = new ArrayList<FoodResult>();
		for (var stack : raw) {
			if (stack == null || stack.isEmpty()) continue;
			// One piece, for the appeal. Two pieces, for the plate. The board hands a
			// guest both of whatever came off it together, so she is fed twice - but the
			// party is counting how good the spread is, not how large its helpings are,
			// and a food is priced once. Two nigiri on a board are therefore worth the
			// appeal of one nigiri and the fulfillment of two, which is the whole of the
			// difference between the two numbers below.
			var dish = Servings.of(stack);
			// nothing worth eating, or nothing worth eating to a guest - poison, no
			// nutrition, a husk. Left on the board; simply not part of the spread.
			if (dish == null) continue;
			// the whole stack, at its worth: two nigiri came off the board together and
			// go back into the guest together, so they are one plate of twice the size
			var props = Servings.of(stack, stack.getCount());
			var item = stack.getItem();
			// The serving is the whole stack, at its own count: two nigiri came off the
			// board together and go into the guest together, so she picks up the pair and
			// not one piece of it. This is the only food in the system whose serving is
			// more than one item, and the reason is a property of the board rather than
			// of the food.
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
	 * Whether the board holds a finished dish rather than a recipe part-way through.
	 *
	 * <p>
	 * A {@link FoodTableItem} is the mod's own answer to this: the model a board's
	 * contents resolve to once they match a registered food table, and never produced by
	 * anything short of a whole dish. A board that is empty, or holding cooked rice and
	 * nothing else, resolves to a bare {@code TableItem} instead and is not party food —
	 * which is the right answer, because there is nothing on it a guest could eat.
	 *
	 * <p>
	 * <b>A nigiri or a gunkan is not a {@code FoodTableItem}, and this is why that does not
	 * stop it counting.</b> Those two are put on the board as the finished item rather than
	 * assembled there, and the model the board resolves to is the one the mod uses to
	 * <i>assemble</i> them — so it carries both the recipe and the dish, and there is no
	 * second form of it to ask which of the two you are looking at. What is on the board
	 * is. The answer is therefore read off the contents directly: a dish is made of
	 * nothing but food, and a recipe in progress is not — there is rice, kelp or a sauce
	 * on every one of them, none of which is anything a guest can eat.
	 */
	private static boolean isDish(TableItem model, List<ItemStack> raw) {
		if (model instanceof FoodTableItem) return true;
		for (var stack : raw) {
			if (stack == null || stack.isEmpty()) continue;
			if (Servings.of(stack) == null) return false;
		}
		return true;
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
			// the first thing worth eating, which is not necessarily the first thing: the
			// board may be holding a husk underneath the sushi
			if (Servings.of(stack) == null) continue;
			take(be, stack.getItem());
			break;
		}
		return countItems(raw);
	}

	/**
	 * Take the whole plate of {@code item} off the board.
	 *
	 * <p>
	 * Empties the stack in place rather than replacing the list, so the board's own recipe
	 * bookkeeping keeps working — {@code addItem} matches against this very list, and
	 * swapping it out from under the block entity would leave the board believing it held
	 * something it does not.
	 *
	 * <p>
	 * <b>The whole stack, because that is what a board hands over.</b> A nigiri recipe
	 * yields two at once and {@code addToPlayer} puts both in the guest's inventory
	 * together; there is no way to lift one off a board and leave its partner, so a guest
	 * cannot come back for seconds either. The stack is emptied here to match, rather than
	 * shrinking by one and leaving a lone piece of sushi on a board whose model still
	 * describes a pair.
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

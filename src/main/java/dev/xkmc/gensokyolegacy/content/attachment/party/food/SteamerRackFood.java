package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.youkaishomecoming.content.pot.steamer.RackItemData;
import dev.xkmc.youkaishomecoming.content.pot.steamer.SteamerBlockEntity;
import dev.xkmc.youkaishomecoming.init.registrate.YHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * A Youkai's Feasts steamer: the food on its racks, whether it is reached as the pot or
 * as one of the racks standing in it.
 *
 * <p>
 * Both halves of the same vessel. The pot carries up to two rack tiers of its own — the
 * {@code pot_racks} property, which is how you steam without a rack at all — and each
 * rack block stands in the pot with up to four tiers of its own. They are separate block
 * entities with separate storage, so a full steamer is genuinely two food sources and
 * neither shadows the other.
 *
 * <p>
 * Unlike the cutting board this needs no reflection: the contents are public fields on
 * {@code RackData} and {@code RackItemData}. The whole of the difficulty is in
 * {@link #isPutOut}.
 *
 * <p>
 * <b>The two halves are placed differently, and that is the rule worth writing down.</b> A
 * pot may stand anywhere — on a stove, on the table, on the ground — because a pot of food
 * on a kitchen floor is a thing a host has actually done and refusing it would put the
 * feature out of reach in the one room it was made for. A rack may not: on the table like
 * anything else, or inside a pot, and on the ground it is not food. A rack is a part of a
 * steamer rather than a dish in its own right, so it needs a pot or a table to be part of
 * anything — which is the part the generic furniture test cannot express, because the pot
 * has to be looked <i>through</i>.
 */
public class SteamerRackFood implements ContainerFoodHandler {

	/**
	 * How far to fall looking for what a rack is standing in.
	 *
	 * <p>
	 * A steamer is at most three blocks of pot with two rack tiers in it, so this is
	 * generous; it only has to be larger than the tallest pot, because a rack on the
	 * ground finds a solid floor immediately and a rack inside a pot falls past the pot's
	 * interior before reaching the pot's own block.
	 */
	private static final int SUPPORT_DROP = 4;

	@Override
	public boolean handles(Block block) {
		// two DelegateBlocks, so identity against the registered blocks is the only way to
		// name either of them; see the class note on why both are claimed
		return block == YHBlocks.STEAMER_RACK.get() || block == YHBlocks.STEAMER_POT.get();
	}

	@Override
	public List<FoodResult> contents(Level level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof SteamerBlockEntity be)) return List.of();
		var ans = new ArrayList<FoodResult>();
		for (var rack : be.racks) {
			for (var slot : rack.list) {
				if (slot == null || slot.stack.isEmpty()) continue;
				FoodResult result = describe(slot, be, state);
				if (result != null) ans.add(result);
			}
		}
		return merge(ans);
	}

	/**
	 * One entry per item currently on the rack.
	 *
	 * <p>
	 * Returned unmerged, one per occupied slot, and folded afterwards: the same item can
	 * sit in several slots of several racks, and a guest coming back for a second slice
	 * should not be served a board that lists it four times.
	 */
	@SuppressWarnings("unused")
	private FoodResult describe(RackItemData slot, SteamerBlockEntity be, BlockState state) {
		var stack = slot.stack;
		var props = Servings.of(stack);
		// unlike the board, the rack has no idea whether what is on it is fit to eat:
		// it will happily steam a bone. Worthless food is skipped, not refused wholesale.
		if (props == null) return null;
		// One item, however many the slot holds: a rack hands its contents over one at a
		// time, so a guest comes back for seconds. The stack it holds is what makes this
		// many serves, not what makes the serving big.
		var item = stack.getItem();
		return FoodResult.of(new ItemStack(item), state.getBlock(), stack.getCount(), props,
				(l, p, s) -> take(be, item));
	}

	/** Folds results that name the same item into one, summing their servings. */
	private static List<FoodResult> merge(List<FoodResult> raw) {
		var merged = new ArrayList<FoodResult>();
		for (var result : raw) {
			boolean folded = false;
			for (int i = 0; i < merged.size(); i++) {
				var have = merged.get(i);
				if (!have.id().equals(result.id())) continue;
				// two slots of the same thing are one food with twice the servings,
				// still handed over one item at a time
				merged.set(i, new FoodResult(have.serving(), have.blockId(),
						have.serves() + result.serves(), have.food(), have.consume()));
				folded = true;
				break;
			}
			if (!folded) merged.add(result);
		}
		return merged;
	}

	/**
	 * The pot is food wherever it stands; a rack is food only on the table or in a pot.
	 *
	 * <p>
	 * The pot has no placement test at all, which is the deliberate asymmetry and the
	 * only reason this is not {@code PartyArea.touchingFurniture}. A steamer is a tall
	 * object that belongs on a stove, and a host who has put one on the floor beside the
	 * table has still laid out a party — the food is in the room, at the table's radius,
	 * and it is absurd to make the vessel be on furniture when its own racks are not.
	 *
	 * <p>
	 * A rack is found by falling rather than by looking up, because a rack <i>inside</i> a
	 * pot has pot interior below it rather than the pot block: the block that actually
	 * carries it is two or three down.
	 */
	@Override
	public boolean isPutOut(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() == YHBlocks.STEAMER_POT.get()) return true;
		if (PartyArea.touchingFurniture(level, pos)) return true;
		var support = PartyArea.supportOf(level, pos, SUPPORT_DROP);
		return support != null && level.getBlockState(support).getBlock() == YHBlocks.STEAMER_POT.get();
	}

	@Override
	public int consume(Level level, BlockPos pos, BlockState state) {
		if (!(level.getBlockEntity(pos) instanceof SteamerBlockEntity be)) return 0;
		for (var rack : be.racks) {
			for (var slot : rack.list) {
				if (slot == null || slot.stack.isEmpty()) continue;
				if (Servings.of(slot.stack) == null) continue;
				return take(be, slot.stack.getItem());
			}
		}
		return remaining(be);
	}

	/**
	 * Take one {@code item} off any rack, in slot order.
	 *
	 * <p>
	 * Goes through {@code setStack} rather than shrinking the stack directly, because that
	 * is the rack's own setter and it re-files the slot's recipe id and notifies the block
	 * entity. Shrinking underneath it would leave the steaming recipe pointing at an
	 * empty slot.
	 */
	private int take(SteamerBlockEntity be, Item item) {
		for (var rack : be.racks) {
			for (var slot : rack.list) {
				if (slot == null || slot.stack.isEmpty() || slot.stack.getItem() != item) continue;
				var left = slot.stack.copyWithCount(slot.stack.getCount() - 1);
				slot.setStack(be, left);
				return left.getCount();
			}
		}
		return 0;
	}

	private static int remaining(SteamerBlockEntity be) {
		int total = 0;
		for (var rack : be.racks) {
			for (var slot : rack.list) {
				if (slot != null && !slot.stack.isEmpty()) total += slot.stack.getCount();
			}
		}
		return total;
	}
}

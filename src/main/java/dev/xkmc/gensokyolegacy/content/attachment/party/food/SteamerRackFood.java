package dev.xkmc.gensokyolegacy.content.attachment.party.food;

import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyArea;
import dev.xkmc.youkaishomecoming.content.pot.steamer.RackItemData;
import dev.xkmc.youkaishomecoming.content.pot.steamer.SteamerBlockEntity;
import dev.xkmc.youkaishomecoming.init.registrate.YHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * A Youkai's Feasts steamer, both halves of it — the pot's own tiers and the rack standing in it
 * are separate block entities, so a full steamer is genuinely two food sources. A pot is food
 * wherever it stands; a rack only on the table or in a pot.
 *
 * @see "doc/design/party/food_mods.md"
 */
public class SteamerRackFood implements ContainerFoodHandler {

	/**
	 * How far to fall looking for what a rack stands in; must exceed the tallest pot.
	 */
	private static final int SUPPORT_DROP = 4;

	@Override
	public boolean handles(Block block) {
		// two DelegateBlocks with no class of their own, so identity is the only way to name either
		return block == YHBlocks.STEAMER_RACK.get() || block == YHBlocks.STEAMER_POT.get();
	}

	@Override
	public List<FoodResult> contents(CollisionGetter level, BlockPos pos, BlockState state) {
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
	 * One entry per occupied slot, unmerged; {@link #merge} folds the same item across slots and racks.
	 */
	@SuppressWarnings("unused")
	private FoodResult describe(RackItemData slot, SteamerBlockEntity be, BlockState state) {
		var stack = slot.stack;
		var props = Servings.of(stack);
		// a rack will steam a bone, so worthless food is skipped rather than the
		// rack refused wholesale
		if (props == null) return null;
		// one item per serve: a rack hands its contents over one at a time, so the
		// stack's count is how many serves it makes
		var item = stack.getItem();
		return FoodResult.of(new ItemStack(item), state.getBlock(), stack.getCount(), props,
				(l, p, s) -> take(be, item));
	}

	/**
	 * Folds results that name the same item into one, summing their servings.
	 */
	private static List<FoodResult> merge(List<FoodResult> raw) {
		var merged = new ArrayList<FoodResult>();
		for (var result : raw) {
			boolean folded = false;
			for (int i = 0; i < merged.size(); i++) {
				var have = merged.get(i);
				if (!have.id().equals(result.id())) continue;
				// one food with twice the servings, still handed over one item at a time —
				// and so with one appeal
				merged.set(i, new FoodResult(have.serving(), have.blockId(),
						have.serves() + result.serves(), have.nutrition(), have.saturation(),
						have.appeal(), have.consume()));
				folded = true;
				break;
			}
			if (!folded) merged.add(result);
		}
		return merged;
	}

	/**
	 * The pot is food wherever it stands; a rack is food on furniture or inside a pot, and is
	 * found by falling, because the block carrying a rack inside a pot is two or three down.
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
	 * Take one {@code item} off any rack, in slot order. Goes through the rack's own
	 * {@code setStack}: shrinking the stack underneath would leave the slot's recipe id pointing at
	 * an empty slot.
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

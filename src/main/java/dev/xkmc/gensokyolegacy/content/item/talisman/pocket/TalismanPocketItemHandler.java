package dev.xkmc.gensokyolegacy.content.item.talisman.pocket;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.FoldedPaperTalisman;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 18-slot IItemHandlerModifiable facade over {@link TalismanPocketData}.
 * Slots 0..8 are the folded talisman row, slots 9..17 the reserve paper row,
 * index i holding the two stacks of pocket slot i (see {@link TalismanSlot}).
 *
 * <p>Slot input restriction is handled here ({@link #isItemValid}): a slot is
 * only locked to the kind present in its partnered slot — a folded slot is
 * restricted when its paper partner has an item, a paper slot when its folded
 * partner has an item; otherwise items of any kind are allowed.
 *
 * <p>The server-side menu is backed by the live pocket {@code ItemStack} and
 * writes every change back to {@link GLTalismans#DC_TALISMAN_POCKET}. The
 * client-side menu uses a dummy (empty) backing stack: slot contents then come
 * purely from container sync via {@code setStackInSlot}, so the input
 * restriction applies identically on both sides. It must be modifiable because
 * {@code ItemHandlerCopySlot.setStackCopy} casts to this interface.
 */
public class TalismanPocketItemHandler implements IItemHandlerModifiable {

	private final ItemStack stack;

	public TalismanPocketItemHandler(ItemStack stack) {
		this.stack = stack;
	}

	private TalismanPocketData data() {
		var data = GLTalismans.DC_TALISMAN_POCKET.get(stack);
		return data != null ? data : new TalismanPocketData(TalismanPocketData.defaultSlots());
	}

	private void save(TalismanPocketData data) {
		if (!stack.isEmpty()) {
			GLTalismans.DC_TALISMAN_POCKET.set(stack, data);
		}
	}

	private static int pair(int slot) {
		return slot % TalismanPocketData.MAX_SLOTS;
	}

	private static boolean foldedSlot(int slot) {
		return slot < TalismanPocketData.MAX_SLOTS;
	}

	@Override
	public int getSlots() {
		return TalismanPocketData.MAX_SLOTS * 2;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
		var pair = data().get(pair(slot));
		return foldedSlot(slot) ? pair.foldedStack() : pair.paperStack();
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		if (stack.isEmpty() || slot < 0 || slot >= getSlots()) return false;
		return foldedSlot(slot) ? isValidFolded(stack, pair(slot)) : isValidPaper(stack, pair(slot));
	}

	private boolean isValidFolded(ItemStack s, int index) {
		if (!(s.getItem() instanceof FoldedPaperTalisman)) return false;
		if (FoldedPaperTalisman.paper(s) == null) return false;
		ItemStack paper = data().get(index).paperStack();
		return paper.isEmpty() || paper.getItem().equals(FoldedPaperTalisman.paper(s));
	}

	private boolean isValidPaper(ItemStack s, int index) {
		if (!(s.getItem() instanceof TalismanPaperItem)) return false;
		ItemStack folded = data().get(index).foldedStack();
		if (folded.isEmpty()) return true;
		return folded.getItem() instanceof FoldedPaperTalisman && s.getItem().equals(FoldedPaperTalisman.paper(folded));
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		ItemStack s = stack.copy();
		if (s.isEmpty() || !isItemValid(slot, s)) return s;
		int index = pair(slot);
		int advance;
		var data = data();
		var pair = data.get(index);
		if (foldedSlot(slot)) {
			if (!pair.foldedStack().isEmpty()) return s;
			advance = 1;
		} else {
			advance = Math.min(s.getCount(), 64 - pair.paperStack().getCount());
		}
		if (advance <= 0) return s;
		if (simulate) {
			s.shrink(advance);
			return s;
		}
		save(data.with(index, foldedSlot(slot) ? pair.withFolded(s.copy()) : pair.withPaper(grow(pair.paperStack(), advance))));
		s.shrink(advance);
		return s;
	}

	private static ItemStack grow(ItemStack stack, int amount) {
		ItemStack ans = stack.copy();
		ans.grow(amount);
		return ans;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount <= 0 || slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
		int index = pair(slot);
		var data = data();
		var pair = data.get(index);
		if (foldedSlot(slot)) {
			ItemStack folded = pair.foldedStack();
			if (folded.isEmpty()) return ItemStack.EMPTY;
			if (simulate) return folded.copy();
			save(data.with(index, pair.withFolded(ItemStack.EMPTY)));
			return folded.copy();
		}
		ItemStack papers = pair.paperStack();
		if (papers.isEmpty()) return ItemStack.EMPTY;
		int n = Math.min(amount, papers.getCount());
		if (simulate) return shrinkTo(papers, n);
		ItemStack left = shrinkTo(papers, papers.getCount() - n);
		save(data.with(index, left.isEmpty() ? pair.withPaper(ItemStack.EMPTY) : pair.withPaper(left)));
		return shrinkTo(papers, n);
	}

	private static ItemStack shrinkTo(ItemStack stack, int count) {
		ItemStack ans = stack.copy();
		ans.setCount(count);
		return ans;
	}

	@Override
	public void setStackInSlot(int slot, ItemStack s) {
		if (slot < 0 || slot >= getSlots()) return;
		int index = pair(slot);
		var data = data();
		var pair = data.get(index);
		ItemStack copy = s.copy();
		if (foldedSlot(slot)) {
			if (copy.isEmpty()) {
				save(data.with(index, pair.withFolded(ItemStack.EMPTY)));
			} else if (copy.getItem() instanceof FoldedPaperTalisman && FoldedPaperTalisman.paper(copy) != null) {
				save(data.with(index, pair.withFolded(copy)));
			}
			return;
		}
		if (copy.isEmpty()) {
			save(data.with(index, pair.withPaper(ItemStack.EMPTY)));
		} else if (copy.getItem() instanceof TalismanPaperItem) {
			save(data.with(index, pair.withPaper(copy)));
		}
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

}
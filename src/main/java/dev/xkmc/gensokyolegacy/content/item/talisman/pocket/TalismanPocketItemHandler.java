package dev.xkmc.gensokyolegacy.content.item.talisman.pocket;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.FoldedPaperTalisman;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.TalismanPaperItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * 14-slot handler over {@link TalismanPocketData}: slots 0..6 hold folded talismans and 7..13 the reserve paper, slot i
 * pairing both halves of pocket slot i; a slot locks to the kind already in its partner, otherwise any kind fits.
 * Client menus back onto an empty dummy stack, so contents arrive purely by sync, and the handler must stay modifiable
 * because {@code ItemHandlerCopySlot.setStackCopy} casts to it.
 */
public class TalismanPocketItemHandler implements IItemHandlerModifiable {

	private final ItemStack stack;

	public TalismanPocketItemHandler(ItemStack stack) {
		this.stack = stack;
	}

	private TalismanPocketData data() {
		var data = GLTalismans.DC_TALISMAN_POCKET.get(stack);
		return data != null ? data : TalismanPocketData.defaults();
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
		var data = data();
		int index = pair(slot);
		return foldedSlot(slot) ? data.folded(index) : data.paper(index);
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		if (stack.isEmpty() || slot < 0 || slot >= getSlots()) return false;
		return foldedSlot(slot) ? isValidFolded(stack, pair(slot)) : isValidPaper(stack, pair(slot));
	}

	private boolean isValidFolded(ItemStack s, int index) {
		if (!(s.getItem() instanceof FoldedPaperTalisman)) return false;
		if (FoldedPaperTalisman.paper(s) == null) return false;
		ItemStack paper = data().paper(index);
		return paper.isEmpty() || paper.getItem().equals(FoldedPaperTalisman.paper(s));
	}

	private boolean isValidPaper(ItemStack s, int index) {
		if (!(s.getItem() instanceof TalismanPaperItem)) return false;
		ItemStack folded = data().folded(index);
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
		if (foldedSlot(slot)) {
			if (data.hasFolded(index)) return s;
			advance = 1;
		} else {
			advance = Math.min(s.getCount(), 64 - data.paper(index).getCount());
		}
		if (advance <= 0) return s;
		if (simulate) {
			s.shrink(advance);
			return s;
		}
		save(foldedSlot(slot) ? data.withFolded(index, s.copy()) : data.withPaper(index, grow(data.paper(index), advance)));
		s.shrink(advance);
		return s;
	}

	private static ItemStack grow(ItemStack stack, int amount) {
		return stack.copyWithCount(stack.getCount() + amount);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount <= 0 || slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
		int index = pair(slot);
		var data = data();
		if (foldedSlot(slot)) {
			ItemStack folded = data.folded(index);
			if (folded.isEmpty()) return ItemStack.EMPTY;
			if (simulate) return folded.copy();
			save(data.withFolded(index, ItemStack.EMPTY));
			return folded.copy();
		}
		ItemStack papers = data.paper(index);
		if (papers.isEmpty()) return ItemStack.EMPTY;
		int n = Math.min(amount, papers.getCount());
		if (simulate) return papers.copyWithCount(n);
		ItemStack left = papers.copyWithCount(papers.getCount() - n);
		save(data.withPaper(index, left.isEmpty() ? ItemStack.EMPTY : left));
		return papers.copyWithCount(n);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack s) {
		if (slot < 0 || slot >= getSlots()) return;
		int index = pair(slot);
		var data = data();
		ItemStack copy = s.copy();
		if (foldedSlot(slot)) {
			if (copy.isEmpty()) {
				save(data.withFolded(index, ItemStack.EMPTY));
			} else if (copy.getItem() instanceof FoldedPaperTalisman && FoldedPaperTalisman.paper(copy) != null) {
				save(data.withFolded(index, copy));
			}
			return;
		}
		if (copy.isEmpty()) {
			save(data.withPaper(index, ItemStack.EMPTY));
		} else if (copy.getItem() instanceof TalismanPaperItem) {
			save(data.withPaper(index, copy));
		}
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

}
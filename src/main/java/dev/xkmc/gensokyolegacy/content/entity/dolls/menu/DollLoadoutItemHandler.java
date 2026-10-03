package dev.xkmc.gensokyolegacy.content.entity.dolls.menu;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollInventory;
import dev.xkmc.gensokyolegacy.content.attachment.doll.MutableDollInventory;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

/**
 * 4-slot {@link IItemHandlerModifiable} facade over a doll's loadout, arranged
 * as a cross: slot 0 main hand (left), 1 off hand (right), 2 core (top),
 * 3 cloth (middle, the body-worn slot). A hand is <b>locked</b> to what a doll can
 * actually use — any bound item, plus the vanilla shield — and to a single item
 * per slot, the one exception being ammunition: an item a behavior spends one per
 * use, where N in the hand is N uses (loadout.md §4). Core and cloth are inert
 * single slots for now.
 *
 * <p>The two rules live here because this is the only writer: {@link
 * #isItemValid} is what a click asks before placing (and what a hopper asks), and
 * {@link #handStackLimit} is what the menu slot reports per item, so vanilla's
 * own split arithmetic never moves more than the rule allows. The one other
 * writer, Alice's arming ({@code AliceDollHost.arm}), goes straight to the ledger
 * and is not gated by these — it only ever hands out star wands and folded
 * talismans, which both admit.
 *
 * <p>Two live backings, mirroring {@code TalismanPocketItemHandler}:
 * the server-side entity menu writes straight into the ledger
 * {@link MutableDollInventory} and refreshes the client mirror afterwards;
 * the server-side item menu reads/writes the doll stack's {@code DOLL_LOADOUT}
 * component. The client-side menu uses a dummy inventory: contents then come
 * purely from container sync, so validation applies identically on both
 * sides. It must be modifiable because {@code ItemHandlerCopySlot} casts to
 * this interface.
 */
public class DollLoadoutItemHandler implements IItemHandlerModifiable {

	/**
	 * The most one hand can ever hold: a full stack of the largest ammunition — TNT
	 * at 64 (hexbrew bottles cap at 16 by their own item properties). What a hand
	 * holds for any one item is {@link #handStackLimit}: {@link #getSlotLimit} cannot
	 * see the stack, so it answers the ceiling and {@link #insertItem} applies the
	 * real rule per write.
	 */
	private static final int HAND_LIMIT = 64;

	private static DollSlot slotOf(int slot) {
		return switch (slot) {
			case 0 -> DollSlot.MAIN_HAND;
			case 1 -> DollSlot.OFF_HAND;
			case 2 -> DollSlot.CORE;
			default -> DollSlot.CLOTH;
		};
	}

	private static boolean isHand(DollSlot slot) {
		return slot == DollSlot.MAIN_HAND || slot == DollSlot.OFF_HAND;
	}

	/**
	 * How many of this stack one hand may hold: the item's own maximum when a
	 * binding <b>spends</b> it (ammunition — a thrown hexbrew bottle, a fired laser,
	 * a detonated TNT), otherwise a single item. Read from the menu through
	 * {@code Slot.getMaxStackSize(ItemStack)}, the one hook vanilla asks per item;
	 * every placement path (click, number-key swap, shift-click, drag) sizes its
	 * transfer with it, so nothing is ever over-written and then truncated away.
	 */
	public static int handStackLimit(ItemStack stack) {
		if (stack.isEmpty()) return 1;
		return DollBehaviorRegistry.spendsOnUse(stack) ? stack.getMaxStackSize() : 1;
	}

	@Nullable
	private final DollEntity doll;

	private final ItemStack stack;

	private final MutableDollInventory dummy = new MutableDollInventory();

	public DollLoadoutItemHandler(@Nullable DollEntity doll) {
		this.doll = doll;
		this.stack = ItemStack.EMPTY;
	}

	public DollLoadoutItemHandler(ItemStack stack) {
		this.doll = null;
		this.stack = stack;
	}

	private boolean isLiveDoll() {
		return doll != null && !doll.level().isClientSide();
	}

	private boolean isLiveItem() {
		return !stack.isEmpty();
	}

	private DollInventory itemData() {
		var data = stack.get(GLItems.DOLL_LOADOUT.get());
		return data != null ? data : DollInventory.empty();
	}

	private void saveItem(DollInventory data) {
		if (!stack.isEmpty()) stack.set(GLItems.DOLL_LOADOUT.get(), data);
	}

	private MutableDollInventory inventory() {
		if (isLiveDoll()) return doll.loadout();
		return dummy;
	}

	private void sync() {
		if (isLiveDoll()) doll.syncLoadoutMirror();
	}

	@Override
	public int getSlots() {
		return 4;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
		if (isLiveItem()) return itemData().get(slotOf(slot));
		return inventory().get(slotOf(slot));
	}

	/**
	 * Writes through to the live backing (ledger, item component, or client
	 * dummy) and refreshes the entity mirror for doll backings.
	 */
	private void writeSlot(DollSlot target, ItemStack stack) {
		ItemStack copy = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		if (isLiveItem()) {
			saveItem(itemData().with(target, copy));
			return;
		}
		inventory().set(target, copy);
		sync();
	}

	/**
	 * The loadout lock: a hand takes only what a doll could act with — any bound
	 * item, whatever the action type, plus the vanilla shield
	 * ({@link DollBehaviorRegistry#usableInHand}). Core and cloth take nothing yet
	 * (mechanical cores and cloth items are future work — open their predicate here
	 * when they land).
	 */
	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		if (stack.isEmpty() || slot < 0 || slot >= getSlots()) return false;
		DollSlot target = slotOf(slot);
		if (!isHand(target)) return false;
		return DollBehaviorRegistry.usableInHand(stack);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		ItemStack remainder = stack.copy();
		if (remainder.isEmpty() || !isItemValid(slot, remainder)) return remainder;
		DollSlot target = slotOf(slot);
		ItemStack current = getStackInSlot(slot).copy();
		if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, remainder)) return remainder;
		// Per-item rule, not the slot ceiling: a hand takes one of anything but a
		// whole stack of what it spends.
		int limit = handStackLimit(remainder);
		int move = Math.min(limit - current.getCount(), remainder.getCount());
		if (move <= 0) return remainder;
		if (simulate) {
			remainder.shrink(move);
			return remainder;
		}
		writeSlot(target, remainder.copyWithCount(current.getCount() + move));
		remainder.shrink(move);
		return remainder;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (amount <= 0 || slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
		DollSlot target = slotOf(slot);
		ItemStack held = getStackInSlot(slot).copy();
		if (held.isEmpty()) return ItemStack.EMPTY;
		int n = Math.min(amount, held.getCount());
		if (simulate) return held.copyWithCount(n);
		writeSlot(target, held.copyWithCount(held.getCount() - n));
		return held.copyWithCount(n);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		if (slot < 0 || slot >= getSlots()) return;
		writeSlot(slotOf(slot), stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
	}

	@Override
	public int getSlotLimit(int slot) {
		if (slot < 0 || slot >= getSlots()) return 0;
		DollSlot target = slotOf(slot);
		return isHand(target) ? HAND_LIMIT : 1;
	}

}

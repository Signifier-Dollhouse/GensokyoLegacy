package dev.xkmc.gensokyolegacy.content.entity.dolls.menu;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollInventory;
import dev.xkmc.gensokyolegacy.content.attachment.doll.MutableDollInventory;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.behavior.DollBehaviorRegistry;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollCoreTalisman;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.FoldedPaperTalisman;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

/**
 * 4-slot {@link IItemHandlerModifiable} facade over a doll's loadout, arranged as a
 * cross: slot 0 main hand (left), 1 off hand (right), 2 core (top), 3 cloth (middle,
 * the body-worn slot). A hand is <b>locked</b> to what a doll can actually use — any
 * bound item, plus the vanilla shield — and to a single item per slot, ammunition
 * excepted (loadout.md §4).
 * <p>
 * The two rules live here because this is the only gated writer: {@link #isItemValid}
 * is what a click asks before placing and {@link #slotLimit} is the per-item form of
 * the count rule. Alice's arming is the one other writer and is not gated by these —
 * it only ever hands out doll lances, star wands and folded talismans, all of which
 * admit.
 * <p>
 * Two live backings: the server-side entity menu writes straight into the ledger and
 * refreshes the client mirror afterwards, the server-side item menu reads/writes the
 * doll stack's {@code DOLL_LOADOUT} component, and the client-side menu uses a dummy
 * inventory so contents come purely from container sync. Modifiable because
 * {@code ItemHandlerCopySlot} casts to this interface.
 */
public class DollLoadoutItemHandler implements IItemHandlerModifiable {

	/**
	 * The most one hand can ever hold: a full stack of the largest ammunition — TNT at
	 * 64 (hexbrew bottles cap at 16 by their own item properties). What a hand holds
	 * for any one item is {@link #slotLimit}: {@link #getSlotLimit} cannot see the stack,
	 * so it answers the ceiling and {@link #insertItem} applies the real rule.
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
	 * How many of this stack a loadout slot may hold: the item's own maximum when a
	 * binding <b>spends</b> it, otherwise a single item; core and cloth stay single
	 * whatever the stack would allow. Read from the menu through
	 * {@code Slot.getMaxStackSize(ItemStack)}, the one hook vanilla asks per item, so
	 * every placement path sizes its transfer with it.
	 */
	public static int slotLimit(DollSlot slot, ItemStack stack) {
		if (!isHand(slot) || stack.isEmpty()) return 1;
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
	 * Writes through to the live backing (ledger, item component, or client dummy) and
	 * refreshes the entity mirror for doll backings.
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
	 * The loadout lock, per slot. A hand takes only what a doll could act with
	 * ({@link DollBehaviorRegistry#usableInHand}); the core takes a folded talisman,
	 * which the doll wears rather than uses ({@link DollCoreTalisman}); cloth takes
	 * nothing yet — open its predicate here when the cloth items land.
	 */
	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		if (stack.isEmpty() || slot < 0 || slot >= getSlots()) return false;
		DollSlot target = slotOf(slot);
		if (target == DollSlot.CORE) return stack.getItem() instanceof FoldedPaperTalisman;
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
		// Per-slot rule, not the slot ceiling: a hand takes one of anything but a whole stack
		// of what it spends, and core and cloth always take exactly one.
		int limit = slotLimit(target, remainder);
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

	/**
	 * The same rule per item rather than per slot: {@link #getSlotLimit} cannot see the
	 * stack, so the menu asks here instead — vanilla sizes every transfer with
	 * {@code Slot.getMaxStackSize(ItemStack)}.
	 */
	public int stackLimit(int slot, ItemStack stack) {
		if (slot < 0 || slot >= getSlots()) return 0;
		return slotLimit(slotOf(slot), stack);
	}

}

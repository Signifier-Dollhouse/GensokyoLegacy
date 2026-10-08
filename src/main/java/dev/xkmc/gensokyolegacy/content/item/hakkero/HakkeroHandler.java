package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 17-slot view of a Mini Hakkero's own storage, for {@code ItemHandlerCopySlot}. Slots
 * {@code 0..7} are the inner rim (input), {@code 8..15} the outer rim (results) and
 * {@code 16} the fuel, matching {@link HakkeroData} and the menu sprite.
 *
 * <p>Input and fuel slots answer {@link #isItemValid}: an input slot only takes something
 * the current mode can actually smelt, and the fuel slot only takes something that burns.
 * That is checked against a {@link HakkeroHandler#smeltCheck} supplied by the caller
 * rather than against a world, so the client can ask the same question the server does.
 *
 * <p>Every write goes straight back to the item's data component, so the menu never holds a
 * second copy of the truth.
 */
public class HakkeroHandler implements IItemHandlerModifiable {

	/** Answers "can this be smelted in the current mode", for {@link #isItemValid}. */
	public interface SmeltCheck {
		boolean canSmelt(ItemStack stack);
	}

	private final ItemStack stack;
	private final SmeltCheck check;

	public HakkeroHandler(ItemStack stack, SmeltCheck check) {
		this.stack = stack;
		this.check = check;
	}

	private HakkeroData data() {
		return HakkeroData.of(stack);
	}

	/**
	 * Publishes the record. The client writes here too — its throwaway stack is what the
	 * menu renders from — so the only case that must be skipped is a genuinely empty stack,
	 * where there is nothing to hang the data off.
	 */
	private void save(HakkeroData data) {
		if (stack.isEmpty()) return;
		stack.set(GLItems.DC_HAKKERO_INV, data);
	}

	private static boolean isInput(int slot) {
		return slot < HakkeroData.CHANNELS;
	}

	private static boolean isResult(int slot) {
		return slot >= HakkeroData.CHANNELS && slot < HakkeroData.FUEL;
	}

	@Override
	public int getSlots() {
		return HakkeroData.SLOTS;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot < 0 || slot >= getSlots()) return ItemStack.EMPTY;
		return data().get(slot);
	}

	/**
	 * Writes a slot outright, bypassing every restriction.
	 *
	 * <p>This is the path the <b>container sync</b> uses: the server pushes each slot's
	 * contents to the client, and the client has to accept them exactly as sent. Filtering
	 * here is what made a re-opened menu look empty — the result and fuel slots were
	 * refusing the server's own data on the way in. Placement rules belong in
	 * {@link #insertItem} and {@link #isItemValid}, which is what a player's click goes
	 * through.
	 */
	@Override
	public void setStackInSlot(int slot, ItemStack s) {
		if (slot < 0 || slot >= getSlots()) return;
		save(data().with(slot, s));
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack in, boolean simulate) {
		if (slot < 0 || slot >= getSlots() || in.isEmpty()) return in;
		if (!isItemValid(slot, in)) return in;
		ItemStack current = getStackInSlot(slot);
		if (current.isEmpty()) return simulate ? shrink(in, in.getCount()) : commit(slot, in, in.getCount());
		if (!ItemStack.isSameItemSameComponents(current, in)) return in;
		int room = Math.min(current.getMaxStackSize(), getSlotLimit(slot)) - current.getCount();
		int move = Math.min(room, in.getCount());
		if (move <= 0) return in;
		if (simulate) return shrink(in, move);
		ItemStack grown = current.copyWithCount(current.getCount() + move);
		save(data().with(slot, grown));
		return shrink(in, move);
	}

	private ItemStack commit(int slot, ItemStack in, int count) {
		save(data().with(slot, in.copyWithCount(count)));
		return shrink(in, count);
	}

	private static ItemStack shrink(ItemStack in, int amount) {
		return amount >= in.getCount() ? ItemStack.EMPTY : in.copyWithCount(in.getCount() - amount);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		if (slot < 0 || slot >= getSlots() || amount <= 0) return ItemStack.EMPTY;
		ItemStack current = getStackInSlot(slot);
		if (current.isEmpty()) return ItemStack.EMPTY;
		int take = Math.min(amount, current.getCount());
		if (simulate) return current.copyWithCount(take);
		ItemStack left = current.copyWithCount(current.getCount() - take);
		save(data().with(slot, left));
		return current.copyWithCount(take);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack s) {
		if (s.isEmpty() || slot < 0 || slot >= getSlots()) return false;
		if (isInput(slot)) return check.canSmelt(s);
		if (slot == HakkeroData.FUEL) return Hakkero.burnTime(s) > 0;
		return false;
	}

	/** Convenience for the menu: the whole contents as a fresh list, for tooltips and tests. */
	public List<ItemStack> contents() {
		HakkeroData data = data();
		List<ItemStack> list = new ArrayList<>(getSlots());
		for (int i = 0; i < getSlots(); i++) list.add(data.get(i));
		return list;
	}

	@Nullable
	public ItemStack fuel() {
		return getStackInSlot(HakkeroData.FUEL);
	}

}

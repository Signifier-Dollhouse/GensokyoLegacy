package dev.xkmc.gensokyolegacy.compat.neoforge;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

/**
 * NeoForge's {@code ItemHandlerCopySlot} on top of Forge's {@link SlotItemHandler}. Vanilla mutates the stack
 * returned by {@link #getItem()} in place and reports it through {@link #setChanged()}, so this caches that stack
 * and writes it back then, which is what an immutable item handler needs.
 */
public class ItemHandlerCopySlot extends Slot {

	private static final Container EMPTY = new SimpleContainer(0);

	private final SlotItemHandler delegate;

	private ItemStack cached;

	public ItemHandlerCopySlot(IItemHandler handler, int index, int x, int y) {
		super(EMPTY, 0, x, y);
		this.delegate = new SlotItemHandler(handler, index, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		return delegate.mayPlace(stack);
	}

	@Override
	public ItemStack getItem() {
		return cached = delegate.getItem();
	}

	@Override
	public void set(ItemStack stack) {
		((IItemHandlerModifiable) delegate.getItemHandler()).setStackInSlot(delegate.getSlotIndex(), stack);
		cached = stack;
	}

	@Override
	public void setChanged() {
		if (cached != null) set(cached);
	}

	@Override
	public ItemStack remove(int amount) {
		ItemStack stack = delegate.getItem().copy();
		ItemStack ret = stack.split(amount);
		set(stack);
		cached = null;
		return ret;
	}

	@Override
	public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
		delegate.onQuickCraft(oldStack, newStack);
	}

	@Override
	public int getMaxStackSize() {
		return delegate.getMaxStackSize();
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return delegate.getMaxStackSize(stack);
	}

	@Override
	public boolean mayPickup(Player player) {
		return delegate.mayPickup(player);
	}

	@Override
	public boolean isSameInventory(Slot other) {
		return other instanceof ItemHandlerCopySlot copy && copy.delegate.getItemHandler() == delegate.getItemHandler();
	}

	public IItemHandler getItemHandler() {
		return delegate.getItemHandler();
	}

}

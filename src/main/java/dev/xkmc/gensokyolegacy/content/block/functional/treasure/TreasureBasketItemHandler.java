package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectEntry;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

/**
 * {@link IItemHandlerModifiable} facade over the basket's effect data. The data is looked up on
 * every call, so the handler stays valid while the basket chunk is unloaded.
 *
 * @see "doc/design/treasure_basket.md"
 */
public class TreasureBasketItemHandler implements IItemHandlerModifiable {

	private final ServerLevel level;
	private final BlockPos pos;

	public TreasureBasketItemHandler(ServerLevel level, BlockPos pos) {
		this.level = level;
		this.pos = pos;
	}

	@Nullable
	private TreasureBasketEffectData data() {
		AreaEffectEntry entry = AreaEffectManager.getOwner(level, pos);
		return entry != null && entry.data instanceof TreasureBasketEffectData data ? data : null;
	}

	@Override
	public int getSlots() {
		return TreasureBasketEffectData.SLOTS;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		TreasureBasketEffectData data = data();
		return data == null ? ItemStack.EMPTY : data.getStack(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		TreasureBasketEffectData data = data();
		if (data != null) data.setStack(slot, stack);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		TreasureBasketEffectData data = data();
		if (data == null || stack.isEmpty() || !isItemValid(slot, stack)) return stack;
		ItemStack cur = data.getStack(slot);
		int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
		int space = cur.isEmpty() ? limit
				: ItemStack.isSameItemSameComponents(cur, stack) ? cur.getMaxStackSize() - cur.getCount() : 0;
		int move = Math.min(stack.getCount(), space);
		if (move <= 0) return stack;
		if (!simulate) data.setStack(slot, cur.isEmpty()
				? stack.copyWithCount(move)
				: cur.copyWithCount(cur.getCount() + move));
		return move == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - move);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		TreasureBasketEffectData data = data();
		if (data == null || amount <= 0) return ItemStack.EMPTY;
		ItemStack cur = data.getStack(slot);
		if (cur.isEmpty()) return ItemStack.EMPTY;
		ItemStack out = cur.copyWithCount(Math.min(amount, cur.getCount()));
		if (!simulate) data.setStack(slot, cur.getCount() > out.getCount()
				? cur.copyWithCount(cur.getCount() - out.getCount()) : ItemStack.EMPTY);
		return out;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}

}

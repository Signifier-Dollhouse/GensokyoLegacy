package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerCopySlot;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * 6-row basket menu, laid out like a large chest. The server slots read and write the effect data
 * through {@link TreasureBasketItemHandler}; the client uses a dummy handler that the menu sync
 * fills in slot by slot.
 *
 * @see "doc/design/treasure_basket.md"
 */
public class TreasureBasketMenu extends AbstractContainerMenu {

	public static final int ROWS = 6;
	public static final int CONTAINER_SLOTS = ROWS * 9;

	private final BlockPos pos;
	private final IItemHandler handler;

	public static TreasureBasketMenu fromNetwork(MenuType<?> type, int wid, Inventory inv,
												 @Nullable RegistryFriendlyByteBuf buf) {
		BlockPos pos = buf != null ? buf.readBlockPos() : BlockPos.ZERO;
		return new TreasureBasketMenu(type, wid, inv, pos);
	}

	public TreasureBasketMenu(MenuType<?> type, int wid, Inventory inv, BlockPos pos) {
		super(type, wid);
		this.pos = pos;
		this.handler = resolveHandler(inv.player, pos);
		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new ItemHandlerCopySlot(handler, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}
		int top = (ROWS - 4) * 18;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + top));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inv, col, 8 + col * 18, 161 + top));
		}
	}

	private static IItemHandler resolveHandler(Player player, BlockPos pos) {
		if (player.level() instanceof ServerLevel sl) {
			return player.level().getBlockEntity(pos) instanceof TreasureBasketBlockEntity be
					? be.getItemCap(null) : new TreasureBasketItemHandler(sl, pos);
		}
		return new ItemStackHandler(TreasureBasketEffectData.SLOTS);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack result = stack.copy();
		if (index < CONTAINER_SLOTS) {
			if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, CONTAINER_SLOTS, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
		else slot.setChanged();
		return result;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.level().getBlockState(pos).is(GLBlocks.TREASURE_BASKET.get())
				&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
	}

}

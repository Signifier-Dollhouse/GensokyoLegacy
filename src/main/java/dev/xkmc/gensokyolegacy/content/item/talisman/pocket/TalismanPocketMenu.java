package dev.xkmc.gensokyolegacy.content.item.talisman.pocket;

import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.base.menu.base.BaseContainerMenu;
import dev.xkmc.l2core.base.menu.base.SpriteManager;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerCopySlot;
import org.jetbrains.annotations.Nullable;

public class TalismanPocketMenu extends BaseContainerMenu<TalismanPocketMenu> {

	public static final SpriteManager MANAGER = new SpriteManager(GensokyoLegacy.MODID, "talisman_pocket");

	private final PlayerSlot<?> slot;
	private final TalismanPocketItemHandler handler;

	public static TalismanPocketMenu fromNetwork(MenuType<?> menu, int wid, Inventory inv, @Nullable RegistryFriendlyByteBuf buf) {
		return new TalismanPocketMenu(menu, wid, inv, null);
	}

	public TalismanPocketMenu(@Nullable MenuType<?> type, int wid, Inventory plInv, @Nullable PlayerSlot<?> slot) {
		super(type, wid, plInv, MANAGER, menu -> new SimpleContainer(0), false);
		this.slot = slot;
		ItemStack backing = slot != null ? slot.getItem(plInv.player) : GLTalismans.TALISMAN_POCKET.asStack();
		this.handler = new TalismanPocketItemHandler(backing);
		getLayout().getSlot("talisman", (x, y) -> new ItemHandlerCopySlot(handler, added++, x, y), this::addSlot);
	}

	public TalismanPocketItemHandler getHandler() {
		return handler;
	}

	@Override
	public ItemStack quickMoveStack(Player pl, int id) {
		ItemStack stack = slots.get(id).getItem();
		int n = handler.getSlots();
		boolean moved;
		if (id >= 36) {
			moved = moveItemStackTo(stack, 0, 36, true);
		} else {
			moved = moveItemStackTo(stack, 36, 36 + n, false);
		}
		if (moved) slots.get(id).setChanged();
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return slot != null && slot.getItem(player).getItem() instanceof TalismanPocket;
	}

}
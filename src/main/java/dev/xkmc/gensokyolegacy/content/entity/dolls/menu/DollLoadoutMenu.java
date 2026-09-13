package dev.xkmc.gensokyolegacy.content.entity.dolls.menu;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.base.menu.base.BaseContainerMenu;
import dev.xkmc.l2core.base.menu.base.SpriteManager;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerCopySlot;
import org.jetbrains.annotations.Nullable;

public class DollLoadoutMenu extends BaseContainerMenu<DollLoadoutMenu> {

	public static final SpriteManager MANAGER = new SpriteManager(GensokyoLegacy.MODID, "doll_loadout");

	@Nullable
	private final DollEntity doll;

	@Nullable
	private final PlayerSlot<?> slot;

	private final DollLoadoutItemHandler handler;

	public static DollLoadoutMenu fromNetwork(MenuType<DollLoadoutMenu> type, int wid, Inventory inv,
											 @Nullable RegistryFriendlyByteBuf buf) {
		if (buf != null && buf.isReadable(1) && !buf.readBoolean())
			return new DollLoadoutMenu(type, wid, inv, (PlayerSlot<?>) null);
		Entity entity = buf != null && buf.isReadable(4) ? inv.player.level().getEntity(buf.readInt()) : null;
		return new DollLoadoutMenu(type, wid, inv,
				entity instanceof DollEntity doll ? doll : null);
	}

	public DollLoadoutMenu(@Nullable MenuType<?> type, int wid, Inventory plInv, @Nullable DollEntity doll) {
		super(type, wid, plInv, MANAGER, menu -> new SimpleContainer(0), false);
		this.doll = doll;
		this.slot = null;
		this.handler = new DollLoadoutItemHandler(doll);
		addLoadoutSlots();
	}

	public DollLoadoutMenu(@Nullable MenuType<?> type, int wid, Inventory plInv, @Nullable PlayerSlot<?> slot) {
		super(type, wid, plInv, MANAGER, menu -> new SimpleContainer(0), false);
		this.doll = null;
		this.slot = slot;
		ItemStack backing = slot == null ? ItemStack.EMPTY : slot.getItem(plInv.player);
		this.handler = new DollLoadoutItemHandler(backing);
		addLoadoutSlots();
	}

	private void addLoadoutSlots() {
		getLayout().getSlot("main", (x, y) -> new ItemHandlerCopySlot(handler, added++, x, y), this::addSlot);
		getLayout().getSlot("off", (x, y) -> new ItemHandlerCopySlot(handler, added++, x, y), this::addSlot);
		getLayout().getSlot("core", (x, y) -> new ItemHandlerCopySlot(handler, added++, x, y), this::addSlot);
		getLayout().getSlot("cloth", (x, y) -> new ItemHandlerCopySlot(handler, added++, x, y), this::addSlot);
	}

	public DollLoadoutItemHandler getHandler() {
		return handler;
	}

	@Nullable
	public DollEntity getDoll() {
		return doll;
	}

	public Slot getLoadoutSlot(String name) {
		return getSlot(name, 0, 0);
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
		if (slot != null) return slot.getItem(player).getItem() instanceof DollItem;
		if (doll == null || !player.isAlive() || doll.isRemoved()) return false;
		return doll.isOwner(player) || player.getAbilities().instabuild;
	}

}

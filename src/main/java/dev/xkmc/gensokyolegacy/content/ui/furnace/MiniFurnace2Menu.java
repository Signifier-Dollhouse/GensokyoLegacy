package dev.xkmc.gensokyolegacy.content.ui.furnace;

import dev.xkmc.gensokyolegacy.content.item.tool.MiniFurnace2;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.base.menu.base.BaseContainerMenu;
import dev.xkmc.l2core.base.menu.base.SpriteManager;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.jetbrains.annotations.Nullable;

/**
 * Menu of the finished Mini Hakkero.
 *
 * <p>The grid is a 5x5 lattice: the eight slots on the outer rim ({@code out0}..{@code out7},
 * corners and edge middles) hold the results, the ring around the middle holds the input
 * ({@code in0}..{@code in7}), and the middle slot ({@code core}) is the hakkero itself, drawn
 * as a button that switches between smoker, furnace and blast furnace. Each input slot smelts
 * into the output slot directly across it from the middle, {@link MiniFurnace2#SPEED} times
 * faster than the matching vanilla block.
 *
 * <p>Nothing is stored on the item: the sixteen slots live in this menu and go back to the
 * player when it closes. Only the mode is kept on the item ({@code DC_FURNACE_2}), so the
 * tooltip can report it and it survives reopening; {@link #mode} mirrors it for the client.
 */
public class MiniFurnace2Menu extends BaseContainerMenu<MiniFurnace2Menu> {

	public static final SpriteManager MANAGER = new SpriteManager(GensokyoLegacy.MODID, "mini_hakkero");
	/** Smelting channels: one input and one output slot each. */
	public static final int CHANNELS = 8;
	/** Menu button id of the center mode switch. */
	public static final int MODE_BUTTON = 0;
	/** Progress is synced in permille, which is all the overlay needs and fits a data slot. */
	private static final int PROGRESS_SCALE = 1000;

	@Nullable
	private final PlayerSlot<?> slot;

	private final DataSlot mode;
	private final DataSlot[] progress;

	/** Recipe currently being cooked in each channel, {@code null} when the channel is idle. */
	private final ResourceLocation[] recipes = new ResourceLocation[CHANNELS];
	private final int[] time = new int[CHANNELS];
	private final int[] max = new int[CHANNELS];
	/** Input the recipe lookup was last made for, so an unsmeltable item is not searched for every tick. */
	private final ItemStack[] looked = new ItemStack[CHANNELS];

	public static MiniFurnace2Menu fromNetwork(MenuType<?> type, int wid, Inventory inv, @Nullable RegistryFriendlyByteBuf buf) {
		return new MiniFurnace2Menu(type, wid, inv, null);
	}

	public MiniFurnace2Menu(@Nullable MenuType<?> type, int wid, Inventory plInv, @Nullable PlayerSlot<?> slot) {
		super(type, wid, plInv, MANAGER, menu -> new SimpleContainer(CHANNELS * 2), true);
		this.slot = slot;
		mode = addDataSlot(DataSlot.standalone());
		progress = new DataSlot[CHANNELS];
		for (int i = 0; i < CHANNELS; i++) progress[i] = addDataSlot(DataSlot.standalone());
		addHakkeroSlots();
		if (slot != null) mode.set(MiniFurnace2.getMode(slot.getItem(plInv.player)).ordinal());
	}

	private void addHakkeroSlots() {
		for (int i = 0; i < CHANNELS; i++) {
			final int index = i;
			getLayout().getSlot("in" + i, (x, y) -> new InputSlot(container, index, x, y), this::addSlot);
		}
		for (int i = 0; i < CHANNELS; i++) {
			final int index = i;
			getLayout().getSlot("out" + i, (x, y) -> new ResultSlot(container, CHANNELS + index, x, y), this::addSlot);
		}
		getLayout().getSlot("core", (x, y) -> new ModeSlot(this, x, y), this::addSlot);
	}

	/**
	 * Server side, once per player tick. Advances every channel that has a recipe in the
	 * current mode; channels whose recipe vanished, whose input was swapped, or whose output
	 * no longer fits drop back to idle.
	 */
	public void tick() {
		if (!(player instanceof ServerPlayer sp)) return;
		var rm = sp.serverLevel().getRecipeManager();
		MiniFurnace2.Mode current = getMode();
		for (int i = 0; i < CHANNELS; i++) tickChannel(sp, rm, current, i);
	}

	private void tickChannel(ServerPlayer sp, RecipeManager rm, MiniFurnace2.Mode current, int i) {
		ItemStack in = container.getItem(i);
		if (in.isEmpty()) {
			reset(i);
			return;
		}
		if (recipes[i] == null) {
			if (ItemStack.matches(looked[i], in)) return; // nothing smeltable here, already checked
			var rec = current.getRecipe(sp, new SingleRecipeInput(in));
			if (rec == null) {
				looked[i] = in.copy();
				return;
			}
			recipes[i] = rec.id();
			max[i] = Math.max(1, rec.value().getCookingTime() / MiniFurnace2.SPEED);
			time[i] = 0;
		}
		var found = rm.byKey(recipes[i]);
		if (found.isEmpty() || !(found.get().value() instanceof AbstractCookingRecipe rec)
				|| rec.getType() != current.getType() || !rec.matches(new SingleRecipeInput(in), sp.level())) {
			reset(i);
			return;
		}
		ItemStack out = container.getItem(CHANNELS + i);
		ItemStack result = rec.assemble(new SingleRecipeInput(in), access);
		if (!accepts(out, result)) return; // result slot is blocked or holds something else, stall
		if (time[i] + 1 < max[i]) {
			time[i]++;
		} else {
			if (out.isEmpty()) container.setItem(CHANNELS + i, result);
			else out.grow(result.getCount());
			in.shrink(1);
			if (in.isEmpty()) {
				reset(i);
				return;
			}
			time[i] = 0;
		}
		progress[i].set(time[i] * PROGRESS_SCALE / max[i]);
	}

	private static boolean accepts(ItemStack out, ItemStack result) {
		if (out.isEmpty()) return true;
		if (!ItemStack.isSameItemSameComponents(out, result)) return false;
		return out.getCount() + result.getCount() <= Math.min(out.getMaxStackSize(), result.getMaxStackSize());
	}

	private void reset(int i) {
		recipes[i] = null;
		looked[i] = ItemStack.EMPTY;
		time[i] = 0;
		max[i] = 0;
		progress[i].set(0);
	}

	/**
	 * The mode this menu is working in. It is read from the data slot on both sides: it is
	 * seeded from the item when the menu opens and kept in step whenever the button is used,
	 * so the client never has to reach for the item stack itself.
	 */
	public MiniFurnace2.Mode getMode() {
		var vals = MiniFurnace2.Mode.values();
		return vals[Mth.clamp(mode.get(), 0, vals.length - 1)];
	}

	/**
	 * How far the given channel has progressed, as the fraction the cooldown overlay draws.
	 */
	public float getProgress(int channel) {
		return progress[channel].get() / (float) PROGRESS_SCALE;
	}

	@Override
	public boolean clickMenuButton(Player pl, int id) {
		if (id != MODE_BUTTON) return false;
		if (slot != null && pl instanceof ServerPlayer sp) {
			ItemStack stack = slot.getItem(sp);
			if (stack.getItem() instanceof MiniFurnace2) {
				MiniFurnace2.Mode next = MiniFurnace2.getMode(stack).next();
				MiniFurnace2.setMode(stack, next);
				mode.set(next.ordinal());
			}
		}
		return true;
	}

	/**
	 * Shift-click goes between the player inventory and the input slots only. The result
	 * slots stay reachable by normal clicks and the center button is not a slot for
	 * transfer purposes at all.
	 */
	@Override
	public ItemStack quickMoveStack(Player pl, int id) {
		if (id >= 36 + CHANNELS * 2) return ItemStack.EMPTY;
		ItemStack stack = slots.get(id).getItem();
		if (stack.isEmpty()) return ItemStack.EMPTY;
		boolean moved = id >= 36 + CHANNELS
				? moveItemStackTo(stack, 0, 36, false)
				: moveItemStackTo(stack, 36, 36 + CHANNELS, true);
		if (moved) slots.get(id).setChanged();
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player pl) {
		return slot != null && slot.getItem(pl).getItem() instanceof MiniFurnace2;
	}

	/**
	 * Inner rim slot. Anything goes in; a channel with no recipe in the current mode simply
	 * sits there. Identified by its container index, which is its channel number.
	 */
	public static class InputSlot extends Slot {

		public InputSlot(Container cont, int channel, int x, int y) {
			super(cont, channel, x, y);
		}

	}

	/**
	 * Outer rim slot. Results land here and can be taken out, but nothing can be put in.
	 */
	public static class ResultSlot extends Slot {

		public ResultSlot(Container cont, int index, int x, int y) {
			super(cont, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

	}

	/**
	 * The middle of the grid: the hakkero itself, shown as the mode button. It is backed by
	 * an empty container so nothing can ever be taken from it or put in, and the icon is
	 * rebuilt from the synced mode so the tooltip stays honest on both sides.
	 */
	public static class ModeSlot extends Slot {

		private final MiniFurnace2Menu menu;

		public ModeSlot(MiniFurnace2Menu menu, int x, int y) {
			super(new SimpleContainer(1), 0, x, y);
			this.menu = menu;
		}

		@Override
		public ItemStack getItem() {
			return MiniFurnace2.setMode(GLItems.MINI_FURNACE_2.asStack(), menu.getMode());
		}

		@Override
		public void set(ItemStack stack) {
			// the icon is not a real item, ignore anything a container sync tries to write here
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public boolean allowModification(Player player) {
			return false;
		}

		@Override
		public boolean isFake() {
			return true;
		}

	}

}

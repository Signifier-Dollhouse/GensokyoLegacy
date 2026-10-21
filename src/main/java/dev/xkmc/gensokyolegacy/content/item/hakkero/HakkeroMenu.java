package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.base.menu.base.BaseContainerMenu;
import dev.xkmc.l2core.base.menu.base.SpriteManager;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.neoforged.neoforge.items.ItemHandlerCopySlot;
import org.jetbrains.annotations.Nullable;

/**
 * Menu of the finished Mini Hakkero: a 5x5 lattice whose outer rim ({@code out0}..{@code out7}) holds
 * the results, whose inner ring ({@code in0}..{@code in7}) holds the input, and whose middle slot
 * ({@code core}) is the hakkero itself, drawn as a button cycling smoker, furnace and blast furnace.
 * The single fuel slot sits at the bottom left, on the player inventory's column.
 * <p>
 * Nothing lives here: the seventeen slots and the whole smelting state belong to the item (see
 * {@link HakkeroData}) and are reached through {@link HakkeroHandler}, so what a channel is cooking
 * is a property of the item, not of an open window.
 */
public class HakkeroMenu extends BaseContainerMenu<HakkeroMenu> {

	public static final SpriteManager MANAGER = new SpriteManager(GensokyoLegacy.MODID, "mini_hakkero");
	/**
	 * Menu button id of the center mode switch.
	 */
	public static final int MODE_BUTTON = 0;
	/**
	 * Progress is synced in permille, which is all the overlay needs and fits a data slot.
	 */
	private static final int PROGRESS_SCALE = 1000;

	@Nullable
	private final PlayerSlot<?> slot;

	/**
	 * The stack the contents live in: the player's own hakkero on the server, a throwaway one on
	 * the client, carried across by the ordinary container sync as the talisman pocket does.
	 */
	private final ItemStack backing;
	private final HakkeroHandler handler;

	private final DataSlot mode;
	private final DataSlot[] progress = new DataSlot[HakkeroData.CHANNELS];
	private final DataSlot burnTime;
	private final DataSlot burnMax;
	/**
	 * Client-only eased copy of {@link #progress}, driven by {@link #advanceInterpolation}.
	 */
	private final float[] smooth = new float[HakkeroData.CHANNELS];
	/**
	 * Client-only wall clock of the last interpolation, in milliseconds.
	 */
	private long lastFrame;

	public static HakkeroMenu fromNetwork(MenuType<?> type, int wid, Inventory inv, @Nullable RegistryFriendlyByteBuf buf) {
		return new HakkeroMenu(type, wid, inv, null);
	}

	public HakkeroMenu(@Nullable MenuType<?> type, int wid, Inventory plInv, @Nullable PlayerSlot<?> slot) {
		super(type, wid, plInv, MANAGER, menu -> new SimpleContainer(0), false);
		this.slot = slot;
		this.backing = slot != null ? slot.getItem(plInv.player) : GLItems.MINI_HAKKERO.asStack();
		this.handler = new HakkeroHandler(backing, this::canSmelt);
		mode = addDataSlot(DataSlot.standalone());
		for (int i = 0; i < HakkeroData.CHANNELS; i++) {
			progress[i] = addDataSlot(DataSlot.standalone());
		}
		burnTime = addDataSlot(DataSlot.standalone());
		burnMax = addDataSlot(DataSlot.standalone());
		addHakkeroSlots();
		if (slot != null) {
			mode.set(Hakkero.getMode(backing).ordinal());
			syncState();
		}
	}

	/**
	 * Input slots only accept what the current mode can smelt. The client runs the same
	 * check, so a stack is never offered to a slot the server would then refuse.
	 */
	private boolean canSmelt(ItemStack stack) {
		var level = player.level();
		return getMode().findRecipe(level, stack) != null;
	}

	private void addHakkeroSlots() {
		for (int i = 0; i < HakkeroData.CHANNELS; i++) {
			final int index = i;
			getLayout().getSlot("in" + i, (x, y) -> new InputSlot(handler, index, x, y), this::addSlot);
		}
		for (int i = 0; i < HakkeroData.CHANNELS; i++) {
			final int index = HakkeroData.CHANNELS + i;
			getLayout().getSlot("out" + i, (x, y) -> new ResultSlot(handler, index, x, y), this::addSlot);
		}
		getLayout().getSlot("fuel", (x, y) -> new FuelSlot(handler, HakkeroData.FUEL, x, y), this::addSlot);
		getLayout().getSlot("core", (x, y) -> new ModeSlot(this, x, y), this::addSlot);
	}

	/**
	 * Pushes the item's smelting state into the data slots the client renders from. Progress goes in
	 * permille of the recipe's own cooking time, so the bar reads the same at 1x or 10x.
	 */
	private void syncState() {
		if (backing.isEmpty()) return;
		HakkeroData data = HakkeroData.of(backing);
		HakkeroMode mode = getMode();
		for (int i = 0; i < HakkeroData.CHANNELS; i++) {
			ItemStack in = data.get(i);
			AbstractCookingRecipe rec = in.isEmpty() ? null : mode.findRecipe(player.level(), in);
			float f = rec == null ? 0.0F : data.progress(i, rec.getCookingTime());
			// a data slot is a short on the wire, so the fraction is clamped into permille
			progress[i].set(Math.clamp((int) (f * PROGRESS_SCALE), 0, PROGRESS_SCALE));
		}
		burnTime.set(data.burnTime());
		burnMax.set(data.burnMax());
	}

	@Override
	public void slotsChanged(Container cont) {
		super.slotsChanged(cont);
		if (!player.level().isClientSide()) syncState();
	}

	@Override
	protected void securedServerSlotChange(Container cont) {
		syncState();
	}

	/**
	 * Keeps the overlay and the flame moving while the menu is open. The item's own
	 * {@code inventoryTick} does the actual smelting — this only mirrors it.
	 */
	public void tick() {
		if (player.level().isClientSide()) return;
		syncState();
	}

	/**
	 * The mode this menu is working in, read from the data slot on both sides: seeded from the
	 * item on open and kept in step by the button, so the client never reaches for the stack.
	 */
	public HakkeroMode getMode() {
		var vals = HakkeroMode.values();
		return vals[Mth.clamp(mode.get(), 0, vals.length - 1)];
	}

	/**
	 * How far the given channel has progressed, as the fraction the cooldown overlay draws.
	 */
	public float getProgress(int channel) {
		return progress[channel].get() / (float) PROGRESS_SCALE;
	}

	/**
	 * The same progress, smoothed: the data slot moves one unit of the recipe's cooking time per
	 * tick (half a percent on a 200-tick recipe), so drawing it raw steps the bar twice a second.
	 */
	public float getSmoothProgress(int channel) {
		return smooth[channel];
	}

	/**
	 * Advances the client-side interpolation; a no-op on the server, where the value is exact. Driven
	 * by wall-clock, not the partial tick, which restarts at zero every tick and stutters the ease.
	 * A large jump (a fresh item, a recipe change) snaps so the bar never lags; small per-tick
	 * increments get spread, and the first frame seeds {@link #smooth} instead of easing towards it.
	 */
	public void advanceInterpolation() {
		if (!player.level().isClientSide()) return;
		long now = Util.getMillis();
		if (lastFrame == 0) {
			lastFrame = now;
			for (int i = 0; i < smooth.length; i++) smooth[i] = getProgress(i);
			return;
		}
		float dt = Math.clamp((now - lastFrame) / 50.0F, 0.0F, 4.0F);
		lastFrame = now;
		if (dt <= 0.0F) return;
		for (int i = 0; i < smooth.length; i++) {
			float target = getProgress(i);
			float cur = smooth[i];
			if (target == 0.0F) {
				smooth[i] = 0.0F;
			} else if (Math.abs(target - cur) > 0.1F) {
				smooth[i] = target; // too far to be a step: this is a reset, not progress
			} else {
				// close roughly a fifth of the remaining gap per tick, frame-rate independent
				smooth[i] = cur + (target - cur) * Math.clamp(dt * 0.2F, 0.0F, 1.0F);
			}
		}
	}

	public float getBurnProgress() {
		int max = burnMax.get();
		return max != 0 && burnTime.get() != 0 ? Mth.clamp((float) burnTime.get() / max, 0.0F, 1.0F) : 0.0F;
	}

	@Override
	public boolean clickMenuButton(Player pl, int id) {
		if (id != MODE_BUTTON) return false;
		if (slot != null && pl instanceof ServerPlayer sp) {
			ItemStack stack = slot.getItem(sp);
			if (stack.getItem() instanceof Hakkero) {
				HakkeroMode next = Hakkero.getMode(stack).nextLit();
				Hakkero.setMode(stack, next);
				mode.set(next.ordinal());
				// a recipe change invalidates whatever the channels were cooking
				syncState();
			}
		}
		return true;
	}

	/**
	 * Shift-click only crosses between the player inventory and the menu, never within it: to the
	 * input slots if the mode can smelt the stack, and to the fuel slot if it burns. The range ends
	 * before the result slots, which are not placeable anyway.
	 */
	@Override
	public ItemStack quickMoveStack(Player pl, int id) {
		if (id >= 36 + HakkeroData.SLOTS) return ItemStack.EMPTY;
		ItemStack stack = slots.get(id).getItem();
		if (stack.isEmpty()) return ItemStack.EMPTY;
		boolean moved = id >= 36
				? moveItemStackTo(stack, 0, 36, false)
				: moveItemStackTo(stack, 36, 36 + HakkeroData.CHANNELS, true)
				|| moveItemStackTo(stack, 36 + HakkeroData.FUEL, 37 + HakkeroData.FUEL, false);
		if (moved) slots.get(id).setChanged();
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player pl) {
		return slot != null && slot.getItem(pl).getItem() instanceof Hakkero;
	}

	/**
	 * Inner rim slot: accepts only what the current mode can smelt (the handler decides). The channel
	 * is kept here because {@link ItemHandlerCopySlot} hard-codes container index {@code 0} — read
	 * back off the slot, every one of these would answer zero.
	 */
	public static class InputSlot extends ItemHandlerCopySlot {

		private final int channel;

		public InputSlot(HakkeroHandler handler, int channel, int x, int y) {
			super(handler, channel, x, y);
			this.channel = channel;
		}

		/**
		 * Which channel of the item this slot feeds, and so which bar it draws.
		 */
		public int channel() {
			return channel;
		}

	}

	/**
	 * Outer rim slot: results are taken out, never put in.
	 */
	public static class ResultSlot extends ItemHandlerCopySlot {

		public ResultSlot(HakkeroHandler handler, int index, int x, int y) {
			super(handler, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

	}

	/**
	 * Fuel slot: only accepts something that actually burns.
	 */
	public static class FuelSlot extends ItemHandlerCopySlot {

		public FuelSlot(HakkeroHandler handler, int index, int x, int y) {
			super(handler, index, x, y);
		}

	}

	/**
	 * The middle of the grid: the hakkero itself, shown as the mode button, backed by an empty
	 * container and rebuilt from the synced mode so the tooltip stays honest on both sides.
	 */
	public static class ModeSlot extends Slot {

		private final HakkeroMenu menu;

		public ModeSlot(HakkeroMenu menu, int x, int y) {
			super(new SimpleContainer(1), 0, x, y);
			this.menu = menu;
		}

		@Override
		public ItemStack getItem() {
			return Hakkero.setMode(GLItems.MINI_HAKKERO.asStack(), menu.getMode());
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

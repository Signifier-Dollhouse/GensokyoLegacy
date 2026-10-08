package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.base.menu.base.BaseContainerMenu;
import dev.xkmc.l2core.base.menu.base.SpriteManager;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.Util;
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
 * Menu of the finished Mini Hakkero.
 *
 * <p>The grid is a 5x5 lattice: the eight slots on the outer rim ({@code out0}..{@code out7},
 * corners and edge middles) hold the results, the ring around the middle holds the input
 * ({@code in0}..{@code in7}) and the middle slot ({@code core}) is the hakkero itself, drawn
 * as a button that switches between smoker, furnace and blast furnace. A single fuel slot
 * sits at the bottom left, on the same column as the player inventory.
 *
 * <p>Nothing lives here. The seventeen slots and the whole smelting state belong to the item
 * (see {@link HakkeroData}) and are read and written through
 * {@link HakkeroHandler}; this menu is only a view of them. Because that includes the
 * input slots, what a channel is cooking is a property of the item and not of an open
 * window — the overlay is fed from the item's progress, refreshed on
 * {@link #securedServerSlotChange}.
 */
public class HakkeroMenu extends BaseContainerMenu<HakkeroMenu> {

	public static final SpriteManager MANAGER = new SpriteManager(GensokyoLegacy.MODID, "mini_hakkero");
	/** Menu button id of the center mode switch. */
	public static final int MODE_BUTTON = 0;
	/** Progress is synced in permille, which is all the overlay needs and fits a data slot. */
	private static final int PROGRESS_SCALE = 1000;

	@Nullable
	private final PlayerSlot<?> slot;

	/**
	 * The stack the contents live in: the player's own hakkero on the server, a throwaway one
	 * on the client. Every slot is an {@link ItemHandlerCopySlot} over the handler, so the
	 * ordinary container sync carries the contents across — the client handler writes them
	 * into this stack and the screen reads them back out of it, exactly as the talisman
	 * pocket does.
	 */
	private final ItemStack backing;
	private final HakkeroHandler handler;

	private final DataSlot mode;
	private final DataSlot[] progress = new DataSlot[HakkeroData.CHANNELS];
	private final DataSlot burnTime;
	private final DataSlot burnMax;
	/** Client-only eased copy of {@link #progress}, driven by {@link #advanceInterpolation}. */
	private final float[] smooth = new float[HakkeroData.CHANNELS];
	/** Client-only wall clock of the last interpolation, in milliseconds. */
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
	 * Pushes the item's smelting state into the data slots the client renders from.
	 *
	 * <p>Progress is sent in permille of the recipe's own cooking time — the same units the
	 * item counts in — so the bar reads the same whether the channel is running at 1x or 10x
	 * and only its rate changes. That also means the client can interpolate between updates
	 * instead of stepping: the value it draws chases this one rather than jumping to it.
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
	 * The mode this menu is working in. Read from the data slot on both sides: it is seeded
	 * from the item when the menu opens and kept in step whenever the button is used, so the
	 * client never has to reach for the item stack itself.
	 */
	public HakkeroMode getMode() {
		var vals = HakkeroMode.values();
		return vals[Mth.clamp(mode.get(), 0, vals.length - 1)];
	}

	/** How far the given channel has progressed, as the fraction the cooldown overlay draws. */
	public float getProgress(int channel) {
		return progress[channel].get() / (float) PROGRESS_SCALE;
	}

	/**
	 * The same progress, smoothed. The data slot only moves one step per tick — the channel
	 * gains one unit of the recipe's cooking time per tick, which on a 200-tick recipe is half
	 * a percent — so drawing it raw makes the bar step twice a second. This eases
	 * {@link #smooth} toward {@link #getProgress} once per frame instead.
	 */
	public float getSmoothProgress(int channel) {
		return smooth[channel];
	}

	/**
	 * Advances the client-side interpolation. No-op on the server, where the value is exact.
	 *
	 * <p>Driven by wall-clock rather than the partial tick: the gap it covers spans whole
	 * ticks, and a partial tick restarts at zero every tick, so easing on it would stutter
	 * in step with the tick boundary. A large jump (a fresh item, a recipe change) is snapped
	 * to immediately so the bar never lags behind the truth; the small per-tick increments a
	 * running channel produces are what get spread out.
	 *
	 * <p>The first frame after the screen opens seeds {@link #smooth} from the data slots
	 * instead of easing towards them. The eased copy starts at zero, so easing in would fade
	 * a channel up from nothing — which reads as a missing overlay when the menu is opened
	 * on a hakkero that has been smelting for a while.
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
	 * Shift-click only ever crosses between the player inventory and the menu, never between
	 * the menu's own slots: an item leaving the inventory is offered to the input slots if
	 * the current mode can smelt it and to the fuel slot if it burns, and an item leaving
	 * the menu goes back to the inventory. Ordering matters — the result slots are not
	 * placeable anyway, but relying on that would let a shift-click silently swallow a
	 * stack, so the range ends before them.
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
	 * Inner rim slot: accepts only what the current mode can smelt (the handler decides).
	 *
	 * <p>The channel is kept here rather than read back off the slot, because
	 * {@link ItemHandlerCopySlot} inherits from {@code StackCopySlot}, whose constructor
	 * hard-codes container index {@code 0}. {@code getSlotIndex()} therefore answers zero for
	 * every one of these slots, which would make all eight overlays draw channel 0's
	 * progress — and show nothing at all whenever channel 0 is the empty one.
	 */
	public static class InputSlot extends ItemHandlerCopySlot {

		private final int channel;

		public InputSlot(HakkeroHandler handler, int channel, int x, int y) {
			super(handler, channel, x, y);
			this.channel = channel;
		}

		/** Which channel of the item this slot feeds, and so which bar it draws. */
		public int channel() {
			return channel;
		}

	}

	/** Outer rim slot: results are taken out, never put in. */
	public static class ResultSlot extends ItemHandlerCopySlot {

		public ResultSlot(HakkeroHandler handler, int index, int x, int y) {
			super(handler, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

	}

	/** Fuel slot: only accepts something that actually burns. */
	public static class FuelSlot extends ItemHandlerCopySlot {

		public FuelSlot(HakkeroHandler handler, int index, int x, int y) {
			super(handler, index, x, y);
		}

	}

	/**
	 * The middle of the grid: the hakkero itself, shown as the mode button. It is backed by
	 * an empty container so nothing can ever be taken from it or put in, and the icon is
	 * rebuilt from the synced mode so the tooltip stays honest on both sides.
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

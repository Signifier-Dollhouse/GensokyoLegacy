package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.serialization.marker.OnInject;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Everything the finished Mini Hakkero carries inside itself: its sixteen grid slots, its
 * fuel slot, and the per-channel smelting state. Stored on the item as a data component
 * (see {@code GLItems.DC_HAKKERO_INV}), so it survives moving between inventories, is
 * synced to the client by the ordinary item sync, and is what the tooltip image simulates.
 *
 * <p>The mode is <b>not</b> here — it lives in its own component ({@code DC_HAKKERO_MODE}) so
 * the item tooltip can read it without walking this much larger record.
 *
 * <p>Slot layout, shared with the menu's sprite:
 * <ul>
 *   <li>{@code 0..7} — inner rim, input, one smelting channel each</li>
 *   <li>{@code 8..15} — outer rim, the result of channel {@code slot - 8}</li>
 *   <li>{@code 16} — fuel</li>
 * </ul>
 *
 * <p>Immutable in the data-component sense: every getter hands out copies and every
 * mutation returns a new instance, so a stored {@link ItemStack} is never aliased. The
 * processing loop is the one exception and edits a {@link #workingCopy()} in place, which
 * is why the stored record must never be handed to it. {@link #hashCode()} is computed once
 * and cached, lazily because l2serial fills {@code @SerialField} fields after construction.
 *
 * <p>All three collections are fixed-size arrays. Note two encoding constraints that shaped
 * them: {@code DC_HAKKERO_INV} must stay registered with {@code cache = false}, because
 * l2serial's {@code cacheEncoding()} keys on {@code equals} and would hand back a stale tag
 * for an instance the processing loop then edits; and no field may mix a {@code null}
 * element with a real one, because the array codec puts both in a single NBT list, which has
 * to be homogeneous — hence the empty-string sentinel in {@link #recipes}.
 */
@SerialClass
public class HakkeroData {

	/** Number of smelting channels: one input and one output slot each. */
	public static final int CHANNELS = 8;
	public static final int OUTPUTS = CHANNELS * 2;
	public static final int FUEL = CHANNELS * 2;
	public static final int SLOTS = CHANNELS * 2 + 1;

	@SerialField
	private ItemStack[] slots = defaultStacks();
	/** Cook progress per channel. */
	@SerialField
	private int[] cookTime = new int[CHANNELS];
	/**
	 * Recipe being cooked in each channel. This is a <b>String</b> array, not
	 * {@code ResourceLocation[]}: l2serial's array codec writes every element into one NBT
	 * list, which must be homogeneous, and it encodes an idle channel's {@code null} as a
	 * compound while a real recipe id becomes a string — "Trying to add tag of type 8 to list
	 * of 10" the first time one channel held a recipe and another did not. Empty strings are
	 * the idle value here; see {@link #id(int)} / {@link #recipe(int)}.
	 */
	@SerialField
	private String[] recipes = new String[CHANNELS];
	@SerialField
	private int burnTime;
	@SerialField
	private int burnMax;
	/**
	 * Game time the last batch of processing ended at. {@code inventoryTick} only runs a
	 * batch once this is at least {@link Hakkero#IDLE_BATCH} ticks in the past, and the
	 * client tooltip replays exactly that gap, so an idle hakkero and its tooltip agree.
	 */
	@SerialField
	private long stamp;

	private int hashCode;

	public HakkeroData() {
	}

	public static HakkeroData empty() {
		return new HakkeroData();
	}

	public static HakkeroData of(ItemStack stack) {
		var data = stack.get(GLItems.DC_HAKKERO_INV);
		return data == null ? new HakkeroData() : data;
	}

	@OnInject
	public void onInject() {
		slots = normalizeStacks(slots);
		cookTime = normalizeTimes(cookTime);
		recipes = normalizeRecipes(recipes);
	}

	private static ItemStack[] defaultStacks() {
		ItemStack[] ans = new ItemStack[SLOTS];
		Arrays.fill(ans, ItemStack.EMPTY);
		return ans;
	}

	private static ItemStack[] normalizeStacks(@Nullable ItemStack[] data) {
		ItemStack[] ans = defaultStacks();
		if (data != null) System.arraycopy(data, 0, ans, 0, Math.min(data.length, SLOTS));
		return ans;
	}

	private static int[] normalizeTimes(@Nullable int[] data) {
		int[] ans = new int[CHANNELS];
		if (data != null) System.arraycopy(data, 0, ans, 0, Math.min(data.length, CHANNELS));
		return ans;
	}

	private static String[] normalizeRecipes(@Nullable String[] data) {
		String[] ans = new String[CHANNELS];
		if (data != null) System.arraycopy(data, 0, ans, 0, Math.min(data.length, CHANNELS));
		for (int i = 0; i < CHANNELS; i++) {
			if (ans[i] == null) ans[i] = "";
		}
		return ans;
	}

	private HakkeroData(ItemStack[] slots, int[] cookTime, String[] recipes,
			int burnTime, int burnMax, long stamp) {
		this.slots = slots;
		this.cookTime = cookTime;
		this.recipes = recipes;
		this.burnTime = burnTime;
		this.burnMax = burnMax;
		this.stamp = stamp;
	}

	public ItemStack get(int slot) {
		if (slot < 0 || slot >= SLOTS) return ItemStack.EMPTY;
		ItemStack stack = slots[slot];
		return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
	}

	/** Live view, for the item's own processing. Never handed outside this package. */
	ItemStack raw(int slot) {
		ItemStack stack = slots[slot];
		return stack == null ? ItemStack.EMPTY : stack;
	}

	public HakkeroData with(int slot, ItemStack stack) {
		ItemStack[] next = slots.clone();
		next[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		return new HakkeroData(next, cookTime, recipes, burnTime, burnMax, stamp);
	}

	/**
	 * A private working copy for the processing loop. The record the item holds is shared
	 * with whatever else is reading it — the menu, the tooltip simulation — and the loop
	 * edits in place, so it must never be handed the stored instance.
	 */
	public HakkeroData workingCopy() {
		return copy();
	}

	/** In-place edit of one slot, for processing; the caller re-publishes the whole record. */
	HakkeroData setRaw(int slot, ItemStack stack) {
		slots[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack;
		return touched();
	}

	public int cookTime(int channel) {
		return cookTime[channel];
	}

	/** The recipe id a channel is cooking, or {@code null} when it is idle. */
	@Nullable
	public ResourceLocation recipe(int channel) {
		String id = recipes[channel];
		return id == null || id.isEmpty() ? null : ResourceLocation.tryParse(id);
	}

	public int burnTime() {
		return burnTime;
	}

	public int burnMax() {
		return burnMax;
	}

	public long stamp() {
		return stamp;
	}

	/** How much of the current fuel is left, as the furnace's burn progress. */
	public float burnProgress() {
		return burnMax > 0 ? Hakkero.clamp01((float) burnTime / burnMax) : 0.0F;
	}

	/**
	 * How far a channel has cooked, as the fraction the cooldown overlay draws.
	 *
	 * @param recipeTime cooking time of the recipe being cooked, or 0 when the channel is
	 *                   idle or its recipe is gone
	 */
	public float progress(int channel, int recipeTime) {
		return recipeTime <= 0 ? 0.0F : Hakkero.clamp01((float) cookTime(channel) / recipeTime);
	}

	/**
	 * In-place channel edit, for the processing loop — which touches every channel every
	 * tick and cannot afford to rebuild the lists per step. Only ever called on a
	 * {@link #copy()} the caller then publishes, never on the record the item holds.
	 */
	HakkeroData setCook(int channel, int time, @Nullable ResourceLocation recipe) {
		cookTime[channel] = time;
		recipes[channel] = recipe == null ? "" : recipe.toString();
		return touched();
	}

	/** In-place fuel edit; see {@link #setCook}. */
	HakkeroData setFuel(int burnTime, int burnMax) {
		this.burnTime = burnTime;
		this.burnMax = burnMax;
		return touched();
	}

	public HakkeroData withStamp(long stamp) {
		this.stamp = stamp;
		return touched();
	}

	private HakkeroData touched() {
		hashCode = 0;
		return this;
	}

	/**
	 * True when any channel has something it could cook right now, which is what keeps a
	 * fuel lit. "Could" is deliberate: a channel whose output slot is already full does not
	 * count, or a full grid would quietly eat every coal dropped into it.
	 */
	/** True when nothing at all is stored, so a tooltip image would be pointless. */
	public boolean isEmpty() {
		for (ItemStack stack : slots) {
			if (stack != null && !stack.isEmpty()) return false;
		}
		return true;
	}

	public boolean hasWork(HakkeroMode mode, Level level) {
		for (int i = 0; i < CHANNELS; i++) {
			ItemStack in = raw(i);
			if (in.isEmpty()) continue;
			var recipe = mode.findRecipe(level, in);
			if (recipe == null) continue;
			// the result has to actually fit, or a full output slot would keep a fuel
			// burning forever while nothing at all is being cooked
			if (Hakkero.accepts(raw(CHANNELS + i), recipe.getResultItem(level.registryAccess()))) return true;
		}
		return false;
	}

	public HakkeroData copy() {
		ItemStack[] next = new ItemStack[SLOTS];
		for (int i = 0; i < SLOTS; i++) {
			ItemStack stack = slots[i];
			next[i] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		}
		return new HakkeroData(next, cookTime.clone(), recipes.clone(), burnTime, burnMax, stamp);
	}

	@Override
	public int hashCode() {
		int h = hashCode;
		if (h == 0) {
			h = Arrays.hashCode(slots);
			h = 31 * h + Arrays.hashCode(cookTime);
			h = 31 * h + Arrays.hashCode(recipes);
			h = 31 * h + burnTime;
			h = 31 * h + burnMax;
			h = 31 * h + Long.hashCode(stamp);
			hashCode = h == 0 ? 1 : h;
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj instanceof HakkeroData other && hashCode() == other.hashCode()) {
			return Arrays.equals(slots, other.slots)
					&& Arrays.equals(cookTime, other.cookTime)
					&& Arrays.equals(recipes, other.recipes)
					&& burnTime == other.burnTime
					&& burnMax == other.burnMax
					&& stamp == other.stamp;
		}
		return false;
	}

}

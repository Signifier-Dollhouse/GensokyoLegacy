package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.gensokyolegacy.content.client.deco.ClientTooltip;
import dev.xkmc.gensokyolegacy.content.entity.broom.BroomEntity;
import dev.xkmc.gensokyolegacy.content.item.common.InvClickItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2core.util.Proxy;
import dev.xkmc.l2menustacker.init.L2MenuStacker;
import dev.xkmc.l2menustacker.screen.packets.CacheMouseToClient;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The finished Mini Hakkero, a sealed {@link HakkeroPrototype}: that one leaked heat into the
 * surrounding inventory, this one carries its own grid. All state lives on the item
 * ({@code DC_HAKKERO_INV}), so smelting runs from {@link #inventoryTick} rather than an open menu —
 * see there for the batching scheme. A burning fuel pushes it to {@link #MAX_SPEED}x, consumed
 * {@link #FUEL_DIVISOR} times faster than in a block furnace; with a broom in the offhand it is a
 * flight booster instead — see {@link #use(Level, Player, InteractionHand)}.
 */
public class Hakkero extends Item implements InvClickItem {

	/**
	 * Cooking time divisor with a lit fuel: {@code 200 / 10}, i.e. ten times a vanilla furnace.
	 */
	public static final int MAX_SPEED = 10;
	/**
	 * A fuel burns this many times faster than it would in a block furnace.
	 */
	public static final int FUEL_DIVISOR = 10;
	/**
	 * How often the item does a catch-up batch while nobody is looking at it — also the
	 * granularity its client tooltip replays at.
	 */
	public static final int IDLE_BATCH = 20;
	/**
	 * Thrust given by a kick paid for out of the fuel slot.
	 */
	public static final float THRUST_FUELLED = 3.0F;

	public Hakkero(Properties properties) {
		super(properties.stacksTo(1));
	}

	public static HakkeroMode getMode(ItemStack stack) {
		HakkeroMode mode = GLItems.DC_HAKKERO_MODE.getOrDefault(stack, HakkeroMode.FURNACE);
		// OFF belongs to the prototype. The component's codec takes the whole enum, so hand-written
		// data or a command could still name it; a sealed firebox has nothing to switch off.
		return mode == HakkeroMode.OFF ? HakkeroMode.FURNACE : mode;
	}

	public static ItemStack setMode(ItemStack stack, HakkeroMode mode) {
		mode = mode == HakkeroMode.OFF ? HakkeroMode.FURNACE : mode;
		stack.set(GLItems.DC_HAKKERO_MODE, mode);
		return stack;
	}

	public static float clamp01(float f) {
		return Math.clamp(f, 0.0F, 1.0F);
	}

	public static int burnTime(ItemStack fuel) {
		return fuel.isEmpty() ? 0 : fuel.getBurnTime(null);
	}

	// ------------------------------------------------------------------ processing

	/**
	 * The single place smelting happens, open menu or not. Stamped at the last batch: while the menu
	 * is open this runs a tick at a time so the GUI looks live, left alone it waits {@link #IDLE_BATCH}
	 * and replays them at once, at no per-tick cost in a pack.
	 */
	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
		if (!(entity instanceof ServerPlayer sp)) return;
		long now = level.getGameTime();
		HakkeroData data = HakkeroData.of(stack);
		int elapsed = (int) Math.max(0, now - data.stamp());
		if (elapsed <= 0) return;
		boolean watching = sp.containerMenu instanceof HakkeroMenu;
		// closed: wait for a whole batch, then do all of it. open: keep up tick by tick.
		if (!watching && elapsed < IDLE_BATCH) return;
		process(stack, level, elapsed);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return false;
	}

	/**
	 * Runs {@code ticks} ticks of smelting on the item in one go and stamps it. The whole batch
	 * is written back once at the end, so a 20-tick catch-up is one data component write.
	 */
	public static void process(ItemStack stack, Level level, int ticks) {
		HakkeroMode mode = getMode(stack);
		// a working copy: the loop edits in place, and the record the item holds is shared
		// with the menu and with any tooltip being built at the same moment
		HakkeroData data = HakkeroData.of(stack).workingCopy();
		RecipeManager rm = level.getRecipeManager();
		boolean changed = false;
		for (int t = 0; t < ticks; t++) changed |= tickChannel(data, level, rm, mode);
		// the stamp is always written, even on a tick where nothing cooked: it is what stops
		// an idle hakkero from re-simulating a fresh batch on every client tooltip
		data = data.withStamp(level.getGameTime());
		if (changed || ticks > 0) stack.set(GLItems.DC_HAKKERO_INV, data);
	}

	/**
	 * One tick of one channel. Fuel first, so a channel that starts work this tick already
	 * gets the boosted rate; returns whether anything moved.
	 */
	private static boolean tickChannel(HakkeroData data, Level level, RecipeManager rm, HakkeroMode mode) {
		boolean changed = false;
		boolean cooking = data.hasWork(mode, level);
		// A fuel burns only while something is cooked, so an idle hakkero never eats one. Checked
		// before and after the channels, because a channel with a full output does not count.
		if (data.burnTime() > 0) {
			if (cooking) {
				data.setFuel(data.burnTime() - FUEL_DIVISOR, data.burnMax());
				changed = true;
			}
		} else if (cooking) {
			ItemStack fuel = data.raw(HakkeroData.FUEL);
			int time = burnTime(fuel);
			if (time > 0) {
				fuel.shrink(1);
				if (fuel.isEmpty()) data.setRaw(HakkeroData.FUEL, ItemStack.EMPTY);
				data.setFuel(time, time);
				changed = true;
			}
		}
		int speed = data.burnTime() > 0 ? MAX_SPEED : 1;
		for (int i = 0; i < HakkeroData.CHANNELS; i++) {
			ItemStack in = data.raw(i);
			if (in.isEmpty()) {
				if (data.cookTime(i) != 0 || data.recipe(i) != null) {
					data.setCook(i, 0, null);
					changed = true;
				}
				continue;
			}
			// Look the recipe up once per channel and remember it by id, or the stored id
			// stays null and the channel resets every tick and never cooks anything.
			AbstractCookingRecipe rec = null;
			if (data.recipe(i) != null) {
				var found = rm.byKey(data.recipe(i));
				if (found.isPresent() && found.get().value() instanceof AbstractCookingRecipe held) rec = held;
			}
			if (rec == null || rec.getType() != mode.getType() || !rec.matches(new SingleRecipeInput(in), level)) {
				var lookup = mode.findRecipeHolder(level, in);
				if (lookup == null) {
					data.setCook(i, 0, null);
					continue;
				}
				// a fresh recipe restarts the channel, the way a furnace drops its progress
				// when the input under it changes
				if (!lookup.id().equals(data.recipe(i))) data.setCook(i, 0, lookup.id());
				rec = lookup.value();
			}
			ItemStack result = rec.assemble(new SingleRecipeInput(in), level.registryAccess());
			if (!accepts(data.raw(HakkeroData.CHANNELS + i), result)) continue;
			// Progress counts in recipe-time units and speed is the rate (blast furnace arrangement).
			// Scaling the target by speed too made it non-monotonic: 100/200 plus fuel finished at once.
			int done = data.cookTime(i) + speed;
			if (done < rec.getCookingTime()) {
				data.setCook(i, done, data.recipe(i));
				changed = true;
				continue;
			}
			int out = HakkeroData.CHANNELS + i;
			if (data.raw(out).isEmpty()) data.setRaw(out, result);
			else data.raw(out).grow(result.getCount());
			in.shrink(1);
			data.setCook(i, 0, in.isEmpty() ? null : data.recipe(i));
			changed = true;
		}
		return changed;
	}

	/**
	 * Whether {@code out} could take one more {@code result}, or is empty and waiting.
	 */
	public static boolean accepts(ItemStack out, ItemStack result) {
		if (out.isEmpty()) return true;
		if (!ItemStack.isSameItemSameComponents(out, result)) return false;
		return out.getCount() + result.getCount() <= Math.min(out.getMaxStackSize(), result.getMaxStackSize());
	}

	// ------------------------------------------------------------------ interaction

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		// riding a broom with a broom in the other hand: this is a flight booster, not a menu
		if (hand == InteractionHand.MAIN_HAND && player.getVehicle() instanceof BroomEntity broom
				&& player.getOffhandItem().is(GLItems.BROOM.get())) {
			return boost(stack, broom, player, level);
		}
		if (!level.isClientSide() && player instanceof ServerPlayer sp) {
			PlayerSlot<?> slot = heldSlot(sp, stack);
			if (slot != null) HakkeroProvider.open(sp, slot);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	/**
	 * Kicks the broom, spending one item out of the fuel slot: {@link #THRUST_FUELLED} for a
	 * {@link #FUEL_DIVISOR}th of that item's burn time, no kick at all from an empty slot, and a
	 * cooldown as long as the thrust lasts. The tick count comes from the item just consumed, never
	 * {@link HakkeroData#burnTime()} — the lit smelting fuel is shared with the smelting timer (so
	 * kicking off it made a boost free while smelting) and frozen while idle (so an idle hakkero
	 * could kick on the same value forever).
	 */
	private InteractionResultHolder<ItemStack> boost(ItemStack stack, BroomEntity broom, Player player, Level level) {
		int time = burnTime(HakkeroData.of(stack).raw(HakkeroData.FUEL));
		// no fuel, no kick: an empty slot falls through to consume and never reaches the
		// broom at all. Checked on both sides, so the client shows no swing either.
		if (time <= 0) return InteractionResultHolder.consume(stack);
		if (!level.isClientSide && player instanceof ServerPlayer sp) {
			// a hakkero already cooling down from an earlier kick cannot be kicked again
			if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
			// a working copy: the stored record is shared with the menu and the tooltip
			HakkeroData data = HakkeroData.of(stack).workingCopy();
			ItemStack fuel = data.raw(HakkeroData.FUEL);
			fuel.shrink(1);
			if (fuel.isEmpty()) data.setRaw(HakkeroData.FUEL, ItemStack.EMPTY);
			stack.set(GLItems.DC_HAKKERO_INV, data);
			int ticks = Math.max(1, time / FUEL_DIVISOR);
			broom.boost(THRUST_FUELLED, ticks);
			GensokyoLegacy.HANDLER.toClientPlayer(new BroomBoostToClient(broom.getId(), THRUST_FUELLED, ticks), sp);
			player.getCooldowns().addCooldown(this, ticks);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	/**
	 * Opens the menu from an inventory right-click; the cached mouse position centres the
	 * window on the cursor, so without it the first click lands wherever the pointer was.
	 */
	@Override
	public void handleClick(ServerPlayer sp, PlayerSlot<?> slot) {
		L2MenuStacker.PACKET_HANDLER.toClientPlayer(new CacheMouseToClient(), sp);
		HakkeroProvider.open(sp, slot);
	}

	/**
	 * Which inventory slot holds the hakkero, so the open menu can point back at it and know
	 * when the player moved or dropped it. Only the hotbar and the offhand are reachable by hand.
	 */
	@Nullable
	private static PlayerSlot<?> heldSlot(ServerPlayer sp, ItemStack held) {
		if (sp.getInventory().offhand.getFirst() == held)
			return PlayerSlot.ofInventory(sp.getInventory().getContainerSize() - 1);
		for (int i = 0; i < 9; i++) {
			if (sp.getInventory().items.get(i) == held) return PlayerSlot.ofInventory(i);
		}
		return null;
	}

	// ------------------------------------------------------------------ tooltip

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.FURNACE_2_LORE.get());
		list.add(GLLang.ItemFurnace.FURNACE_2_USE.get());
		list.add(GLLang.ItemFurnace.FURNACE_1_DESC.get(
				Component.translatable(getMode(stack).block().getDescriptionId()).withStyle(ChatFormatting.WHITE)));
		list.add(GLLang.ItemFurnace.FURNACE_2_FUEL.get());
		list.add(GLLang.ItemFurnace.FURNACE_2_BOOST.get());
	}

	/**
	 * Contents as seen <i>now</i>: the gap between the recorded stamp and a smooth client clock is
	 * replayed onto a copy, so the image changes every frame, not once a second.
	 */
	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		// shift-holding is read through the client-only helper; the method itself is only
		// ever reached from a client tooltip, so the guard is just to keep it honest
		if (ClientTooltip.isShiftDown()) return Optional.empty();
		// Proxy rather than Minecraft directly: this method is reached from common code, and
		// naming the client class here would make the server fail to load this item
		Level level = Proxy.getLevel();
		if (level == null) return Optional.empty();
		HakkeroData data = HakkeroData.of(stack);
		if (data.isEmpty()) return Optional.empty();
		double now = ClientTooltip.smoothGameTime(level);
		if (!Double.isNaN(now)) data = simulate(stack, level, now - data.stamp());
		HakkeroMode mode = getMode(stack);
		// three rows of eight: the eight input slots, the eight results, then the fuel
		List<ItemStack> list = new ArrayList<>(HakkeroData.SLOTS);
		for (int i = 0; i < HakkeroData.SLOTS; i++) list.add(data.get(i));
		int[] progress = new int[HakkeroData.CHANNELS];
		for (int i = 0; i < HakkeroData.CHANNELS; i++) {
			ItemStack in = data.get(i);
			var recipe = in.isEmpty() ? null : mode.findRecipe(level, in);
			// permille of the recipe's own cooking time, the units the item counts in
			float f = recipe == null ? 0.0F : data.progress(i, recipe.getCookingTime());
			progress[i] = Math.clamp((int) (f * HakkeroTooltip.SCALE), 0, HakkeroTooltip.SCALE);
		}
		return Optional.of(new HakkeroTooltip(list, progress));
	}

	/**
	 * Replays {@code ticks} ticks on a copy through the same {@link #tickChannel}, whole ones first
	 * and the remainder as a fraction of one more — that is what advances the result every frame.
	 */
	public static HakkeroData simulate(ItemStack stack, Level level, double ticks) {
		HakkeroData sim = HakkeroData.of(stack).copy();
		if (ticks <= 0) return sim;
		RecipeManager rm = level.getRecipeManager();
		HakkeroMode mode = getMode(stack);
		int whole = (int) ticks;
		for (int t = 0; t < whole; t++) tickChannel(sim, level, rm, mode);
		double rest = ticks - whole;
		if (rest > 0) partialTick(sim, level, mode, rest);
		return sim;
	}

	/**
	 * Advances progress by a fraction of one tick: already-cooking channels and burning fuel get
	 * their share of a step. Nothing else changes — a channel cannot start or finish mid-tick.
	 */
	private static void partialTick(HakkeroData sim, Level level, HakkeroMode mode, double rest) {
		int speed = sim.burnTime() > 0 ? MAX_SPEED : 1;
		for (int i = 0; i < HakkeroData.CHANNELS; i++) {
			if (sim.recipe(i) == null || sim.cookTime(i) <= 0) continue;
			ItemStack in = sim.raw(i);
			if (in.isEmpty()) continue;
			var rec = mode.findRecipe(level, in);
			if (rec == null) continue;
			// the same gate the full tick uses, so a blocked result slot still holds its bar
			if (!accepts(sim.raw(HakkeroData.CHANNELS + i), rec.getResultItem(level.registryAccess()))) continue;
			int step = Math.max(1, (int) Math.round(speed * rest));
			sim.setCook(i, Math.min(rec.getCookingTime(), sim.cookTime(i) + step), sim.recipe(i));
		}
		if (sim.burnTime() > 0) {
			sim.setFuel(Math.max(0, sim.burnTime() - (int) Math.ceil(FUEL_DIVISOR * rest)), sim.burnMax());
		}
	}

	/**
	 * Client-side mirror of a {@link #boost} kick, sent because the broom flies client side.
	 */
	public record BroomBoostToClient(int broomId, float power,
									 int ticks) implements SerialPacketBase<BroomBoostToClient> {

		@Override
		public void handle(Player player) {
			if (player.level().getEntity(broomId) instanceof BroomEntity broom) broom.boost(power, ticks);
		}

	}

}

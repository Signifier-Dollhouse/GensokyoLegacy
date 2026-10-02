package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.l2itemselector.select.item.IItemSelector;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import dev.xkmc.gensokyolegacy.content.item.dagger.client.DaggerGloveModeWheel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Selector listener for the {@link DaggerGloveItem}'s firing mode, mirroring
 * {@code DollGloveSelectionListener} (glove.md §3).
 * <p>
 * Simpler than the doll glove's because all three modes are always available: there is no
 * availability filter, no client/server split over what is visible, and no hidden modes to keep
 * out of the wheel. The mode is the enum ordinal in a data component, so both the wheel and the
 * scroll selector speak ordinals and neither can name a mode that does not exist.
 */
public class DaggerGloveSelectionListener extends IItemSelector implements WheelAdaptor.Provider {

	public static final DaggerGloveSelectionListener INSTANCE = new DaggerGloveSelectionListener(GensokyoLegacy.loc("dagger_glove"));

	public DaggerGloveSelectionListener(ResourceLocation id) {
		super(id);
	}

	public static void register() {
		IItemSelector.register(INSTANCE);
	}

	@Nullable
	public static ItemStack getHeldGlove(Player player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof DaggerGloveItem) return main;
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof DaggerGloveItem) return off;
		return null;
	}

	@Override
	public boolean test(ItemStack stack) {
		return stack.getItem() instanceof DaggerGloveItem;
	}

	@Override
	public int getIndex(Player player, ItemStack stack) {
		return DaggerGloveItem.getMode(stack).ordinal();
	}

	@Override
	public List<ItemStack> getList(ItemStack stack) {
		List<ItemStack> list = new ArrayList<>();
		for (var mode : DaggerGloveMode.values()) {
			ItemStack icon = DaggerGloveItem.displayStack(mode);
			icon.set(DataComponents.ITEM_NAME, mode.displayName());
			list.add(icon);
		}
		return list;
	}

	/**
	 * Every mode is always listed, so the scroll list never changes shape and its hash never has to
	 * be part of the cache key.
	 */
	@Override
	public int getSelHash(ItemStack stack) {
		return DaggerGloveMode.values().length;
	}

	/** Scroll selection steps through every mode, wrapping; returns the new mode's ordinal. */
	@Override
	public int move(int i, Player player, ItemStack stack) {
		var modes = DaggerGloveMode.values();
		return Math.floorMod(DaggerGloveItem.getMode(stack).ordinal() + i, modes.length);
	}

	@Override
	public void swap(Player sender, int index, ItemStack stack) {
		ItemStack held = getHeldGlove(sender);
		if (held == null) return;
		DaggerGloveItem.setMode(held, byOrdinal(index));
	}

	@Override
	public Optional<WheelAdaptor<?>> get(@Nullable Player player, int wheelIndex, boolean main) {
		if (player == null) return Optional.empty();
		ItemStack stack = getHeldGlove(player);
		if (stack == null) return Optional.empty();
		if (wheelIndex == 0) return Optional.of(new DaggerGloveModeWheel(stack));
		return Optional.empty();
	}

	/** The mode at this ordinal, or the glove's default if the ordinal names no mode. */
	private static DaggerGloveMode byOrdinal(int index) {
		var modes = DaggerGloveMode.values();
		return index < 0 || index >= modes.length ? DaggerGloveMode.SINGLE : modes[index];
	}

	}
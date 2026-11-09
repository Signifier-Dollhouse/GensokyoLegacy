package dev.xkmc.gensokyolegacy.content.item.dagger;

import dev.xkmc.gensokyolegacy.content.item.dagger.client.DaggerGloveModeWheel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2itemselector.select.item.IItemSelector;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Selector listener for the {@link DaggerGloveItem}'s firing mode, mirroring {@code DollGloveSelectionListener}
 * (glove.md §3); all three modes are always available and stored as enum ordinals, so there is no availability
 * filter and no client/server split.
 *
 * @see "doc/design/dagger_glove.md"
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
	public int getIndex(Player player) {
		ItemStack held = getHeldGlove(player);
		return held == null ? 0 : DaggerGloveItem.getMode(held).ordinal();
	}

	@Override
	public List<ItemStack> getList() {
		List<ItemStack> list = new ArrayList<>();
		for (var mode : DaggerGloveMode.values()) {
			// the flat icon variant, not the held separate-transforms model: the sidebar is
			// gui-only, so it wants the mode's own icon like the wheel does
			ItemStack icon = DaggerGloveItem.iconStack(mode);
			icon.setHoverName(mode.displayName());
			list.add(icon);
		}
		return list;
	}

	@Override
	public void swap(Player sender, int index) {
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

	/**
	 * The mode at this ordinal, or the glove's default if the ordinal names no mode.
	 */
	private static DaggerGloveMode byOrdinal(int index) {
		var modes = DaggerGloveMode.values();
		return index < 0 || index >= modes.length ? DaggerGloveMode.SINGLE : modes[index];
	}

}

package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.content.item.debug.client.InteriorWandModeWheel;
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
 * Selector listener for the {@link InteriorWandItem}'s mode; every mode is always listed, so the
 * list never changes shape.
 *
 * @see "doc/design/structure/interior_wand.md"
 */
public class InteriorWandSelectionListener extends IItemSelector implements WheelAdaptor.Provider {

	public static final InteriorWandSelectionListener INSTANCE =
			new InteriorWandSelectionListener(GensokyoLegacy.loc("interior_wand"));

	public InteriorWandSelectionListener(ResourceLocation id) {
		super(id);
	}

	public static void register() {
		IItemSelector.register(INSTANCE);
	}

	@Nullable
	public static ItemStack getHeldWand(Player player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof InteriorWandItem) return main;
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof InteriorWandItem) return off;
		return null;
	}

	@Override
	public boolean test(ItemStack stack) {
		return stack.getItem() instanceof InteriorWandItem;
	}

	@Override
	public int getIndex(Player player, ItemStack stack) {
		return InteriorWandItem.getMode(stack).ordinal();
	}

	@Override
	public List<ItemStack> getList(ItemStack stack) {
		List<ItemStack> list = new ArrayList<>();
		for (InteriorWandMode mode : InteriorWandMode.values()) {
			list.add(InteriorWandItem.iconStack(mode));
		}
		return list;
	}

	@Override
	public int getSelHash(ItemStack stack) {
		return InteriorWandMode.values().length;
	}

	@Override
	public int move(int i, Player player, ItemStack stack) {
		var modes = InteriorWandMode.values();
		return Math.floorMod(InteriorWandItem.getMode(stack).ordinal() + i, modes.length);
	}

	@Override
	public void swap(Player sender, int index, ItemStack stack) {
		ItemStack held = getHeldWand(sender);
		if (held == null) return;
		InteriorWandItem.setMode(held, byOrdinal(index));
	}

	@Override
	public Optional<WheelAdaptor<?>> get(@Nullable Player player, int wheelIndex, boolean main) {
		if (player == null) return Optional.empty();
		ItemStack stack = getHeldWand(player);
		if (stack == null) return Optional.empty();
		if (wheelIndex == 0) return Optional.of(new InteriorWandModeWheel(stack));
		return Optional.empty();
	}

	/**
	 * The mode at this ordinal, or the wand's default if the ordinal names no mode.
	 */
	private static InteriorWandMode byOrdinal(int index) {
		var modes = InteriorWandMode.values();
		return index < 0 || index >= modes.length ? InteriorWandMode.BOUND : modes[index];
	}

}

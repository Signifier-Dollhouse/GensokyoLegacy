package dev.xkmc.gensokyolegacy.content.item.devil;

import dev.xkmc.gensokyolegacy.content.item.devil.client.DevilScrollModeWheel;
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
 * Selector listener for the scroll's mode, mirroring the anchor's. Every mode is always listed.
 *
 * @see "doc/design/item/devils_scroll.md"
 */
public class DevilScrollSelectionListener extends IItemSelector implements WheelAdaptor.Provider {

	public static final DevilScrollSelectionListener INSTANCE =
			new DevilScrollSelectionListener(GensokyoLegacy.loc("devils_scroll"));

	public DevilScrollSelectionListener(ResourceLocation id) {
		super(id);
	}

	public static void register() {
		IItemSelector.register(INSTANCE);
	}

	@Nullable
	public static ItemStack getHeldScroll(Player player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof DevilScrollItem) return main;
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof DevilScrollItem) return off;
		return null;
	}

	@Override
	public boolean test(ItemStack stack) {
		return stack.getItem() instanceof DevilScrollItem;
	}

	@Override
	public int getIndex(Player player, ItemStack stack) {
		return DevilScrollItem.getMode(stack).ordinal();
	}

	@Override
	public List<ItemStack> getList(ItemStack stack) {
		List<ItemStack> list = new ArrayList<>();
		for (DevilScrollMode mode : DevilScrollMode.values()) {
			list.add(mode.icon());
		}
		return list;
	}

	@Override
	public int getSelHash(ItemStack stack) {
		return DevilScrollMode.values().length;
	}

	@Override
	public int move(int i, Player player, ItemStack stack) {
		var modes = DevilScrollMode.values();
		return Math.floorMod(DevilScrollItem.getMode(stack).ordinal() + i, modes.length);
	}

	@Override
	public void swap(Player sender, int index, ItemStack stack) {
		ItemStack held = getHeldScroll(sender);
		if (held == null) return;
		DevilScrollItem.setMode(held, DevilScrollMode.byId(index));
	}

	@Override
	public Optional<WheelAdaptor<?>> get(@Nullable Player player, int wheelIndex, boolean main) {
		if (player == null) return Optional.empty();
		ItemStack stack = getHeldScroll(player);
		if (stack == null) return Optional.empty();
		if (wheelIndex == 0) return Optional.of(new DevilScrollModeWheel(stack));
		return Optional.empty();
	}

}

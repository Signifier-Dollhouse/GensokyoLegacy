package dev.xkmc.gensokyolegacy.content.item.anchor;

import dev.xkmc.gensokyolegacy.content.item.anchor.client.AnchorModeWheel;
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
 * Selector listener for the anchor's mode, mirroring the fan's. Every mode is always listed, so the
 * list never changes shape.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorSelectionListener extends IItemSelector implements WheelAdaptor.Provider {

	public static final AnchorSelectionListener INSTANCE =
			new AnchorSelectionListener(GensokyoLegacy.loc("minamitsu_anchor"));

	public AnchorSelectionListener(ResourceLocation id) {
		super(id);
	}

	public static void register() {
		IItemSelector.register(INSTANCE);
	}

	@Nullable
	public static ItemStack getHeldAnchor(Player player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof AnchorItem) return main;
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof AnchorItem) return off;
		return null;
	}

	@Override
	public boolean test(ItemStack stack) {
		return stack.getItem() instanceof AnchorItem;
	}

	@Override
	public int getIndex(Player player) {
		ItemStack held = getHeldAnchor(player);
		return held == null ? 0 : AnchorItem.getMode(held).ordinal();
	}

	@Override
	public List<ItemStack> getList() {
		List<ItemStack> list = new ArrayList<>();
		for (AnchorMode mode : AnchorMode.values()) {
			list.add(mode.icon());
		}
		return list;
	}

	@Override
	public void swap(Player sender, int index) {
		ItemStack held = getHeldAnchor(sender);
		if (held == null) return;
		AnchorItem.setMode(held, AnchorMode.byId(index));
	}

	@Override
	public Optional<WheelAdaptor<?>> get(@Nullable Player player, int wheelIndex, boolean main) {
		if (player == null) return Optional.empty();
		ItemStack stack = getHeldAnchor(player);
		if (stack == null) return Optional.empty();
		if (wheelIndex == 0) return Optional.of(new AnchorModeWheel(stack));
		return Optional.empty();
	}

}

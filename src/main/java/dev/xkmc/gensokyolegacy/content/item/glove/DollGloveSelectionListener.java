package dev.xkmc.gensokyolegacy.content.item.glove;

import dev.xkmc.gensokyolegacy.content.item.glove.client.DollGloveClientModes;
import dev.xkmc.gensokyolegacy.content.item.glove.client.DollGloveModeWheel;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveMode;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveModes;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.select.item.IItemSelector;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import dev.xkmc.l2library.util.Proxy;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The glove's item-selector entry: wheel and scroll selection over the visible modes, stored as an ordinal.
 *
 * @see "doc/design/doll/glove.md"
 */
public class DollGloveSelectionListener extends IItemSelector implements WheelAdaptor.Provider {

	public static final DollGloveSelectionListener INSTANCE = new DollGloveSelectionListener(GensokyoLegacy.loc("doll_glove"));
	public static final ResourceLocation ID = GensokyoLegacy.loc("doll_glove");

	public DollGloveSelectionListener(ResourceLocation id) {
		super(id);
	}

	public static void register() {
		IItemSelector.register(INSTANCE);
	}

	@Nullable
	public static ItemStack getHeldGlove(Player player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof DollGloveItem) return main;
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof DollGloveItem) return off;
		return null;
	}

	@Override
	public boolean test(ItemStack stack) {
		return stack.getItem() instanceof DollGloveItem;
	}

	@Override
	public int getIndex(Player player) {
		ItemStack held = getHeldGlove(player);
		if (held == null) return 0;
		var mode = DollGloveItem.getMode(held);
		var avail = player.level().isClientSide ?
				DollGloveClientModes.available(player, mode) :
				DollGloveModes.potentiallyVisible();
		int idx = avail.indexOf(mode);
		return idx < 0 ? 0 : idx;
	}

	@Override
	public List<ItemStack> getList() {
		// the scroll sidebar has no player param, so resolve the client player internally;
		// the server keeps the static set (sizing only, never shown).
		var modes = FMLEnvironment.dist == Dist.CLIENT ? clientModes() : DollGloveModes.potentiallyVisible();
		List<ItemStack> list = new ArrayList<>();
		for (var m : modes) {
			ItemStack icon = DollGloveItem.iconStack(m);
			icon.setHoverName(m.displayName());
			list.add(icon);
		}
		return list;
	}

	private static List<DollGloveMode> clientModes() {
		Player player = Proxy.getPlayer();
		if (player == null) return DollGloveModes.potentiallyVisible();
		ItemStack held = getHeldGlove(player);
		if (held == null) return DollGloveModes.potentiallyVisible();
		return DollGloveClientModes.availableForDisplay(held);
	}

	/**
	 * Scroll selection cycles the visible modes and returns the new mode's ordinal; {@link #swap} reads the
	 * slot back as an ordinal, so the pair stays consistent as the visible set changes size.
	 */
	@Override
	public int move(int i, Player player) {
		ItemStack held = getHeldGlove(player);
		if (held == null) return 0;
		var mode = DollGloveItem.getMode(held);
		var avail = DollGloveClientModes.available(player, mode);
		if (avail.isEmpty()) return mode.ordinal();
		int idx = avail.indexOf(mode);
		if (idx < 0) idx = 0;
		return avail.get(Math.floorMod(idx + i, avail.size())).ordinal();
	}

	@Override
	public void swap(Player sender, int index) {
		ItemStack held = getHeldGlove(sender);
		if (held == null) return;
		var modes = DollGloveMode.values();
		if (index < 0 || index >= modes.length) return;
		GLItems.DOLL_GLOVE_MODE.set(held, index);
	}

	@Override
	public Optional<WheelAdaptor<?>> get(@Nullable Player player, int wheelIndex, boolean main) {
		if (player == null) return Optional.empty();
		ItemStack stack = getHeldGlove(player);
		if (stack == null) return Optional.empty();
		if (wheelIndex == 0) {
			return Optional.of(new DollGloveModeWheel(stack));
		}
		return Optional.empty();
	}

}

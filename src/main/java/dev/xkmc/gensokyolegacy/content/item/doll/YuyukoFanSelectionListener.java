package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.item.doll.client.YuyukoFanModeWheel;
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
 * Selector listener for the {@link YuyukoFanItem}'s mode, mirroring the dagger glove's mode wheel.
 * Both modes are always listed, so the list never changes shape.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class YuyukoFanSelectionListener extends IItemSelector implements WheelAdaptor.Provider {

	public static final YuyukoFanSelectionListener INSTANCE =
			new YuyukoFanSelectionListener(GensokyoLegacy.loc("yuyuko_fan"));

	public YuyukoFanSelectionListener(ResourceLocation id) {
		super(id);
	}

	public static void register() {
		IItemSelector.register(INSTANCE);
	}

	@Nullable
	public static ItemStack getHeldFan(Player player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof YuyukoFanItem) return main;
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof YuyukoFanItem) return off;
		return null;
	}

	@Override
	public boolean test(ItemStack stack) {
		return stack.getItem() instanceof YuyukoFanItem;
	}

	@Override
	public int getIndex(Player player, ItemStack stack) {
		return YuyukoFanItem.getMode(stack).ordinal();
	}

	@Override
	public List<ItemStack> getList(ItemStack stack) {
		List<ItemStack> list = new ArrayList<>();
		for (YuyukoFanMode mode : YuyukoFanMode.values()) {
			list.add(YuyukoFanItem.iconStack(mode));
		}
		return list;
	}

	@Override
	public int getSelHash(ItemStack stack) {
		return YuyukoFanMode.values().length;
	}

	@Override
	public int move(int i, Player player, ItemStack stack) {
		var modes = YuyukoFanMode.values();
		return Math.floorMod(YuyukoFanItem.getMode(stack).ordinal() + i, modes.length);
	}

	@Override
	public void swap(Player sender, int index, ItemStack stack) {
		ItemStack held = getHeldFan(sender);
		if (held == null) return;
		YuyukoFanItem.setMode(held, YuyukoFanMode.byId(index));
	}

	@Override
	public Optional<WheelAdaptor<?>> get(@Nullable Player player, int wheelIndex, boolean main) {
		if (player == null) return Optional.empty();
		ItemStack stack = getHeldFan(player);
		if (stack == null) return Optional.empty();
		if (wheelIndex == 0) return Optional.of(new YuyukoFanModeWheel(stack));
		return Optional.empty();
	}

}

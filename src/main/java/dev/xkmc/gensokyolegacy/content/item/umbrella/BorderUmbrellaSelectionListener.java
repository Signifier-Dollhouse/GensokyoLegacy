package dev.xkmc.gensokyolegacy.content.item.umbrella;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.select.item.IItemSelector;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaMode;
import dev.xkmc.gensokyolegacy.content.item.umbrella.wheel.BorderUmbrellaDistanceWheel;
import dev.xkmc.gensokyolegacy.content.item.umbrella.wheel.BorderUmbrellaModeWheel;
import dev.xkmc.gensokyolegacy.content.item.umbrella.wheel.BorderUmbrellaSlotWheel;
import dev.xkmc.gensokyolegacy.content.item.umbrella.wheel.UmbrellaFakeWheel;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.world.item.Items;

public class BorderUmbrellaSelectionListener extends IItemSelector implements WheelAdaptor.Provider {

	public static final BorderUmbrellaSelectionListener INSTANCE = new BorderUmbrellaSelectionListener(GensokyoLegacy.loc("border_umbrella"));
	public static final ResourceLocation ID = GensokyoLegacy.loc("border_umbrella");

	public BorderUmbrellaSelectionListener(ResourceLocation id) {
		super(id);
	}

	public static void register() {
		IItemSelector.register(INSTANCE);
	}

	@Nullable
	public static ItemStack getHeldUmbrella(Player player) {
		ItemStack main = player.getMainHandItem();
		if (main.getItem() instanceof BorderUmbrellaItem) return main;
		ItemStack off = player.getOffhandItem();
		if (off.getItem() instanceof BorderUmbrellaItem) return off;
		return null;
	}

	public static List<BorderUmbrellaMode> getAvailableModes(ItemStack stack) {
		List<BorderUmbrellaMode> ans = new ArrayList<>();
		for (var m : BorderUmbrellaMode.values()) {
			if (m.isAvailable(stack)) ans.add(m);
		}
		return ans;
	}

	@Override
	public boolean test(ItemStack stack) {
		return stack.getItem() instanceof BorderUmbrellaItem;
	}

	@Override
	public int getIndex(Player player, ItemStack stack) {
		var mode = stack.getOrDefault(GLItems.UMBRELLA_TYPE.get(), BorderUmbrellaMode.RECORD);
		var avail = getAvailableModes(stack);
		int idx = avail.indexOf(mode);
		return idx < 0 ? 0 : idx;
	}

	@Override
	public List<ItemStack> getList(ItemStack stack) {
		var avail = getAvailableModes(stack);
		List<ItemStack> list = new ArrayList<>();
		for (var m : avail) {
			ItemStack icon = m.icon();
			icon.set(DataComponents.ITEM_NAME, m.displayName());
			list.add(icon);
		}
		return list;
	}

	@Override
	public void swap(Player sender, int index, ItemStack stack) {
		ItemStack held = getHeldUmbrella(sender);
		if (held == null) return;
		var avail = getAvailableModes(held);
		if (index < 0 || index >= avail.size()) return;
		var mode = avail.get(index);
		held.set(GLItems.UMBRELLA_TYPE.get(), mode);
	}

	@Override
	public Optional<WheelAdaptor<?>> get(@Nullable Player player, int wheelIndex, boolean main) {
		if (player == null) return Optional.empty();
		ItemStack stack = getHeldUmbrella(player);
		if (stack == null) return Optional.empty();
		if (wheelIndex == 0) {
			return Optional.of(new BorderUmbrellaModeWheel(stack));
		} else if (wheelIndex == 1) {
			return Optional.of(new BorderUmbrellaSlotWheel(stack));
		} else if (wheelIndex == -1) {
			return Optional.of(new BorderUmbrellaDistanceWheel(stack));
		} else if (wheelIndex == 2) {
			// fake wheel at index 2 for editing stored position, like GolemFakeWheel in ModularGolems
			ItemStack icon = new ItemStack(Items.NAME_TAG);
			return Optional.of(new UmbrellaFakeWheel(icon, GLLang.UMBRELLA$WHEEL_EDIT.get()));
		}
		return Optional.empty();
	}
}

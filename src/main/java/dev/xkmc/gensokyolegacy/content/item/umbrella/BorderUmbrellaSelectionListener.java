package dev.xkmc.gensokyolegacy.content.item.umbrella;

import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaMode;
import dev.xkmc.gensokyolegacy.content.item.umbrella.wheel.*;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.select.item.IItemSelector;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
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
	public int getIndex(Player player) {
		ItemStack held = getHeldUmbrella(player);
		if (held == null) return 0;
		var mode = GLItems.UMBRELLA_TYPE.getOrDefault(held, BorderUmbrellaMode.RECORD);
		var avail = getAvailableModes(held);
		int idx = avail.indexOf(mode);
		return idx < 0 ? 0 : idx;
	}

	@Override
	public List<ItemStack> getList() {
		List<ItemStack> list = new ArrayList<>();
		ItemStack held = FMLEnvironment.dist == Dist.CLIENT ? clientHeld() : null;
		if (held == null) return list;
		for (var m : getAvailableModes(held)) {
			ItemStack icon = m.icon();
			icon.setHoverName(m.displayName());
			list.add(icon);
		}
		return list;
	}

	@Nullable
	private static ItemStack clientHeld() {
		Player player = Minecraft.getInstance().player;
		return player == null ? null : getHeldUmbrella(player);
	}

	@Override
	public void swap(Player sender, int index) {
		ItemStack held = getHeldUmbrella(sender);
		if (held == null) return;
		var avail = getAvailableModes(held);
		if (index < 0 || index >= avail.size()) return;
		var mode = avail.get(index);
		GLItems.UMBRELLA_TYPE.set(held, mode);
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
			return Optional.of(new UmbrellaFakeWheel(UmbrellaWheelIcons.EDIT, GLLang.ItemUmbrella.WHEEL_EDIT.get()));
		}
		return Optional.empty();
	}
}

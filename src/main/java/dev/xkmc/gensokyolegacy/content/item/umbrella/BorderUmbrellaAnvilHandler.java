package dev.xkmc.gensokyolegacy.content.item.umbrella;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import dev.xkmc.gensokyolegacy.content.item.umbrella.data.BorderUmbrellaUnlock;

public class BorderUmbrellaAnvilHandler {

	@SubscribeEvent
	public static void onAnvilUpdate(AnvilUpdateEvent e) {
		ItemStack left = e.getLeft();
		ItemStack right = e.getRight();
		if (!(left.getItem() instanceof BorderUmbrellaItem)) return;
		var unlock = GLItems.UMBRELLA_UNLOCK.getOrDefault(left, BorderUmbrellaUnlock.DEFAULT);
		if (right.is(Items.CHORUS_FRUIT)) {
			if (unlock.travelUnlocked()) return;
			ItemStack out = left.copy();
			out.set(GLItems.UMBRELLA_UNLOCK.get(), unlock.withTravel(true));
			e.setOutput(out);
			e.setCost(5);
			e.setMaterialCost(1);
		} else if (right.is(Items.ECHO_SHARD)) {
			if (unlock.captureUnlocked()) return;
			ItemStack out = left.copy();
			out.set(GLItems.UMBRELLA_UNLOCK.get(), unlock.withCapture(true));
			e.setOutput(out);
			e.setCost(10);
			e.setMaterialCost(1);
		}
	}
}
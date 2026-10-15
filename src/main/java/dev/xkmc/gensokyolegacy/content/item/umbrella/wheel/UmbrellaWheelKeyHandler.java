package dev.xkmc.gensokyolegacy.content.item.umbrella.wheel;

import dev.xkmc.gensokyolegacy.content.item.umbrella.screen.BorderUmbrellaManageScreen;
import dev.xkmc.l2itemselector.wheel.DefaultKeyHandler;
import dev.xkmc.l2itemselector.wheel.WheelAdaptor;
import dev.xkmc.l2itemselector.wheel.WheelContext;
import dev.xkmc.l2itemselector.wheel.WheelHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Handles a click on the fake edit wheel at index 2. Mirrors {@code GolemWheelKeyHandler} from
 * ModularGolems: intercepts a SWITCH whose target index is 2 and opens the position editor.
 */
public class UmbrellaWheelKeyHandler extends DefaultKeyHandler.Fast {

	@Override
	protected void execute(WheelAdaptor<?> wheel, Player player, ActionCode action, WheelContext ctx) {
		if (action == ActionCode.SWITCH) {
			int target = WheelHandler.wheelIndex + ctx.code().switcher();
			if (target == 2) {
				WheelHandler.disableWheel(player);
				Minecraft.getInstance().mouseHandler.releaseMouse();
				BorderUmbrellaManageScreen.openViaWheel();
				return;
			}
		}
		super.execute(wheel, player, action, ctx);
	}
}

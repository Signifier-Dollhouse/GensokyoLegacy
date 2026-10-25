package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * One fan mode's air-click action and selector icon, carried by the enum constant.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public interface YuyukoFanModeHandler {

	/**
	 * Performs the mode. Returns true when something happened and the fan should spend a use.
	 */
	boolean use(ServerPlayer player, ItemStack stack, DollAttachment att);

	/**
	 * The un-named selector icon for this mode.
	 */
	ItemStack icon();

}

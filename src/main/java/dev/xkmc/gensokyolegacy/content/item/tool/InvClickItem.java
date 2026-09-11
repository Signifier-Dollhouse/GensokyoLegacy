package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.server.level.ServerPlayer;

public interface InvClickItem {
	void handleClick(ServerPlayer sp, PlayerSlot<?> slot);
}

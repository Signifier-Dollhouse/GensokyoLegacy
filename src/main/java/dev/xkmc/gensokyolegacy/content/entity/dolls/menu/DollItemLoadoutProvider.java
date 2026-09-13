package dev.xkmc.gensokyolegacy.content.entity.dolls.menu;

import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record DollItemLoadoutProvider(
		ServerPlayer sp, PlayerSlot<?> slot
) implements MenuProvider {

	public static void open(ServerPlayer sp, PlayerSlot<?> slot) {
		new DollItemLoadoutProvider(sp, slot).open();
	}

	@Override
	public Component getDisplayName() {
		return slot.getItem(sp).getHoverName();
	}

	public void open() {
		sp.openMenu(this, buf -> buf.writeBoolean(false));
	}

	@Override
	public AbstractContainerMenu createMenu(int wid, Inventory inv, Player pl) {
		return new DollLoadoutMenu(GLMisc.DOLL_LOADOUT.get(), wid, inv, slot);
	}

}

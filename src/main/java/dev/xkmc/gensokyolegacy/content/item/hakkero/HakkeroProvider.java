package dev.xkmc.gensokyolegacy.content.item.hakkero;

import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import dev.xkmc.l2menustacker.screen.source.PlayerSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record HakkeroProvider(ServerPlayer sp, PlayerSlot<?> slot) implements MenuProvider {

	public static void open(ServerPlayer sp, PlayerSlot<?> slot) {
		new HakkeroProvider(sp, slot).open();
	}

	@Override
	public Component getDisplayName() {
		return Component.empty();
	}

	public void open() {
		sp.openMenu(this);
	}

	@Override
	public AbstractContainerMenu createMenu(int wid, Inventory inv, Player pl) {
		return new HakkeroMenu(GLMisc.MINI_HAKKERO.get(), wid, inv, slot);
	}

}

package dev.xkmc.gensokyolegacy.content.ui.talisman;

import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public record TalismanPocketProvider(
		ServerPlayer sp, InteractionHand hand
) implements MenuProvider {

	public static void open(ServerPlayer sp, InteractionHand hand) {
		new TalismanPocketProvider(sp, hand).open();
	}

	@Override
	public Component getDisplayName() {
		return Component.empty();
	}

	public void open() {
		sp.openMenu(this, this::write);
	}

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeEnum(hand);
	}

	@Override
	public AbstractContainerMenu createMenu(int wid, Inventory inv, Player pl) {
		return new TalismanPocketMenu(GLMisc.TALISMAN_POCKET.get(), wid, inv, hand);
	}

}
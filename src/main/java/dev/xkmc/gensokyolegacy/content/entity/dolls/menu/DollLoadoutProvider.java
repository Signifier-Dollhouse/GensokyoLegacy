package dev.xkmc.gensokyolegacy.content.entity.dolls.menu;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLMisc;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public record DollLoadoutProvider(
		ServerPlayer sp, int dollId
) implements MenuProvider {

	public static void open(ServerPlayer sp, DollEntity doll) {
		new DollLoadoutProvider(sp, doll.getId()).open();
	}

	@Override
	public Component getDisplayName() {
		Entity entity = sp.level().getEntity(dollId);
		return entity != null ? entity.getDisplayName() : Component.empty();
	}

	public void open() {
		sp.openMenu(this, buf -> {
			buf.writeBoolean(true);
			buf.writeInt(dollId);
		});
	}

	@Nullable
	private DollEntity doll() {
		Entity entity = sp.level().getEntity(dollId);
		return entity instanceof DollEntity doll ? doll : null;
	}

	@Override
	public AbstractContainerMenu createMenu(int wid, Inventory inv, Player pl) {
		return new DollLoadoutMenu(GLMisc.DOLL_LOADOUT.get(), wid, inv, doll());
	}

}

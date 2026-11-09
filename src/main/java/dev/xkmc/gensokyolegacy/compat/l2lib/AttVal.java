package dev.xkmc.gensokyolegacy.compat.l2lib;

import dev.xkmc.l2library.capability.entity.GeneralCapabilityHolder;
import dev.xkmc.l2library.capability.entity.GeneralCapabilityTemplate;
import dev.xkmc.l2library.capability.player.PlayerCapabilityTemplate;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public interface AttVal<H extends ICapabilityProvider, T extends GeneralCapabilityTemplate<H, T>> {

	AttType<H, T> type();

	interface CapVal<E extends ICapabilityProvider, T extends GeneralCapabilityTemplate<E, T>>
			extends AttVal<E, T> {

	}

	interface PlayerVal<T extends PlayerCapabilityTemplate<T>> extends AttVal<Player, T> {

	}

}

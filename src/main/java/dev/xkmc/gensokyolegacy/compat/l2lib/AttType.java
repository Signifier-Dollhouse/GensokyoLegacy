package dev.xkmc.gensokyolegacy.compat.l2lib;

import dev.xkmc.l2library.capability.entity.GeneralCapabilityHolder;
import dev.xkmc.l2library.capability.entity.GeneralCapabilityTemplate;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import java.util.Optional;

public record AttType<H extends ICapabilityProvider, T extends GeneralCapabilityTemplate<H, T>>(
		GeneralCapabilityHolder<H, T> holder) {

	public T getOrCreate(H e) {
		return holder.get(e);
	}

	public boolean isProper(H e) {
		return holder.isProper(e);
	}

	public Optional<T> getExisting(H e) {
		return holder.isProper(e) ? Optional.of(holder.get(e)) : Optional.empty();
	}

}

package dev.xkmc.gensokyolegacy.compat.l2lib;

import dev.xkmc.l2library.capability.entity.GeneralCapabilityHolder;
import dev.xkmc.l2library.capability.entity.GeneralCapabilityTemplate;
import dev.xkmc.l2library.capability.player.PlayerCapabilityHolder;
import dev.xkmc.l2library.capability.player.PlayerCapabilityNetworkHandler;
import dev.xkmc.l2library.capability.player.PlayerCapabilityTemplate;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Builds the legacy capability holders behind the 1.21 attachment-style API. Each attachment
 * class declares a static {@code CAPABILITY} token, which is what the holder is built from.
 */
public final class AttReg {

	private final String modid;

	private AttReg(String modid) {
		this.modid = modid;
	}

	public static AttReg of(Reg reg) {
		return new AttReg(reg.modid());
	}

	@SuppressWarnings("unchecked")
	private static <T> Capability<T> capability(Class<T> cls) {
		try {
			Field f = cls.getField("CAPABILITY");
			return (Capability<T>) f.get(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(cls.getName() + " has no static CAPABILITY field", e);
		}
	}

	public <E extends PlayerCapabilityTemplate<E>> AttVal.PlayerVal<E> player(
			String id, Class<E> cls, Supplier<E> sup,
			Function<PlayerCapabilityHolder<E>, PlayerCapabilityNetworkHandler<E>> network) {
		ResourceLocation rl = new ResourceLocation(modid, id);
		var holder = new PlayerCapabilityHolder<>(rl, capability(cls), cls, sup, network);
		return new PlayerValImpl<>(rl, holder);
	}

	public <H extends ICapabilityProvider, E extends GeneralCapabilityTemplate<H, E>> AttVal.CapVal<H, E> entity(
			String id, Class<E> cls, Supplier<E> sup, Class<H> entityClass, Predicate<H> pred) {
		ResourceLocation rl = new ResourceLocation(modid, id);
		var holder = new GeneralCapabilityHolder<>(rl, capability(cls), cls, sup, entityClass, pred);
		return new CapValImpl<>(rl, holder);
	}

	private record PlayerValImpl<E extends PlayerCapabilityTemplate<E>>(
			ResourceLocation id, PlayerCapabilityHolder<E> holder) implements AttVal.PlayerVal<E> {

		@Override
		public AttType<net.minecraft.world.entity.player.Player, E> type() {
			return new AttType<>(holder);
		}

	}

	private record CapValImpl<H extends ICapabilityProvider, E extends GeneralCapabilityTemplate<H, E>>(
			ResourceLocation id, GeneralCapabilityHolder<H, E> holder) implements AttVal.CapVal<H, E> {

		@Override
		public AttType<H, E> type() {
			return new AttType<>(holder);
		}

	}

}

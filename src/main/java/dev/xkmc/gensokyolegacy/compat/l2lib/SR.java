package dev.xkmc.gensokyolegacy.compat.l2lib;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Deferred registration helper for built-in registries.
 */
public record SR<T>(DeferredRegister<T> reg, String modid) {

	public static <T> SR<T> of(Reg parent, Registry<T> reg) {
		return new SR<>(parent.make(reg), parent.modid());
	}

	public static <T> SR<T> of(Reg parent, IForgeRegistry<T> reg) {
		return new SR<>(parent.make(reg), parent.modid());
	}

	public static <T> SR<T> of(Reg parent, ResourceKey<? extends Registry<T>> reg) {
		return new SR<>(parent.make(reg), parent.modid());
	}

	public <H extends T> Val<H> reg(String id, Supplier<H> sup) {
		return new Val.Deferred<>(reg.register(id, sup));
	}

	public <H extends T> Val<H> reg(String id, Function<ResourceLocation, H> sup) {
		return new Val.Deferred<>(reg.register(id, () -> sup.apply(new ResourceLocation(modid, id))));
	}

}

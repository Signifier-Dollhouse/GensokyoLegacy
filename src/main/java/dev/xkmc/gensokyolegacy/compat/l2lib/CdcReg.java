package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.serialization.Codec;
import dev.xkmc.l2serial.serialization.codec.CodecAdaptor;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deferred registration for a registry of codecs, as used by the RPG element registries.
 */
public record CdcReg<T>(DeferredRegister<Codec<? extends T>> reg) {

	public static <T> CdcReg<T> of(Reg parent, ResourceKey<Registry<Codec<? extends T>>> key) {
		return new CdcReg<>(parent.make(key));
	}

	public static <T> CdcReg<T> of(Reg parent, Registry<Codec<? extends T>> reg) {
		return new CdcReg<>(parent.make(reg));
	}

	public <R extends T> CdcVal<R> reg(String id, Codec<R> codec) {
		return new CdcValImpl<R, T>(reg.register(id, () -> codec), codec);
	}

	public <R extends T> CdcVal<R> reg(String id, Class<R> cls) {
		return new CdcValImpl<R, T>(reg.register(id, () -> new CodecAdaptor<>(cls)), new CodecAdaptor<>(cls));
	}

	private record CdcValImpl<R extends T, T>(RegistryObject<Codec<? extends T>> val, Codec<R> codec)
			implements CdcVal<R> {

		@Override
		public Codec<R> get() {
			return codec;
		}

		@Override
		public ResourceLocation id() {
			return val.getId();
		}

		@Override
		public ResourceKey<? super Codec<R>> key() {
			return Wrappers.cast(val.getKey());
		}

	}

}

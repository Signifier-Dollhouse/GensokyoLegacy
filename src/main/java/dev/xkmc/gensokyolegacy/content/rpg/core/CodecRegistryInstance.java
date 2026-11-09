package dev.xkmc.gensokyolegacy.content.rpg.core;

import com.mojang.serialization.Codec;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import dev.xkmc.gensokyolegacy.compat.l2lib.CdcReg;
import dev.xkmc.gensokyolegacy.compat.l2lib.CdcVal;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.util.Wrappers;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

public record CodecRegistryInstance<T extends CodecElement<?>>(
		ResourceKey<Registry<Codec<? extends T>>> key,
		Supplier<IForgeRegistry<Codec<? extends T>>> registry,
		CdcReg<T> reg
) {

	public static <T extends CodecElement<?>> CodecRegistryInstance<T> of(String id) {
		ResourceKey<Registry<Codec<? extends T>>> key = ResourceKey.createRegistryKey(GensokyoLegacy.loc(id));
		RegistryBuilder<Codec<? extends T>> builder = new RegistryBuilder<>();
		builder.setName(key.location());
		Supplier<IForgeRegistry<Codec<? extends T>>>[] holder = new Supplier[]{() -> null};
		OneTimeEventReceiver.addModListener(GensokyoLegacy.REGISTRATE, NewRegistryEvent.class,
				e -> holder[0] = e.create(builder));
		return new CodecRegistryInstance<>(key, () -> holder[0].get(), CdcReg.of(GensokyoLegacy.REG, key));
	}

	public Codec<T> codec() {
		return registry.get().getCodec().dispatch(x -> Wrappers.cast(x.codec()), e -> e);
	}

	public <E extends T> CdcVal<E> reg(String id, Codec<E> codec) {
		return reg.reg(id, codec);
	}

}

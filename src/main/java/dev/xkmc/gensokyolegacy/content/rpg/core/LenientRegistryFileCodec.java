package dev.xkmc.gensokyolegacy.content.rpg.core;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

/**
 * A {@link RegistryFileCodec} that falls back to the inline value when the ops cannot see the registry:
 * 1.20.1 syncs datapack registries in the login packet under builtin-only registry ops.
 */
public final class LenientRegistryFileCodec<E> implements Codec<Holder<E>> {

	private final ResourceKey<? extends Registry<E>> key;
	private final Codec<E> element;
	private final RegistryFileCodec<E> strict;

	private LenientRegistryFileCodec(ResourceKey<? extends Registry<E>> key, Codec<E> element) {
		this.key = key;
		this.element = element;
		this.strict = RegistryFileCodec.create(key, element);
	}

	public static <E> Codec<Holder<E>> create(ResourceKey<? extends Registry<E>> key, Codec<E> element) {
		return new LenientRegistryFileCodec<>(key, element);
	}

	@Override
	public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> ops, T input) {
		if (ops instanceof RegistryOps<?> reg && reg.getter(key).isPresent())
			return strict.decode(ops, input);
		return element.decode(ops, input).map(p -> p.mapFirst(Holder::direct));
	}

	@Override
	public <T> DataResult<T> encode(Holder<E> input, DynamicOps<T> ops, T prefix) {
		if (ops instanceof RegistryOps<?> reg && reg.owner(key).isPresent())
			return strict.encode(input, ops, prefix);
		return element.encode(input.value(), ops, prefix);
	}

}

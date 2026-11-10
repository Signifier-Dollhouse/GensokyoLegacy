package dev.xkmc.gensokyolegacy.content.rpg.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.xkmc.gensokyolegacy.compat.l2lib.CdcVal;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Codec dispatch for the RPG element types. Forge registries are not available until after mod
 * construction, so the name-to-codec mapping is kept locally instead of in a registry.
 */
public final class CodecRegistryInstance<T extends CodecElement<?>> {

	private final ResourceLocation id;
	private final Map<ResourceLocation, Codec<? extends T>> byId = new LinkedHashMap<>();
	private final Map<Codec<? extends T>, ResourceLocation> byCodec = new IdentityHashMap<>();

	private CodecRegistryInstance(ResourceLocation id) {
		this.id = id;
	}

	public static <T extends CodecElement<?>> CodecRegistryInstance<T> of(String id) {
		return new CodecRegistryInstance<>(GensokyoLegacy.loc(id));
	}

	public Codec<T> codec() {
		return ResourceLocation.CODEC.partialDispatch("type",
				e -> {
					ResourceLocation key = byCodec.get(e.codec());
					return key == null
							? DataResult.error(() -> "Unregistered " + id + " codec: " + e.getClass().getName())
							: DataResult.success(key);
				},
				key -> {
					Codec<? extends T> codec = byId.get(key);
					return codec == null
							? DataResult.error(() -> "Unknown " + id + " type: " + key)
							: DataResult.success(codec);
				});
	}

	public <E extends T> CdcVal<E> reg(String name, Codec<E> codec) {
		ResourceLocation key = GensokyoLegacy.loc(name);
		byId.put(key, codec);
		byCodec.put(codec, key);
		return new Entry<>(key, codec);
	}

	private record Entry<T>(ResourceLocation id, Codec<T> codec) implements CdcVal<T> {

		@Override
		public Codec<T> get() {
			return codec;
		}

		@Override
		public ResourceKey<? super Codec<T>> key() {
			throw new UnsupportedOperationException(id + " is not registry-backed");
		}

	}

}

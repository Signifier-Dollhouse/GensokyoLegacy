package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.compat.l2lib.datamap.DataMapConfig;
import dev.xkmc.l2library.serial.config.BaseConfig;
import dev.xkmc.l2library.serial.config.ConfigTypeEntry;
import dev.xkmc.l2library.serial.config.PacketHandlerWithConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The 1.20.1 replacement for NeoForge data maps: a {@link ConfigTypeEntry} whose merged config
 * holds the map, exposed with the {@code get(Holder)} shape the 1.21 call sites use.
 */
public final class DataMapReg<K, V> {

	private final ResourceLocation id;
	private final ResourceKey<? extends Registry<K>> key;
	private final ConfigTypeEntry<? extends BaseConfig> entry;

	public <C extends DataMapConfig<V>> DataMapReg(PacketHandlerWithConfig channel,
			ResourceLocation id, ResourceKey<? extends Registry<K>> key, Class<C> cls) {
		this.id = id;
		this.key = key;
		this.entry = new ConfigTypeEntry<>(channel, id.getPath(), cls);
	}

	@Nullable
	public V get(RegistryAccess access, Holder<K> holder) {
		return get(holder);
	}

	@Nullable
	public V get(Holder<K> holder) {
		return holder.unwrapKey().map(e -> values().get(e.location())).orElse(null);
	}

	public Stream<Pair<Holder<K>, V>> getAll(RegistryAccess access) {
		var reg = access.registry(key);
		if (reg.isEmpty()) return Stream.empty();
		return values().entrySet().stream()
				.flatMap(e -> reg.get().getHolder(ResourceKey.create(key, e.getKey()))
						.<Pair<Holder<K>, V>>map(h -> Pair.of(h, e.getValue())).stream());
	}

	public ResourceLocation id() {
		return id;
	}

	@SuppressWarnings("unchecked")
	private Map<ResourceLocation, V> values() {
		return ((DataMapConfig<V>) entry.getMerged()).values();
	}

	@SuppressWarnings("unchecked")
	public <C extends DataMapConfig<V>> ConfigTypeEntry<C> entry() {
		return (ConfigTypeEntry<C>) entry;
	}

}

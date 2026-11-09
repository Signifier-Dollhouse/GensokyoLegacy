package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * Placeholder for the 1.21 data-map registration. Data is read through the JSON config layer
 * (see {@code doc/backport/decisions.md} D12); until that lands the lookups report nothing.
 */
public record DataMapReg<K, V>(ResourceLocation id, ResourceKey<? extends Registry<K>> key, Codec<V> codec) {

	@Nullable
	public V get(RegistryAccess access, Holder<K> holder) {
		return null;
	}

	@Nullable
	public V get(Holder<K> holder) {
		var server = ServerLifecycleHooks.getCurrentServer();
		return server == null ? null : get(server.registryAccess(), holder);
	}

	public Stream<Pair<Holder<K>, V>> getAll(RegistryAccess access) {
		return Stream.empty();
	}

}

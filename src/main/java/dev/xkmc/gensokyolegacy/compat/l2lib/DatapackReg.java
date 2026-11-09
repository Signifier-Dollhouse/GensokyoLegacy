package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DataPackRegistryEvent;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * A datapack registry registered through {@link DataPackRegistryEvent.NewRegistry}.
 */
public record DatapackReg<T>(ResourceKey<Registry<T>> key, Codec<T> codec) {

	public void onRegister(DataPackRegistryEvent.NewRegistry event) {
		event.dataPackRegistry(key, codec, codec);
	}

	@Nullable
	public Holder<T> get(RegistryAccess access, ResourceLocation id) {
		var reg = access.registry(key);
		if (reg.isEmpty()) return null;
		return reg.get().getHolder(ResourceKey.create(key, id)).orElse(null);
	}

	public Stream<Holder<T>> getAll(RegistryAccess access) {
		var reg = access.registry(key);
		if (reg.isEmpty()) return Stream.empty();
		return reg.get().holders().map(e -> e);
	}

}

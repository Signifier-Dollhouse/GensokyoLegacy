package dev.xkmc.gensokyolegacy.compat.datagen;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * 1.21's Registrate {@code DataProviderInitializer} on the 1.20.1 datagen API: bootstraps are collected into one
 * {@link RegistrySetBuilder} that {@code GensokyoLegacy#gatherData} turns into a {@code DatapackBuiltinEntriesProvider}.
 */
public class DataProviderInitializer {

	private final RegistrySetBuilder builder = new RegistrySetBuilder();

	public <T> void add(ResourceKey<? extends Registry<T>> key, RegistrySetBuilder.RegistryBootstrap<T> bootstrap) {
		builder.add(key, bootstrap);
	}

	public RegistrySetBuilder builder() {
		return builder;
	}

	/**
	 * Tag providers over a dynamic registry need the built entries in their lookup, which the vanilla
	 * {@link GatherDataEvent#getLookupProvider()} does not carry. The real provider is constructed directly in
	 * {@code gatherData}; this type only exists so the generator methods keep one parameter type.
	 */
	public static <T> ProviderType<RegistrateTagsProvider.Impl<T>> dynamicTag(String type) {
		return ProviderType.register(type, (registrate, event) -> {
			throw new UnsupportedOperationException("dynamic tag providers are created in gatherData");
		});
	}

	public static <T> RegistrateTagsProvider.Impl<T> tagProvider(AbstractRegistrate<?> registrate,
			ProviderType<RegistrateTagsProvider.Impl<T>> type, String name,
			ResourceKey<? extends Registry<T>> key, PackOutput out,
			CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper helper) {
		return new RegistrateTagsProvider.Impl<>(registrate, type, name, out, key, registries, helper);
	}

}

package dev.xkmc.gensokyolegacy.compat.datagen;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.resources.ResourceKey;
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
	 * {@link GatherDataEvent#getLookupProvider()} does not carry. {@link #dynamicTag} only registers the type so
	 * Registrate builds a provider per tag kind; the callbacks run inside their {@code addTags} at run time.
	 */
	public static <T> ProviderType<RegistrateTagsProvider.Impl<T>> dynamicTag(String type, ResourceKey<? extends Registry<T>> key) {
		return ProviderType.register("tags/" + type,
				(pvd) -> (reg, e) ->
						new RegistrateTagsProvider.Impl<T>(reg, pvd, type, e.getGenerator().getPackOutput(),
								key, e.getLookupProvider(), e.getExistingFileHelper()));
	}

}

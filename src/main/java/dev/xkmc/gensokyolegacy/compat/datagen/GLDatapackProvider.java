package dev.xkmc.gensokyolegacy.compat.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A {@link DatapackBuiltinEntriesProvider} with a distinct name. 1.20.1 rejects two providers
 * with the same name, and every instance of the vanilla class is called "Registries".
 */
public class GLDatapackProvider extends DatapackBuiltinEntriesProvider {

	private final String name;

	public GLDatapackProvider(String name, PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
							  RegistrySetBuilder builder, Set<String> modIds) {
		super(output, registries, builder, modIds);
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}

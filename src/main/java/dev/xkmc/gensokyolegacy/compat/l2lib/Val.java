package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * A registered value that also knows its id and registry key.
 */
public interface Val<T> extends Supplier<T> {

	T get();

	ResourceLocation id();

	ResourceKey<? super T> key();

	record Registrate<T>(RegistryEntry<T> val) implements Val<T> {

		@Override
		public T get() {
			return val.get();
		}

		@Override
		public ResourceLocation id() {
			return val.getId();
		}

		@Override
		public ResourceKey<? super T> key() {
			return val.getKey();
		}

	}

	/**
	 * A value that is not registry-backed, like a criterion trigger on 1.20.1.
	 */
	record Unregistered<T>(ResourceLocation id, Supplier<T> sup) implements Val<T> {

		@Override
		public T get() {
			return sup.get();
		}

		@Override
		public ResourceKey<? super T> key() {
			throw new UnsupportedOperationException(id + " is not registry-backed");
		}

	}

	record Deferred<T>(RegistryObject<T> val) implements Val<T> {

		@Override
		public T get() {
			return val.get();
		}

		@Override
		public ResourceLocation id() {
			return val.getId();
		}

		@Override
		public ResourceKey<? super T> key() {
			return val.getKey();
		}

	}

}

package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

public record SimpleEntry<T>(RegistryEntry<T> val) implements LegacyHolder<T> {

	/**
	 * Wrap a registrate entry whose type is a subtype of the holder's, which a record's invariant field cannot.
	 */
	@SuppressWarnings("unchecked")
	public static <T> SimpleEntry<T> of(RegistryEntry<? extends T> val) {
		return new SimpleEntry<>((RegistryEntry<T>) val);
	}

	@Override
	public ResourceKey<T> key() {
		return val.getKey();
	}

	@Override
	public T get() {
		return val.get();
	}

	@Override
	public Holder<T> holder() {
		return this;
	}

	@Override
	public int hashCode() {
		return key().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return obj instanceof Holder<?> h &&
				h.kind() == Kind.REFERENCE &&
				key().equals(h.unwrapKey().orElse(null));
	}

}

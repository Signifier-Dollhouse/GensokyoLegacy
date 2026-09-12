package dev.xkmc.gensokyolegacy.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.function.Function;

public final class DummyHolderGetter {

	private static final class BoundRef<T> extends Holder.Reference<T> {

		private BoundRef(HolderOwner<T> owner, ResourceKey<T> key, T value) {
			super(Holder.Reference.Type.STAND_ALONE, owner, key, value);
		}

	}

	private DummyHolderGetter() {
	}

	public static <T> HolderGetter<T> create(Function<ResourceKey<T>, T> dummyValue) {
		HolderOwner<T> owner = new HolderOwner<>() {

			@Override
			public boolean canSerializeIn(HolderOwner<T> other) {
				return true;
			}

		};
		return new HolderGetter<>() {

			@Override
			public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
				return Optional.of(new BoundRef<>(owner, key, dummyValue.apply(key)));
			}

			@Override
			public Optional<HolderSet.Named<T>> get(TagKey<T> key) {
				return Optional.empty();
			}

		};
	}

}
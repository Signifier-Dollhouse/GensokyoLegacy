package dev.xkmc.gensokyolegacy.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.Optional;

public final class DummyHolderGetter<T> implements HolderOwner<T>, HolderGetter<T> {

	public static <T> HolderGetter<T> create() {
		return new DummyHolderGetter<>();
	}

	@Override
	public boolean canSerializeIn(HolderOwner<T> other) {
		return true;
	}

	@Override
	public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
		return Optional.of(Holder.Reference.createStandAlone(this, key));
	}

	@Override
	public Optional<HolderSet.Named<T>> get(TagKey<T> key) {
		return Optional.empty();
	}

}
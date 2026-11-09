package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record DataGenHolder<T>(ResourceKey<T> key, T val) implements Holder<T> {

	@Override
	public T value() {
		return val;
	}

	@Override
	public boolean isBound() {
		return true;
	}

	@Override
	public boolean is(ResourceLocation id) {
		return false;
	}

	@Override
	public boolean is(ResourceKey<T> id) {
		return false;
	}

	@Override
	public boolean is(Predicate<ResourceKey<T>> pred) {
		return false;
	}

	@Override
	public boolean is(TagKey<T> tag) {
		return false;
	}

	@Override
	public Stream<TagKey<T>> tags() {
		return Stream.empty();
	}

	@Override
	public Either<ResourceKey<T>, T> unwrap() {
		return Either.left(key());
	}

	@Override
	public Optional<ResourceKey<T>> unwrapKey() {
		return Optional.of(key);
	}

	@Override
	public Kind kind() {
		return Kind.REFERENCE;
	}

	@Override
	public boolean canSerializeIn(HolderOwner<T> owner) {
		return true;
	}

}

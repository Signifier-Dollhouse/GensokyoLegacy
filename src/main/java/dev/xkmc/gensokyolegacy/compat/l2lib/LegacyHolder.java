package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A holder that also knows its registry key, so registrate entries can be passed where a
 * {@link Holder} is expected.
 */
public interface LegacyHolder<T> extends Holder<T>, Supplier<T> {

	ResourceKey<T> key();

	default Holder<T> holder() {
		return this;
	}

	@Override
	default T value() {
		return get();
	}

	@Override
	default boolean isBound() {
		return true;
	}

	@Override
	default boolean is(ResourceLocation id) {
		return key().location().equals(id);
	}

	@Override
	default boolean is(ResourceKey<T> id) {
		return key().equals(id);
	}

	@Override
	default boolean is(Predicate<ResourceKey<T>> pred) {
		return pred.test(key());
	}

	@Override
	default boolean is(TagKey<T> tag) {
		return false;
	}

	@Override
	default Stream<TagKey<T>> tags() {
		return Stream.empty();
	}

	@Override
	default Either<ResourceKey<T>, T> unwrap() {
		return Either.left(key());
	}

	@Override
	default Optional<ResourceKey<T>> unwrapKey() {
		return Optional.of(key());
	}

	@Override
	default Kind kind() {
		return Kind.REFERENCE;
	}

	@Override
	default boolean canSerializeIn(HolderOwner<T> owner) {
		return true;
	}

}

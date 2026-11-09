package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.mojang.serialization.Codec;

import java.util.List;

public record EnumCodec<T>(Codec<T> codec) {

	public static <T extends Enum<T>> EnumCodec<T> of(Class<T> cls, T[] vals) {
		var codec = Codec.STRING.xmap(e -> {
			try {
				return Enum.valueOf(cls, e);
			} catch (Exception ex) {
				throw new IllegalArgumentException(e + " is not a valid " + cls.getSimpleName()
						+ ". Valid values are: " + List.of(vals));
			}
		}, Enum::name);
		return new EnumCodec<>(codec);
	}

	public EnumCodec<List<T>> toList() {
		return new EnumCodec<>(codec.listOf());
	}

}

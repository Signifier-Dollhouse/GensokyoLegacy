package dev.xkmc.gensokyolegacy.content.rpg.core;

import com.mojang.serialization.Codec;

public interface CodecElement<T> {

	Codec<T> codec();

}

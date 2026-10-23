package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;

import java.util.List;

public record Dialog(
		String text,
		List<DialogOption<?>> options,
		List<String> animations
) {

	public static final Codec<Dialog> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("text").forGetter(Dialog::text),
			CodecRegistry.OPTION.codec().listOf().fieldOf("options").forGetter(Dialog::options),
			Codec.STRING.listOf().optionalFieldOf("animations", List.of()).forGetter(Dialog::animations)
	).apply(i, Dialog::new));

	/**
	 * A dialog written where it is used, or named by a registry key: the codec inlines a direct holder
	 * and writes a registry reference as its id. Chats and quests inline; only what must be named later
	 * is registered.
	 */
	public static final Codec<Holder<Dialog>> HOLDER = RegistryFileCodec.create(CodecRegistry.Keys.DIALOG, CODEC);

}

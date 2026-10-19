package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.core.CharacterEntry;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.core.GatedEntry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/**
 * A conversation topic a character offers on the topic list.
 * <p>
 * An override starter is not a topic at all: it is left out of the weighted roll that picks one chat
 * per conversation and is entered the moment the player starts talking to her, provided its
 * conditions hold. It therefore needs no starter line of its own, which is why {@code text} is
 * optional and empty for one.
 *
 * @param override whether this starter takes over the conversation instead of competing for the topic list
 */
public record DialogStarter(
		EntityType<?> character,
		List<QuestCondition<?>> conditions,
		String text,
		Holder<Dialog> dialog,
		int weight,
		boolean override
) implements GatedEntry, CharacterEntry {

	public DialogStarter(EntityType<?> character, List<QuestCondition<?>> conditions, String text, Holder<Dialog> dialog) {
		this(character, conditions, text, dialog, 1, false);
	}

	public static final Codec<DialogStarter> CODEC = RecordCodecBuilder.create(i -> i.group(
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("character").forGetter(DialogStarter::character),
			CodecRegistry.CONDITION.codec().listOf().fieldOf("conditions").forGetter(DialogStarter::conditions),
			Codec.STRING.optionalFieldOf("text", "").forGetter(DialogStarter::text),
			Dialog.HOLDER.fieldOf("dialog").forGetter(DialogStarter::dialog),
			Codec.INT.optionalFieldOf("weight", 1).forGetter(DialogStarter::weight),
			Codec.BOOL.optionalFieldOf("override", false).forGetter(DialogStarter::override)
	).apply(i, DialogStarter::new));

	public static final Codec<Holder<DialogStarter>> HOLDER = RegistryFileCodec.create(CodecRegistry.Keys.STARTER, CODEC);

}

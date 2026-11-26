package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.core.GatedEntry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.core.Holder;

import java.util.List;
import java.util.Optional;

/**
 * One branch of a {@link ConditionalDialogOption}: the conditions it answers to, what clicking it
 * does, and the dialog it opens.
 */
public record ConditionalEntry(
		List<QuestCondition<?>> conditions,
		List<DialogAction<?>> actions,
		Holder<Dialog> dialog
) implements OptionResult, GatedEntry {

	public static final Codec<ConditionalEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
			CodecRegistry.CONDITION.codec().listOf().optionalFieldOf("conditions", List.of())
					.forGetter(ConditionalEntry::conditions),
			CodecRegistry.ACTION.codec().listOf().optionalFieldOf("actions", List.of())
					.forGetter(ConditionalEntry::actions),
			Dialog.HOLDER.fieldOf("next").forGetter(ConditionalEntry::dialog)
	).apply(i, ConditionalEntry::new));

	@Override
	public Optional<Holder<Dialog>> next() {
		return Optional.of(dialog);
	}

}

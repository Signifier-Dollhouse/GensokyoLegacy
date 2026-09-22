package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import net.minecraft.core.Holder;

import java.util.List;
import java.util.Optional;

public record WeightedEntry(
		int weight,
		List<DialogAction<?>> actions,
		Optional<Holder<Dialog>> next
) implements OptionResult {

	public static final Codec<WeightedEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("weight").forGetter(WeightedEntry::weight),
			CodecRegistry.ACTION.codec().listOf().fieldOf("actions").forGetter(WeightedEntry::actions),
			Dialog.HOLDER.optionalFieldOf("next").forGetter(WeightedEntry::next)
	).apply(i, WeightedEntry::new));

}

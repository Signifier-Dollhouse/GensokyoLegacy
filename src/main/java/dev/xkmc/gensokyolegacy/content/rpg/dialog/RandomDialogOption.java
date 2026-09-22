package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record RandomDialogOption(
		List<QuestCondition<?>> conditions,
		String text,
		List<WeightedEntry> entries
) implements DialogOption<RandomDialogOption> {

	public static final MapCodec<RandomDialogOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			CodecRegistry.CONDITION.codec().listOf().fieldOf("conditions").forGetter(RandomDialogOption::conditions),
			Codec.STRING.fieldOf("text").forGetter(RandomDialogOption::text),
			WeightedEntry.CODEC.listOf().fieldOf("entries").forGetter(RandomDialogOption::entries)
	).apply(i, RandomDialogOption::new));

	public WeightedEntry roll(RandomSource random) {
		int total = 0;
		for (var e : entries) {
			total += Math.max(0, e.weight());
		}
		if (total <= 0) return entries.getFirst();
		int roll = random.nextInt(total);
		for (var e : entries) {
			roll -= Math.max(0, e.weight());
			if (roll < 0) return e;
		}
		return entries.getLast();
	}

	@Override
	public MapCodec<RandomDialogOption> codec() {
		return CODEC;
	}

	@Override
	public Component display() {
		return Component.translatable(text());
	}

	@Override
	public @Nullable OptionResult resolve(@Nullable RandomSource random) {
		if (random == null) return null;
		return roll(random);
	}

}

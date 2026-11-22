package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A topic whose answer is chosen by the conditions that hold on click: the first entry whose
 * conditions pass wins. An entry without conditions is the fallback, so the answer never falls
 * through to nothing.
 */
public record ConditionalDialogOption(
		String text,
		List<ConditionalEntry> entries
) implements DialogOption<ConditionalDialogOption> {

	public static final MapCodec<ConditionalDialogOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("text").forGetter(ConditionalDialogOption::text),
			ConditionalEntry.CODEC.listOf().fieldOf("entries").forGetter(ConditionalDialogOption::entries)
	).apply(i, ConditionalDialogOption::new));

	@Override
	public MapCodec<ConditionalDialogOption> codec() {
		return CODEC;
	}

	@Override
	public List<QuestCondition<?>> conditions() {
		return List.of();
	}

	@Override
	public Component display() {
		return Component.translatable(text());
	}

	@Override
	public @Nullable OptionResult resolve(ServerPlayer sp, YoukaiEntity ch, @Nullable RandomSource random) {
		for (var e : entries) {
			if (e.match(sp, ch)) return e;
		}
		return null;
	}

}

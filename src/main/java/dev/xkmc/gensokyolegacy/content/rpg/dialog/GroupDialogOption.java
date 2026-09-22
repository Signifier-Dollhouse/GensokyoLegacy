package dev.xkmc.gensokyolegacy.content.rpg.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.rpg.action.DialogAction;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestCondition;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record GroupDialogOption(
		String group,
		String text,
		List<DialogAction<?>> actions,
		Optional<Holder<Dialog>> next
) implements DialogOption<GroupDialogOption>, OptionResult {

	public static final MapCodec<GroupDialogOption> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("group").forGetter(GroupDialogOption::group),
			Codec.STRING.fieldOf("text").forGetter(GroupDialogOption::text),
			CodecRegistry.ACTION.codec().listOf().fieldOf("actions").forGetter(GroupDialogOption::actions),
			Dialog.HOLDER.optionalFieldOf("next").forGetter(GroupDialogOption::next)
	).apply(i, GroupDialogOption::new));

	@Override
	public MapCodec<GroupDialogOption> codec() {
		return CODEC;
	}

	@Override
	public List<QuestCondition<?>> conditions() {
		return List.of();
	}

	@Override
	public String groupKey() {
		return group;
	}

	@Override
	public Component display() {
		return Component.translatable(text());
	}

	@Override
	public OptionResult resolve(@Nullable RandomSource random) {
		return this;
	}

}
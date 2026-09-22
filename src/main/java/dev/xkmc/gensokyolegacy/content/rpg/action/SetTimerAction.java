package dev.xkmc.gensokyolegacy.content.rpg.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;

public record SetTimerAction(String key, int delay) implements DialogAction<SetTimerAction> {

	public static final MapCodec<SetTimerAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("key").forGetter(SetTimerAction::key),
			Codec.INT.fieldOf("delay").forGetter(SetTimerAction::delay)
	).apply(i, SetTimerAction::new));

	@Override
	public void execute(ActionContext context) {
		var holder = CharDataHolder.get(context.sp(), context.character());
		long next = context.sp().level().getGameTime() + delay;
		holder.data().setTimer(key, next);
		holder.sync();
	}

	@Override
	public MapCodec<SetTimerAction> codec() {
		return CODEC;
	}

}

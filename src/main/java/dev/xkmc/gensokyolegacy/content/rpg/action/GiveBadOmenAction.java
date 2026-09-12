package dev.xkmc.gensokyolegacy.content.rpg.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public record GiveBadOmenAction(int duration) implements DialogAction<GiveBadOmenAction> {

	public static final MapCodec<GiveBadOmenAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("duration").forGetter(GiveBadOmenAction::duration)
	).apply(i, GiveBadOmenAction::new));

	@Override
	public void execute(ActionContext context) {
		context.sp().addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, duration, 0));
	}

	@Override
	public MapCodec<GiveBadOmenAction> codec() {
		return CODEC;
	}

}
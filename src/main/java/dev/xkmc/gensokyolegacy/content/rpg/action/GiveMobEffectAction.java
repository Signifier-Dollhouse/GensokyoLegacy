package dev.xkmc.gensokyolegacy.content.rpg.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public record GiveMobEffectAction(Holder<MobEffect> effect, int duration, int amplifier) implements DialogAction<GiveMobEffectAction> {

	public static final MapCodec<GiveMobEffectAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(GiveMobEffectAction::effect),
			Codec.INT.fieldOf("duration").forGetter(GiveMobEffectAction::duration),
			Codec.INT.optionalFieldOf("amplifier", 0).forGetter(GiveMobEffectAction::amplifier)
	).apply(i, GiveMobEffectAction::new));

	@Override
	public void execute(ActionContext context) {
		context.sp().addEffect(new MobEffectInstance(effect, duration, amplifier));
	}

	@Override
	public MapCodec<GiveMobEffectAction> codec() {
		return CODEC;
	}

}
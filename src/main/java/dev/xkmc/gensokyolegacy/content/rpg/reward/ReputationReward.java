package dev.xkmc.gensokyolegacy.content.rpg.reward;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestReward;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerPlayer;

public record ReputationReward(int reputation, int softCap, int capIncrease, int maxCap) implements QuestReward<ReputationReward> {

	public static final MapCodec<ReputationReward> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("reputation").forGetter(ReputationReward::reputation),
			Codec.INT.fieldOf("soft_cap").forGetter(ReputationReward::softCap),
			Codec.INT.fieldOf("cap_increase").forGetter(ReputationReward::capIncrease),
			Codec.INT.fieldOf("max_cap").forGetter(ReputationReward::maxCap)

	).apply(i, ReputationReward::new));

	@Override
	public MapCodec<ReputationReward> codec() {
		return CODEC;
	}

	@Override
	public void execute(ServerPlayer sp, YoukaiEntity ch) {
		GLMeta.CHAR.type().getOrCreate(sp).get(sp, ch).gain(reputation, softCap, capIncrease, maxCap);
	}

}

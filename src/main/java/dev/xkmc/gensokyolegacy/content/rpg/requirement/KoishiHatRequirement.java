package dev.xkmc.gensokyolegacy.content.rpg.requirement;

import com.mojang.serialization.MapCodec;
import dev.xkmc.gensokyolegacy.content.rpg.quest.QuestData;
import dev.xkmc.gensokyolegacy.content.rpg.trigger.KoishiHatTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record KoishiHatRequirement() implements QuestRequirement<KoishiHatRequirement, KoishiHatTrigger> {

	public static final MapCodec<KoishiHatRequirement> CODEC = MapCodec.unit(new KoishiHatRequirement());

	@Override
	public MapCodec<KoishiHatRequirement> codec() {
		return CODEC;
	}

	@Override
	public Class<KoishiHatTrigger> getTrigger() {
		return KoishiHatTrigger.class;
	}

	@Override
	public int match(KoishiHatTrigger trigger) {
		return 1;
	}

	@Override
	public int getMaxProgress() {
		return 1;
	}

	@Override
	public List<Component> getDesc(Player player, QuestData data, String key) {
		return List.of();
	}

}

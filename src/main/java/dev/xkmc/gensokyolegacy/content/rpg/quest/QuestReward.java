package dev.xkmc.gensokyolegacy.content.rpg.quest;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecElement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface QuestReward<T extends Record & QuestReward<T>> extends CodecElement<T> {

	void execute(ServerPlayer sp, YoukaiEntity ch);

	/** what the reward is worth, for surfaces that have no inventory to hand (JEI) */
	Component getDesc();

}

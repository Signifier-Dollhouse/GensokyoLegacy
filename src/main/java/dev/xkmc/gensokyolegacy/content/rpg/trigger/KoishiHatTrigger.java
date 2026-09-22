package dev.xkmc.gensokyolegacy.content.rpg.trigger;

import net.minecraft.server.level.ServerPlayer;

public record KoishiHatTrigger(ServerPlayer player) implements QuestTrigger<KoishiHatTrigger> {
}

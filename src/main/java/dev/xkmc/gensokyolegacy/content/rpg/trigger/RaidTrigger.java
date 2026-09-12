package dev.xkmc.gensokyolegacy.content.rpg.trigger;

import net.minecraft.server.level.ServerPlayer;

public record RaidTrigger(ServerPlayer player) implements QuestTrigger<RaidTrigger> {
}
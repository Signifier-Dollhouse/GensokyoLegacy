package dev.xkmc.gensokyolegacy.content.client.debug;

import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.UUID;

public record DoorRequestToServer(UUID id) implements SerialPacketBase<DoorRequestToServer> {

	@Override
	public void handle(Player player) {
		if (!(player instanceof ServerPlayer sp)) return;
		if (!(sp.serverLevel().getEntity(id) instanceof YoukaiEntity e)) return;
		ArrayList<Component> doors = new ArrayList<>();
		if (e instanceof SmartYoukaiEntity smart) {
			var set = BrainUtils.getMemory(smart, MemoryModuleType.DOORS_TO_CLOSE);
			if (set != null) {
				for (GlobalPos gpos : set) {
					doors.add(Component.literal("%s %s".formatted(gpos.dimension().location(),
							gpos.pos().toShortString())).withStyle(ChatFormatting.GRAY));
				}
			}
		}
		GensokyoLegacy.HANDLER.toClientPlayer(CharacterInfoToClient.ofDoor(doors), sp);
	}

}

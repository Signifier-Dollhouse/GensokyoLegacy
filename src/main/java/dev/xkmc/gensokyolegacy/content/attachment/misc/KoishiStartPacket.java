package dev.xkmc.gensokyolegacy.content.attachment.misc;

import dev.xkmc.l2serial.network.RecordPacketBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public record KoishiStartPacket(Type type, Vec3 pos) implements RecordPacketBase {

	public enum Type {
		START,
		PARTICLE
	}

	@Override
	public void handle(Player player) {
		ClientCapHandler.koishiAttack(type, pos);
	}

}

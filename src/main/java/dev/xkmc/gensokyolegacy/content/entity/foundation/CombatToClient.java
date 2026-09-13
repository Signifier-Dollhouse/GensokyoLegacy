package dev.xkmc.gensokyolegacy.content.entity.foundation;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

public record CombatToClient(
		int id, DamageRefactorEntity.CombatData data
) implements SerialPacketBase<CombatToClient> {

	public static void send(DamageRefactorEntity e) {
		GensokyoLegacy.HANDLER.toTrackingPlayers(new CombatToClient(e.getId(), e.getCombatData()), e);
	}

	@Override
	public void handle(Player player) {
		if (player.level().getEntity(id) instanceof DamageRefactorEntity e) {
			e.applyData(data);
		}
	}

}
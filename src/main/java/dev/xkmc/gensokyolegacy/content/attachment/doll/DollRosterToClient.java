package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-to-owner summoned-doll roster (glove.md §2c). Full-replace, in ledger
 * order: the sidebar lists one row per entry. Sent only when the roster
 * actually changed (see {@code DollAttachment.maybePushRoster}); the
 * capability itself syncs nothing to clients.
 *
 * <p>Entries carry the live entity id for O(1) client lookup plus the ledger
 * uuid to guard against id reuse after a discard/resummon: the client drops
 * entries whose entity is missing or whose uuid mismatches.
 */
public record DollRosterToClient(ArrayList<Entry> roster) implements SerialPacketBase<DollRosterToClient> {

	public record Entry(int id, UUID uuid) {
	}

	@Override
	public void handle(Player player) {
		GLMeta.DOLL.type().getOrCreate(player).setRoster(roster);
	}

}

package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-to-owner summoned-doll roster, full-replace in ledger order, sent only when it changed
 * (glove.md §2c) since the capability itself syncs nothing. Entries carry the live entity id for
 * O(1) client lookup plus the ledger uuid, which guards against id reuse after a
 * discard/resummon.
 *
 * @see doc/design/doll/glove.md
 */
public record DollRosterToClient(ArrayList<Entry> roster) implements SerialPacketBase<DollRosterToClient> {

	public record Entry(int id, UUID uuid) {
	}

	@Override
	public void handle(Player player) {
		GLMeta.DOLL.type().getOrCreate(player).setRoster(roster);
	}

}

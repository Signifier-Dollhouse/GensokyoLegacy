package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2serial.network.SerialPacketBase;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-to-owner doll roster, full-replace in ledger order, sent only on change (glove.md §2c);
 * the capability itself syncs nothing. Each entry pairs the entity id for O(1) lookup with the
 * ledger uuid, which guards id reuse after a resummon.
 *
 * @see "doc/design/doll/glove.md"
 */
public record DollRosterToClient(ArrayList<Entry> roster) implements SerialPacketBase<DollRosterToClient> {

	public record Entry(int id, UUID uuid) {
	}

	@Override
	public void handle(Player player) {
		GLMeta.DOLL.type().getOrCreate(player).setRoster(roster);
	}

}

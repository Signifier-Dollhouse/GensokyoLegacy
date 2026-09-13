package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollState;
import dev.xkmc.gensokyolegacy.content.attachment.doll.StrayHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;

/**
 * Stray cut: detaches the ledger entry (no item produced) and hosts it transiently.
 * From here commands can't reach the doll, itemize no-ops, and only death ends it
 * (dropping the item form). The detached inventory is already current — all server
 * mutations are ledger-direct. Fully defaulted — the entity inherits it as-is.
 */
public interface DollStray extends DollBaseImpl {

	default void becomeStray() {
		DollEntity doll = asDoll();
		if (doll.strayHost() != null) return;
		DollHost host = doll.getHost();
		DollData data = host == null ? null : host.detach(doll.getUUID());
		if (data == null) {
			data = new DollData();
			data.uuid = doll.getUUID();
			data.state = DollState.SUMMONED;
			for (DollSlot slot : DollSlot.values())
				data.inventory.set(slot, doll.getLoadoutItem(slot));
		}
		doll.writeValuesTo(data);
		doll.setStrayHost(new StrayHost(data));
	}

}

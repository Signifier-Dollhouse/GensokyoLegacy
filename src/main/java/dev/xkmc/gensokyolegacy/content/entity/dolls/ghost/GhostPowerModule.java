package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollModule;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;

/**
 * Carries the ghost's power through the ledger's park/resummon round trip.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
final class GhostPowerModule implements DollModule {

	@Override
	public void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(GhostEntity.DATA_POWER, GhostPower.MAX);
	}

	@Override
	public void writeValuesTo(SynchedEntityData sync, DollData data) {
		data.power = sync.get(GhostEntity.DATA_POWER);
	}

	@Override
	public void readValuesFrom(SynchedEntityData sync, DollData data) {
		sync.set(GhostEntity.DATA_POWER, Mth.clamp(data.power, 0, GhostPower.MAX));
	}

}

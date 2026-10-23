package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollModule;
import net.minecraft.network.syncher.SynchedEntityData;

/**
 * Carries the variant through the ledger's park/resummon round trip. The client reads the same
 * value from the entity's synced accessor, never from the server-only {@link DollData}.
 */
final class GhostVariantModule implements DollModule {

	@Override
	public void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(GhostEntity.DATA_VARIANT, GhostVariant.CURIOS.ordinal());
	}

	@Override
	public void writeValuesTo(SynchedEntityData sync, DollData data) {
		data.variant = sync.get(GhostEntity.DATA_VARIANT);
	}

	@Override
	public void readValuesFrom(SynchedEntityData sync, DollData data) {
		sync.set(GhostEntity.DATA_VARIANT, GhostVariant.byId(data.variant).ordinal());
	}

}

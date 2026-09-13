package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

/**
 * Shared base for all doll impl interfaces. Exposes the implementing entity in a
 * typed way, so feature interfaces inherit {@link #asDoll()} instead of each declaring
 * their own cast. Named to avoid clashing with NeoForge's {@code self()} defaults.
 * The entity itself needs no additional code for this.
 */
public interface DollBaseImpl {

	default DollEntity asDoll() {
		return (DollEntity) this;
	}

	/**
	 * The vanilla synced data, redeclared here so defaults can read and write the
	 * entity accessors. Implemented by vanilla {@code Entity} — the doll inherits it
	 * with no additional code.
	 */
	SynchedEntityData getEntityData();

}

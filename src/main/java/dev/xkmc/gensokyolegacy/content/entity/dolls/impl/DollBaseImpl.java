package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;

/**
 * Shared base for all doll impl interfaces: exposes the implementing entity in a typed way so
 * feature interfaces inherit {@link #asDoll()} instead of each declaring their own cast. Named to
 * avoid clashing with NeoForge's {@code self()} defaults.
 */
public interface DollBaseImpl {

	default DollEntity asDoll() {
		return (DollEntity) this;
	}

	/** The vanilla synced data, redeclared here so defaults can read the accessors. */
	SynchedEntityData getEntityData();

}

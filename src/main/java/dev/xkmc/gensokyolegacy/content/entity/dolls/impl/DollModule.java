package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

/**
 * A pluggable doll data slice: synced-accessor definitions plus {@link DollData}
 * persistence. Instances are created per entity and kept in the base's module list;
 * each hierarchy level appends its own in {@code createDollModules(List)}, chaining
 * through {@code super}.
 *
 * <p>Construction timing: {@code defineSynchedData} runs inside the vanilla
 * {@code Entity} constructor, before any subclass state exists. Registration must
 * therefore be static-safe (fresh instances, no instance state); the synced snapshot
 * (not the entity) is passed in, so modules never need an entity reference.
 */
public interface DollModule {

	default void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	default void writeValuesTo(SynchedEntityData sync, DollData data) {
	}

	default void readValuesFrom(SynchedEntityData sync, DollData data) {
	}

	/**
	 * Player interaction, dispatched in module order; first non-{@code PASS} wins. Runs
	 * on both sides — client predicts, server executes.
	 */
	default InteractionResult interact(Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

}

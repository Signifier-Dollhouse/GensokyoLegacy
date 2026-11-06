package dev.xkmc.gensokyolegacy.content.entity.youkai.impl;

import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationState;
import dev.xkmc.gensokyolegacy.content.block.deco.bed.YoukaiBedBlock;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.HomeModule;
import dev.xkmc.gensokyolegacy.content.entity.foundation.IDimensionBoundEntity;
import dev.xkmc.gensokyolegacy.content.entity.foundation.ISleepOffsetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

public interface YoukaiIntrinsics extends YoukaiBaseImpl, ISleepOffsetEntity, IDimensionBoundEntity {

	// intrinsic flags

	default boolean mayFly() {
		return true;
	}

	/**
	 * Whether she ever settles onto the ground. A hovering character has no walkable start node - a walk node
	 * wants solid ground under the feet and hers is a block of air - so landing her would leave her unable to
	 * path anywhere, and she would have to climb again next tick.
	 */
	default boolean mayLand() {
		return true;
	}

	default boolean vanishOutOfSight() {
		return true;
	}

	default boolean isNamed() {
		return true;
	}

	// youkai states

	/**
	 * Whether this character is visiting someone else's home. A visitor has no {@link HomeModule} binding,
	 * so it can neither sleep nor claim a bed, and the quest/dialog layer treats it as a guest.
	 */
	default boolean isVisiting() {
		return getFlag(YoukaiFlags.VISITING);
	}

	/**
	 * Whether this character is a guest at somebody's party. Like a visitor she has
	 * no {@link HomeModule} binding, and the quest/dialog layer treats her as a guest.
	 */
	default boolean isPartying() {
		return getFlag(YoukaiFlags.PARTYING);
	}

	@Override
	default double getSleepOffset(BlockPos pos) {
		if (asYoukai().level().isLoaded(pos)) {
			return YoukaiBedBlock.getSleepOffset(asYoukai().level().getBlockState(pos));
		}
		return YoukaiBedBlock.VANILLA_SLEEP_OFFSET;
	}

	default ReputationState getReputation(LivingEntity le) {
		return getData(le).map(e -> e.data().getState()).orElse(ReputationState.STRANGER);
	}

}

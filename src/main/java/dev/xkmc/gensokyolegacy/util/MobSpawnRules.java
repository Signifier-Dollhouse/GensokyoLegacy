package dev.xkmc.gensokyolegacy.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;

/**
 * Shared natural-spawn gate: animals-spawnable ground under a lit spot, the rule the plain fairy
 * and the barrens ghost both follow.
 */
public final class MobSpawnRules {

	public static boolean onLitAnimalGround(EntityType<?> type, LevelAccessor level, MobSpawnType spawnType,
											BlockPos pos, RandomSource random) {
		boolean lit = ignoresLightRequirements(spawnType) || level.getRawBrightness(pos, 0) > 8;
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && lit;
	}

	/**
	 * 1.21's {@code MobSpawnType#ignoresLightRequirements}; 1.20.1 has no such helper.
	 */
	private static boolean ignoresLightRequirements(MobSpawnType type) {
		return switch (type) {
			case SPAWNER, COMMAND, EVENT, TRIGGERED, JOCKEY, REINFORCEMENT, MOB_SUMMONED, DISPENSER -> true;
			default -> false;
		};
	}

	private MobSpawnRules() {
	}

}

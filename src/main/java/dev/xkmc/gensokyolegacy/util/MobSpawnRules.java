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
		boolean lit = MobSpawnType.ignoresLightRequirements(spawnType) || level.getRawBrightness(pos, 0) > 8;
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && lit;
	}

	private MobSpawnRules() {
	}

}

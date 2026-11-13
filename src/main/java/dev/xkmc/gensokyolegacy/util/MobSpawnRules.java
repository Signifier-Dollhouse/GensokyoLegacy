package dev.xkmc.gensokyolegacy.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Shared natural-spawn gates: animals-spawnable ground under a lit spot for the fairy and the
 * barrens ghost, or vanilla's hostile darkness rule for the wild stray doll.
 */
public final class MobSpawnRules {

	public static boolean onLitAnimalGround(EntityType<?> type, LevelAccessor level, MobSpawnType spawnType,
											BlockPos pos, RandomSource random) {
		boolean lit = MobSpawnType.ignoresLightRequirements(spawnType) || level.getRawBrightness(pos, 0) > 8;
		return level.getBlockState(pos.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && lit;
	}

	public static boolean hostileGround(EntityType<? extends Mob> type, ServerLevelAccessor level,
										MobSpawnType spawnType, BlockPos pos, RandomSource random) {
		return level.getDifficulty() != Difficulty.PEACEFUL
				&& (MobSpawnType.ignoresLightRequirements(spawnType) || Monster.isDarkEnoughToSpawn(level, pos, random))
				&& Mob.checkMobSpawnRules(type, level, spawnType, pos, random);
	}

	private MobSpawnRules() {
	}

}

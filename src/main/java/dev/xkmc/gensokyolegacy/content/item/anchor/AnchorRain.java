package dev.xkmc.gensokyolegacy.content.item.anchor;

import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorBulletEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The storm mode's sneak-right-click: anchors fall on every enemy that can see the player or is
 * hunting them, padded with random points around the player when there are too few targets.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public final class AnchorRain {

	private static final double RANGE = 32;
	private static final double MIN_GAP = 3;
	private static final int MAX_ANCHORS = 32;
	private static final int MIN_ANCHORS = 10;
	private static final int DROP_MIN = 24;
	private static final int DROP_MAX = 32;
	private static final int FILL_TRIES = 128;
	private static final double FALL_SPEED = 1.5;
	private static final int LIFE = 200;

	private AnchorRain() {
	}

	public static void rain(ServerPlayer sp) {
		for (BlockPos target : select(sp))
			drop(sp, target);
	}

	/**
	 * Target points first, selected at random and kept at least {@link #MIN_GAP} apart, then random
	 * points around the player until there are {@link #MIN_ANCHORS} of them.
	 */
	private static List<BlockPos> select(ServerPlayer sp) {
		ServerLevel level = sp.serverLevel();
		List<BlockPos> candidates = new ArrayList<>();
		for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class,
				sp.getBoundingBox().inflate(RANGE))) {
			if (!le.isAlive() || le == sp) continue;
			if (le.distanceToSqr(sp) > RANGE * RANGE) continue;
			boolean hunting = le instanceof Mob mob && mob.getTarget() == sp;
			boolean seen = le instanceof Enemy && le.hasLineOfSight(sp);
			if (hunting || seen) candidates.add(le.blockPosition());
		}
		RandomSource rand = level.getRandom();
		for (int i = candidates.size() - 1; i > 0; i--) {
			int j = rand.nextInt(i + 1);
			BlockPos tmp = candidates.get(i);
			candidates.set(i, candidates.get(j));
			candidates.set(j, tmp);
		}
		List<BlockPos> points = new ArrayList<>();
		for (BlockPos pos : candidates) {
			if (points.size() >= MAX_ANCHORS) break;
			if (tooClose(points, pos)) continue;
			points.add(pos);
		}
		for (int i = 0; i < FILL_TRIES && points.size() < MIN_ANCHORS; i++) {
			BlockPos pos = randomPoint(sp, rand);
			if (!tooClose(points, pos)) points.add(pos);
		}
		return points;
	}

	private static boolean tooClose(List<BlockPos> points, BlockPos pos) {
		for (BlockPos other : points)
			if (other.distSqr(pos) < MIN_GAP * MIN_GAP) return true;
		return false;
	}

	private static BlockPos randomPoint(ServerPlayer sp, RandomSource rand) {
		double angle = rand.nextDouble() * Math.PI * 2;
		double dist = RANGE * Math.sqrt(rand.nextDouble());
		int x = Mth.floor(sp.getX() + Math.cos(angle) * dist);
		int z = Mth.floor(sp.getZ() + Math.sin(angle) * dist);
		return new BlockPos(x, sp.getBlockY(), z);
	}

	/**
	 * Drops one anchor from 24-32 blocks above the point, or from just under the first blocking
	 * block above it when the point is roofed over.
	 */
	private static void drop(ServerPlayer sp, BlockPos target) {
		ServerLevel level = sp.serverLevel();
		int height = DROP_MIN + level.getRandom().nextInt(DROP_MAX - DROP_MIN + 1);
		BlockPos spawn = target.above(height);
		for (int dy = 1; dy <= height; dy++) {
			BlockPos pos = target.above(dy);
			if (!level.isLoaded(pos)) break;
			if (level.getBlockState(pos).blocksMotion()) {
				spawn = pos.below();
				break;
			}
		}
		Vec3 at = Vec3.atCenterOf(spawn);
		AnchorBulletEntity proj = new AnchorBulletEntity(GLEntities.ANCHOR_BULLET.get(), sp, level);
		proj.setItem(new ItemStack(GLItems.MINAMITSU_ANCHOR.get()));
		proj.configure(at);
		proj.setup(0, LIFE, false, false, new Vec3(0, -FALL_SPEED, 0));
		proj.moveTo(at);
		level.addFreshEntity(proj);
		SpellContainer.track(sp, proj);
	}

}

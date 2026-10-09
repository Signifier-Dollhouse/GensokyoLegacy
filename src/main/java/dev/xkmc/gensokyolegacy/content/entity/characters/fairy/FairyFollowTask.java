package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.phys.Vec3;

/**
 * Following someone, at a distance that means something for a fairy who hovers.
 * <p>
 * Vanilla measures the standoff to the player's <em>centre</em>, which for her is a
 * block above their head - so any threshold small enough to be described as "she
 * should keep a block's distance" is already satisfied while she is directly
 * overhead, and she ends up sitting on top of them. Only the approach is worth
 * keeping from the stock behavior; the distance test is re-done here, flat, so one
 * block means one block on the ground rather than one block in whatever direction
 * happens to be nearest.
 * <p>
 * The walk target it writes keeps vanilla's own value of 2, which
 * {@code YoukaiMoveTask} counts in Manhattan blocks: with the block of height
 * between them that lands at about a block of ground clearance, which is the shape
 * wanted here.
 */
public class FairyFollowTask extends FollowTemptation {

	/**
	 * How close on the ground she is willing to come.
	 */
	private static final int CLOSE_ENOUGH = 4;

	public FairyFollowTask() {
		super(entity -> 1f);
	}

	/**
	 * The supertype's exact signature: narrowing the parameter would make this an
	 * overload rather than an override, so the cast belongs here.
	 */
	@Override
	protected void tick(ServerLevel level, PathfinderMob owner, long gameTime) {
		var entity = (PlainFairyEntity) owner;
		var player = entity.getBrain().getMemory(MemoryModuleType.TEMPTING_PLAYER).orElse(null);
		if (player == null) return;
		entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true));
		if (entity.position().subtract(player.position()).lengthSqr() < CLOSE_ENOUGH * CLOSE_ENOUGH) {
			entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
			entity.setDeltaMovement(entity.getDeltaMovement().scale(0.7));
			return;
		}
		entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
				new WalkTarget(new EntityTracker(player, false), 1f, CLOSE_ENOUGH));
	}

}
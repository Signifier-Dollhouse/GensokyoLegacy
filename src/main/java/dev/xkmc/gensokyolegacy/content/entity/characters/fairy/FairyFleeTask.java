package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import com.google.common.collect.ImmutableMap;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Panic, in the shape vanilla gives it: what {@code AvoidEntityGoal} does when a
 * cow or a sheep decides you are a threat.
 * <p>
 * Deliberately not routed through {@code WALK_TARGET} the way the other
 * characters move. She hovers, so a walk route has no walkable start node and the
 * memory-driven move task spends its time failing to reach her own doorstep. A
 * passive mob sidesteps all of that by pathing once and handing the result
 * straight to the navigation, and so does this - through her flying navigation,
 * which is the only one that can path from where she actually is.
 * <p>
 * She re-picks rather than committing to one bearing, which is what makes it read
 * as running around instead of bolting in a straight line, and she stops once the
 * attacker is far enough behind to no longer be worth running from.
 */
public class FairyFleeTask extends Behavior<PlainFairyEntity> {

	/** How far out she aims to get, and how far off "straight away" a bearing may fall. */
	private static final double FLEE_DISTANCE = 16.0;
	private static final double FLEE_SPREAD = 0.6;

	/** Ticks between re-picks. Short, because she is meant to keep changing her mind. */
	private static final int REPATH_INTERVAL = 10;
	/** Bearings tried before giving up on this spot entirely - a cliff, a wall. */
	private static final int SPOT_ATTEMPTS = 3;

	/** Past this much separation she settles down instead of running. */
	private static final double GIVE_UP_DISTANCE = 24.0;

	/**
	 * Unused as a speed, but the route still has to be handed over with one. The
	 * flying move control takes her speed off the FLYING_SPEED attribute whenever
	 * she is airborne - which she always is - so the modifier here changes nothing
	 * about how fast she goes; see {@link PlainFairyEntity#FLEE_MULTIPLIER} for
	 * where the panic speed actually lives.
	 */
	private static final float ROUTE_SPEED = 1.0f;

	/** Share of her health restored every {@link #HEAL_INTERVAL} ticks while she runs. */
	private static final int HEAL_INTERVAL = 10;
	private static final float HEAL_FRACTION = 0.1f;

	public FairyFleeTask() {
		super(ImmutableMap.of(
				MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
				MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT,
				GLBrains.MEM_FEAR.get(), MemoryStatus.VALUE_PRESENT
		), 0, 0);
	}

	@Override
	protected boolean checkExtraStartConditions(ServerLevel level, PlainFairyEntity entity) {
		return attacker(entity) != null;
	}

	/**
	 * She keeps running for as long as she remembers the hit, even once she has put
	 * the ground between them - the fear memory's own expiry is what ends it, not the
	 * attacker going out of sight.
	 * <p>
	 * The activity check is load-bearing, not decoration. Changing activity does not
	 * stop behaviors - {@code Brain} only sets the new activity and moves on - so a
	 * behavior has to notice itself being outranked. Without it, being handed a cake
	 * would leave her sprinting away for the rest of the memory's life with both
	 * tasks writing to the same navigation.
	 */
	@Override
	protected boolean canStillUse(ServerLevel level, PlainFairyEntity entity, long gameTime) {
		return entity.getActivity() == GLBrains.FEAR.get();
	}

	@Override
	protected void start(ServerLevel level, PlainFairyEntity entity, long gameTime) {
		entity.setFleeing(true);
		run(level, entity);
	}

	@Override
	protected void tick(ServerLevel level, PlainFairyEntity entity, long gameTime) {
		var attacker = attacker(entity);
		if (attacker == null) return;
		if (gameTime % HEAL_INTERVAL == 0) {
			entity.heal(entity.getMaxHealth() * HEAL_FRACTION);
		}
		if (gameTime % REPATH_INTERVAL != 0) return;
		if (entity.distanceToSqr(attacker) > GIVE_UP_DISTANCE * GIVE_UP_DISTANCE) return;
		run(level, entity);
	}

	@Override
	protected void stop(ServerLevel level, PlainFairyEntity entity, long gameTime) {
		entity.setFleeing(false);
		entity.getNavigation().stop();
	}

	/**
	 * Point her at a spot behind her, and hand the route over. A bearing that does
	 * not path anywhere gets another go with a different one - a single unlucky
	 * direction must not be the difference between running and standing still.
	 */
	private static void run(ServerLevel level, PlainFairyEntity entity) {
		var attacker = attacker(entity);
		if (attacker == null) return;
		var nav = entity.navCtrl.flying();
		for (int attempt = 0; attempt < SPOT_ATTEMPTS; attempt++) {
			var path = nav.createPath(Set.of(spot(level, entity, attacker)), 18);
			if (path != null && nav.moveTo(path, ROUTE_SPEED)) return;
		}
		entity.getNavigation().stop();
	}

	@Nullable
	private static LivingEntity attacker(PlainFairyEntity entity) {
		var attacker = BrainUtils.getMemory(entity, GLBrains.MEM_FEAR.get());
		if (attacker == null || !attacker.isAlive() || attacker.level() != entity.level()) return null;
		return attacker;
	}

	/**
	 * Open ground behind her, on a bearing nudged off dead-straight so that a run
	 * curves rather than tracing a line. One block up off the heightmap, because
	 * that is where she hovers - and a flying node wants air, not the floor she is
	 * hovering over.
	 */
	private static BlockPos spot(ServerLevel level, PlainFairyEntity entity, LivingEntity attacker) {
		double away = Math.atan2(entity.getZ() - attacker.getZ(), entity.getX() - attacker.getX());
		double bearing = away + (entity.getRandom().nextDouble() - 0.5) * 2 * FLEE_SPREAD;
		double x = entity.getX() + Math.cos(bearing) * FLEE_DISTANCE;
		double z = entity.getZ() + Math.sin(bearing) * FLEE_DISTANCE;
		var column = BlockPos.containing(x, entity.getY(), z);
		return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column).above();
	}

}
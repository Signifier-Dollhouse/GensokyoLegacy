package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.behavior.module.FairyFeedModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.DynamicSensor;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.Set;

/**
 * Who she is drawn to, if anyone: the player holding something she eats - a cookie
 * or a cake, per {@code FairyFeedModule#isFood} - or simply someone she is not
 * hostile towards, which is enough to be worth walking over to.
 * <p>
 * Either way it is a {@code FollowTemptation} away, with the cooldown half of that
 * being {@code CountDownCooldownTicks}: vanilla always registers that alongside and
 * it has to be registered here too, or the whole temptation can only ever happen once.
 * <p>
 * Writes vanilla's {@code TEMPTING_PLAYER} rather than a memory of our own so the
 * pipeline is reused rather than rebuilt. Being the <em>only</em> writer of that
 * memory is deliberate: two sensors both claiming it would race. Typed to
 * {@link FairyEntity} because what draws her to somebody is the same for every fairy.
 */
public class FairyInterestSensor extends DynamicSensor<FairyEntity> {

	/**
	 * Vanilla's own temptation range, and the point past which she loses interest.
	 */
	private static final double RANGE = 10.0;

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(MemoryModuleType.TEMPTING_PLAYER);
	}

	@Override
	protected void doTick(ServerLevel level, FairyEntity entity) {
		// While she is still busy with the last thing she was fed she is drawn to
		// nobody at all. Said once per scan, not once per player in range.
		if (!availableForTemptation(entity)) {
			BrainUtils.clearMemory(entity, MemoryModuleType.TEMPTING_PLAYER);
			return;
		}
		var target = level.players().stream()
				.filter(EntitySelector.NO_SPECTATORS)
				.filter(player -> entity.closerThan(player, RANGE) && entity.hasLineOfSight(player))
				.filter(player -> drawsHer(entity, player))
				.min(Comparator.comparingDouble(entity::distanceToSqr));
		if (target.isEmpty()) {
			BrainUtils.clearMemory(entity, MemoryModuleType.TEMPTING_PLAYER);
			return;
		}
		BrainUtils.setMemory(entity, MemoryModuleType.TEMPTING_PLAYER, target.get());
	}

	/**
	 * Whether she will follow anyone at all yet. The same cooldown that keeps her
	 * from being farmed keeps her from trailing the player with her hand out for a
	 * treat she cannot have.
	 */
	private static boolean availableForTemptation(FairyEntity entity) {
		return entity.getModule(FairyFeedModule.class)
				.map(FairyFeedModule::getCoolDown)
				.map(coolDown -> coolDown == 0)
				.orElse(true);
	}

	private static boolean drawsHer(FairyEntity entity, Player player) {
		return FairyFeedModule.isFood(player.getMainHandItem())
				|| FairyFeedModule.isFood(player.getOffhandItem());
	}

}
package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.DynamicSensor;
import dev.xkmc.gensokyolegacy.content.entity.module.FairyCakeModule;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Comparator;
import java.util.Set;

/**
 * Who she is drawn to, if anyone. Two reasons, and cake wins:
 * <ul>
 * <li>the player is holding cake, which is the thing she actually wants;</li>
 * <li>or they are simply someone she is not hostile towards, which is enough to be
 * worth walking over to.</li>
 * </ul>
 * Either way it is a {@code FollowTemptation} away: walking to them, looking at them,
 * backing off when they leave the area and then not coming straight back - the
 * cooldown half of that being {@code CountDownCooldownTicks}, which vanilla always
 * registers alongside and which has to be registered here too or the whole
 * temptation can only ever happen once.
 * <p>
 * Writes vanilla's {@code TEMPTING_PLAYER} rather than a memory of our own so the
 * pipeline is reused rather than rebuilt. Being the <em>only</em> writer of that
 * memory is deliberate: two sensors both claiming it would race, and whichever ran
 * second would erase the other's find.
 */
public class FairyInterestSensor extends DynamicSensor<PlainFairyEntity> {

	/**
	 * Vanilla's own temptation range, and the point past which she loses interest.
	 */
	private static final double RANGE = 10.0;

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(MemoryModuleType.TEMPTING_PLAYER);
	}

	@Override
	protected void doTick(ServerLevel level, PlainFairyEntity entity) {
		// While she is still busy with the last cake she is drawn to nobody at all:
		// not the player holding one, and not an interesting person either. Said once
		// per scan rather than once per player in range, because it is not about any
		// particular one of them.
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
	 * from being farmed keeps her from trailing the player around with her hand out
	 * for a slice she cannot have.
	 */
	private static boolean availableForTemptation(PlainFairyEntity entity) {
		return entity.getModule(FairyCakeModule.class)
				.map(FairyCakeModule::getCoolDown)
				.map(coolDown -> coolDown == 0)
				.orElse(true);
	}

	private static boolean drawsHer(PlainFairyEntity entity, Player player) {
		if (FairyCakeModule.isCake(player.getMainHandItem())
				|| FairyCakeModule.isCake(player.getOffhandItem())) {
			return true;
		}
		return false;
	}

}
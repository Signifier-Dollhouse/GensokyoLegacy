package dev.xkmc.gensokyolegacy.content.entity.behavior.sensor;

import dev.xkmc.gensokyolegacy.content.attachment.character.ReputationConstants;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class NearbyPlayerSensor<E extends SmartYoukaiEntity> extends AbstractNearbyEntitySensor<Player, E> {

	public NearbyPlayerSensor() {
		super(MemoryModuleType.NEAREST_PLAYERS, Player.class,
				(target, entity) -> target.canBeSeenByAnyone());
	}

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of(
				MemoryModuleType.NEAREST_PLAYERS,
				MemoryModuleType.NEAREST_VISIBLE_PLAYER,
				MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER
		);
	}

	@Override
	protected void setMemory(ServerLevel level, E entity, List<Player> list) {
		list.sort(Comparator.comparingDouble(entity::distanceToSqr));
		BrainUtils.setMemory(entity, MemoryModuleType.NEAREST_PLAYERS, list);
		boolean allHostile = !list.isEmpty() && list.stream().allMatch(p ->
				entity.getData(p).map(h -> h.data().reputation)
						.orElse(ReputationConstants.INITIAL_REPUTATION) < ReputationConstants.DISCARD_REP_THRESHOLD);
		list = new ArrayList<>(list);
		list.removeIf(p -> !entity.targets.contains(p) && p.distanceTo(entity) > p.getVisibilityPercent(entity) * 16);
		BrainUtils.setMemory(entity, MemoryModuleType.NEAREST_VISIBLE_PLAYER, list.isEmpty() ? null : list.get(0));
		list = new ArrayList<>(list);
		list.removeIf(p -> !p.canBeSeenAsEnemy());
		BrainUtils.setMemory(entity, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, list.isEmpty() ? null : list.get(0));
		// a party guest is not walked out over a disagreement, and a visitor writes
		// off her window first so the scheduler does not put her straight back
		if (allHostile && entity.vanishOnDislike() && !entity.isPartying()) {
			var visit = entity.getModule(VisitModule.class);
			if (visit.isPresent()) visit.get().giveUp();
			else entity.discard();
		}
	}

}

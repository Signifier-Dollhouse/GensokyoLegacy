package dev.xkmc.gensokyolegacy.content.entity.behavior.sensor;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.entity.youkai.SmartYoukaiEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;

import java.util.Set;

/**
 * Periodically refreshes cached home block interest points around the entity,
 * so behavior start checks hit a warm cache instead of triggering a cold
 * search only when a task rolls. Ticks every 40 game ticks (2 seconds);
 * results accumulate in the shared per-kind {@code BlockSearchCache}, whose
 * own cooldown throttles full scans.
 * <p>
 * One instance is registered per block kind, next to the behavior that uses
 * it, so entities only scan for kinds they actually need.
 */
public class YoukaiHomeBlocksSensor<E extends SmartYoukaiEntity> extends Sensor<E> {

	private final HomeBlockKind[] kinds;

	public YoukaiHomeBlocksSensor(HomeBlockKind... kinds) {
		super(40);
		this.kinds = kinds;
	}

	@Override
	public Set<MemoryModuleType<?>> requires() {
		return Set.of();
	}

	@Override
	protected void doTick(ServerLevel level, E entity) {
		var home = IHomeHolder.of(level, entity);
		if (home == null || !home.isValid()) return;
		var center = entity.blockPosition();
		for (var kind : kinds) {
			home.getBlockAround(kind, center);
		}
	}

}

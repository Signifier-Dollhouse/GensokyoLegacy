package dev.xkmc.gensokyolegacy.content.entity.behavior.brain;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import dev.xkmc.gensokyolegacy.mixin.BrainAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

public class SmartBrain<E extends Mob> extends Brain<E> {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * The accessor mixin is applied to {@link Brain} itself, so the accessor
	 * methods are only present on a live brain, not in this class file.
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Mob> BrainAccessor<T> access(Brain<T> brain) {
		return (BrainAccessor<T>) brain;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static SmartBrain<?> construct(TaskBoard board, Dynamic<?> dynamic) {
		var ans = new Provider<>(board.memories(), board.getSensors().stream().map(e -> new SensorType(() -> e)).toList()).makeBrain(dynamic);
		ans.setCoreActivities(Set.of(Activity.CORE));
		board.buildBrain(ans);
		return ans;
	}

	private List<Activity> priorityActivities = new ArrayList<>();

	@SuppressWarnings({"unchecked"})
	private SmartBrain(
			Collection<? extends MemoryModuleType<?>> memoryModuleTypes,
			Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes,
			ImmutableList memoryValues,
			Supplier<Codec<Brain<E>>> codec
	) {
		super(memoryModuleTypes, sensorTypes, memoryValues, codec);
	}

	public void setPriorityActivities(List<Activity> list) {
		priorityActivities = list;
	}

	@Override
	public void tick(ServerLevel level, E entity) {
		var brain = access(this);
		brain.doForgetOutdatedMemories();
		brain.doTickSensors(level, entity);
		updateActivities(level, entity);
		brain.doStartEachNonRunningBehavior(level, entity);
		brain.doTickEachRunningBehavior(level, entity);
	}

	private void updateActivities(ServerLevel level, E entity) {
		var brain = access(this);
		for (var e : priorityActivities) {
			if (isActive(e)) {
				if (brain.checkActivityRequirements(e))
					return;
				else brain.setLastScheduleUpdate(0);
			}
			if (brain.checkActivityRequirements(e)) {
				setActiveActivityIfPossible(e);
				return;
			}
		}
		updateActivityFromSchedule(level.getDayTime(), level.getGameTime());
	}

	@Override
	public Brain<E> copyWithoutBehaviors() {
		SmartBrain<E> brain = new SmartBrain<>(access(this).brainMemories().keySet(), access(this).brainSensors().keySet(), ImmutableList.of(), access(this).brainCodec());
		for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : access(this).brainMemories().entrySet()) {
			MemoryModuleType<?> memorymoduletype = entry.getKey();
			if (entry.getValue().isPresent()) {
				access(brain).brainMemories().put(memorymoduletype, entry.getValue());
			}
		}
		return brain;
	}

	public static final class Provider<E extends Mob> {
		private final Collection<? extends MemoryModuleType<?>> memoryTypes;
		private final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes;
		private final Codec<Brain<E>> codec;

		@SuppressWarnings({"rawtypes", "unchecked"})
		public Provider(Collection memoryTypes, Collection sensorTypes) {
			this.memoryTypes = memoryTypes;
			this.sensorTypes = sensorTypes;
			this.codec = Brain.codec(memoryTypes, sensorTypes);
		}

		public SmartBrain<E> makeBrain(Dynamic<?> ops) {
			return this.codec
					.parse(ops)
					.resultOrPartial(LOGGER::error)
					.map(e -> new SmartBrain<>(this.memoryTypes, this.sensorTypes, ImmutableList.copyOf(access(e).brainMemoryValues().toList()), () -> this.codec))
					.orElseGet(() -> new SmartBrain<>(this.memoryTypes, this.sensorTypes, ImmutableList.of(), () -> this.codec));
		}

	}

}
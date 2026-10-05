package dev.xkmc.gensokyolegacy.mixin;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * {@code SmartBrain} re-implements {@link Brain#tick} to squeeze prioritized
 * activities in between the vanilla memory/sensor bookkeeping and the behavior
 * ticking, and clones itself by hand. All of that needs the vanilla private
 * state, which is unreachable from a subclass in another package.
 * <p>
 * The invoker names deliberately differ from the target method names: an
 * invoker that shares name and descriptor with its target would generate a
 * method delegating to itself.
 */
@Mixin(Brain.class)
public interface BrainAccessor<T extends LivingEntity> {

	@Accessor("memories")
	Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> brainMemories();

	@Accessor("sensors")
	Map<SensorType<? extends Sensor<? super T>>, Sensor<? super T>> brainSensors();

	@Accessor("codec")
	Supplier<Codec<Brain<T>>> brainCodec();

	@Accessor("lastScheduleUpdate")
	void setLastScheduleUpdate(long gameTime);

	@Invoker("memories")
	Stream<?> brainMemoryValues();

	@Invoker("forgetOutdatedMemories")
	void doForgetOutdatedMemories();

	@Invoker("tickSensors")
	void doTickSensors(ServerLevel level, T entity);

	@Invoker("startEachNonRunningBehavior")
	void doStartEachNonRunningBehavior(ServerLevel level, T entity);

	@Invoker("tickEachRunningBehavior")
	void doTickEachRunningBehavior(ServerLevel level, T entity);

	@Invoker("activityRequirementsAreMet")
	boolean checkActivityRequirements(Activity activity);

}
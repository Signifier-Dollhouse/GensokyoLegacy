package dev.xkmc.gensokyolegacy.content.entity.behavior.brain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.party.PartyTasks;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Registration util for Activity and Behavior. Two kinds of activity: prioritized and scheduled; a
 * prioritized one's start memory also becomes the start memory-absence of every less prioritized
 * activity, enforcing exclusivity and priority. Three kinds of behavior - always, exclusive and
 * random; all always-type plus 1 exclusive and 1 random may execute simultaneously.
 */
public class TaskBoard {

	private record ActivityEntry(Activity activity, @Nullable MemoryModuleType<?> memory, int priority) {
	}

	private record BehaviorEntry(int priority, BehaviorControl<?> behavior, Set<Activity> activities) {
	}

	private final Map<Activity, ActivityEntry> activities = new LinkedHashMap<>();
	private final List<ActivityEntry> priorities = new ArrayList<>();
	private final List<Activity> priority = new ArrayList<>();
	private final List<BehaviorEntry> always = new ArrayList<>();
	private final List<BehaviorEntry> first = new ArrayList<>();
	private final List<BehaviorEntry> random = new ArrayList<>();
	private final List<Sensor<?>> sensors = new ArrayList<>();
	private final Map<Class<?>, BehaviorEntry> map = new HashMap<>();
	private final Set<MemoryModuleType<?>> memories = new LinkedHashSet<>();

	private Schedule schedule = null;

	private void addMemories(BehaviorControl<?> ctrl) {
		if (ctrl instanceof Behavior<?> beh) {
			memories.addAll(beh.entryCondition.keySet());
		}
	}


	/**
	 * Register a memory with the brain without attaching a task to it. {@link Brain#setMemory} is a
	 * silent no-op and {@code getMemory} throws outright for a type the brain does not know, so a
	 * task that touches a memory it never names in its entry condition has to register it here.
	 */
	public void addMemory(MemoryModuleType<?> memory) {
		memories.add(memory);
	}

	/**
	 * Register the whole party machine - the director and all twelve state tasks - under
	 * {@link GLBrains#PARTY}. It is a cascade rather than a task, so an entity class should not be
	 * the place that knows how many pieces it has or in what order. See {@code PartyTasks}.
	 */
	public void addPartyTasks() {
		PartyTasks.register(this);
	}

	public Set<MemoryModuleType<?>> memories() {
		return memories;
	}

	/**
	 * Add an always-executing behavior to some activities
	 */
	public void addAlways(BehaviorControl<?> behavior, Activity... activities) {
		var entry = new BehaviorEntry(0, behavior, new LinkedHashSet<>(Set.of(activities)));
		always.add(entry);
		map.put(behavior.getClass(), entry);
		addMemories(behavior);
	}

	/**
	 * Add an exclusive behavior to some activities. Only 1 exclusive behavior can run at a time,
	 * and the one with the smallest priority number will run.
	 */
	public void addExclusive(int priority, BehaviorControl<?> behavior, Activity... activities) {
		var entry = new BehaviorEntry(priority, behavior, new LinkedHashSet<>(Set.of(activities)));
		first.add(entry);
		map.put(behavior.getClass(), entry);
		addMemories(behavior);
	}

	/**
	 * Add a randomly-executing behavior to some activities. It runs in parallel with exclusive
	 * behaviors, but only 1 randomly executing behavior will execute at a time.
	 */
	public void addRandom(BehaviorControl<?> behavior, Activity... activities) {
		var entry = new BehaviorEntry(0, behavior, new LinkedHashSet<>(Set.of(activities)));
		random.add(entry);
		map.put(behavior.getClass(), entry);
		addMemories(behavior);
	}

	/**
	 * Add an existing behavior to an extra activity
	 */
	public void addBehaviorActivity(Class<?> cls, Activity activity) {
		map.get(cls).activities.add(activity);
	}

	/**
	 * Add a sensor
	 */
	public void addSensor(Sensor<?> sensor) {
		this.sensors.add(sensor);
	}

	/**
	 * Register an activity as a scheduled activity.
	 * If there is an associated memory type,
	 * the activity will not be executed without presence of that memory.
	 */
	public void addScheduledActivity(Activity activity, @Nullable MemoryModuleType<?> test) {
		addActivityMemory(test);
		activities.put(activity, new ActivityEntry(activity, test, Integer.MAX_VALUE));
	}

	/**
	 * Register an activity as a prioritized activity, smaller priority number first. If there is
	 * an associated memory type, the activity will not be executed without presence of that memory,
	 * and that memory is also added to the must-be-absent requirements of every less prioritized
	 * activity.
	 */
	public void addPrioritizedActivity(Activity activity, @Nullable MemoryModuleType<?> test, int priority) {
		addActivityMemory(test);
		var e = new ActivityEntry(activity, test, priority);
		activities.put(activity, e);
		priorities.add(e);
	}

	/**
	 * An activity's memory has to be registered with the brain, not just named in its
	 * requirements. {@code Brain.checkMemory} answers {@code false} for an unregistered type
	 * whatever status is asked for, so one unregistered activity memory silently blocks every
	 * activity below it, the scheduled ones included.
	 */
	private void addActivityMemory(@Nullable MemoryModuleType<?> test) {
		if (test != null) memories.add(test);
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	/**
	 * Finish constructing the task board and sort everything.
	 */
	public void build() {
		addPrioritizedActivity(Activity.FIGHT, MemoryModuleType.ATTACK_TARGET, 0);
		priorities.sort(Comparator.comparingInt(e -> e.priority));
		first.sort(Comparator.comparingInt(e -> e.priority));
		for (var e : priorities) {
			priority.add(e.activity);
		}
	}

	public List<Sensor<?>> getSensors() {
		return sensors;
	}

	/**
	 * The number an activity was registered with, lowest wins - the same ordering {@link #build()}
	 * sorts by, read back by anything that has to ask "is what she is doing now more urgent than
	 * this". A scheduled activity was never prioritized, hence {@link Integer#MAX_VALUE}.
	 */
	public int priorityOf(Activity activity) {
		for (var e : priorities) {
			if (e.activity().equals(activity)) return e.priority();
		}
		return Integer.MAX_VALUE;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void buildBrain(SmartBrain<?> brain) {
		var list = new ArrayList<Activity>();
		list.add(Activity.CORE);
		for (var e : priorities) {
			list.add(e.activity);
		}
		for (var e : activities.entrySet()) {
			if (e.getValue().priority == Integer.MAX_VALUE) {
				list.add(e.getKey());
			}
		}
		int index = 0;
		Set<MemoryModuleType<?>> previous = new LinkedHashSet<>();
		for (var act : list) {
			var behaviors = fetch(act);
			var copy = new ArrayList();
			for (var e : behaviors) {
				copy.add(Pair.of(index++, e));
			}
			Set<Pair<MemoryModuleType<?>, MemoryStatus>> current = new LinkedHashSet<>();
			for (var e : previous) {
				current.add(Pair.of(e, MemoryStatus.VALUE_ABSENT));
			}
			var entry = activities.get(act);
			if (entry != null) {
				var mem = entry.memory();
				if (mem != null) {
					if (entry.priority < Integer.MAX_VALUE) previous.add(mem);
					current.add(Pair.of(mem, MemoryStatus.VALUE_PRESENT));
				}
			}
			brain.setPriorityActivities(priority);
			brain.addActivityWithConditions(act, ImmutableList.copyOf(copy), current);
		}
		if (schedule != null)
			brain.setSchedule(schedule);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private List<BehaviorControl<?>> fetch(Activity activity) {
		List<Pair<BehaviorControl<?>, Integer>> subFirst = new ArrayList<>();
		for (var e : first) {
			if (e.activities.contains(activity))
				subFirst.add(Pair.of(e.behavior, 100));
		}
		List<Pair<BehaviorControl<?>, Integer>> subRandom = new ArrayList<>();
		for (var e : random) {
			if (e.activities.contains(activity))
				subRandom.add(Pair.of(e.behavior, 100));
		}
		List<BehaviorControl<?>> behaviors = new ArrayList<>();
		for (var e : always) {
			if (e.activities.contains(activity))
				behaviors.add(e.behavior);
		}
		if (subFirst.size() > 1)
			behaviors.add(new GateBehavior(ImmutableMap.of(), ImmutableSet.of(), GateBehavior.OrderPolicy.ORDERED, GateBehavior.RunningPolicy.RUN_ONE, subFirst));
		else if (subFirst.size() == 1)
			behaviors.add(subFirst.getFirst().getFirst());
		if (subRandom.size() > 1)
			behaviors.add(new GateBehavior(ImmutableMap.of(), ImmutableSet.of(), GateBehavior.OrderPolicy.SHUFFLED, GateBehavior.RunningPolicy.RUN_ONE, subRandom));
		else if (subRandom.size() == 1)
			behaviors.add(subRandom.getFirst().getFirst());
		return behaviors;
	}

}

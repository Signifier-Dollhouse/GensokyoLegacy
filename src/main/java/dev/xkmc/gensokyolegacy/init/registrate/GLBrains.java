package dev.xkmc.gensokyolegacy.init.registrate;

import com.mojang.serialization.Codec;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.CompoundPath;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.party.PartyServe;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.init.reg.simple.SR;
import dev.xkmc.l2core.init.reg.simple.Val;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;
import java.util.Optional;

public class GLBrains {

	private static final SR<SensorType<?>> SENSORS = SR.of(GensokyoLegacy.REG, BuiltInRegistries.SENSOR_TYPE);
	private static final SR<MemoryModuleType<?>> MEMORIES = SR.of(GensokyoLegacy.REG, BuiltInRegistries.MEMORY_MODULE_TYPE);
	private static final SR<Activity> ACTIVITIES = SR.of(GensokyoLegacy.REG, BuiltInRegistries.ACTIVITY);

	public static final Val<SensorType<YoukaiUpdateHomeSensor<?>>> SN_HOME = SENSORS.reg("home", () -> new SensorType<>(YoukaiUpdateHomeSensor::new));
	public static final Val<SensorType<YoukaiFindPreySensor<?>>> SN_HUNT = SENSORS.reg("hunt", () -> new SensorType<>(YoukaiFindPreySensor::new));
	public static final Val<SensorType<NearbyItemsSensor<?>>> SN_ITEM = SENSORS.reg("nearby_items", () -> new SensorType<>(NearbyItemsSensor::new));
	public static final Val<SensorType<NearbyLivingEntitySensor<?>>> SN_LE = SENSORS.reg("nearby_living_entities", () -> new SensorType<>(NearbyLivingEntitySensor::new));
	public static final Val<SensorType<NearbyPlayerSensor<?>>> SN_PLAYER = SENSORS.reg("nearby_players", () -> new SensorType<>(NearbyPlayerSensor::new));
	public static final Val<SensorType<YoukaiHomeBlocksSensor<?>>> SN_HOME_BLOCKS = SENSORS.reg("home_blocks", () -> new SensorType<>(YoukaiHomeBlocksSensor::new));

	public static final Val<MemoryModuleType<CompoundPath>> MEM_PATH = MEMORIES.reg("path", () -> new MemoryModuleType<>(Optional.empty()));
	public static final Val<MemoryModuleType<LivingEntity>> MEM_PREY = MEMORIES.reg("prey", () -> new MemoryModuleType<>(Optional.empty()));
	public static final Val<MemoryModuleType<Unit>> MEM_DOWN = MEMORIES.reg("down", () -> new MemoryModuleType<>(Optional.of(Unit.CODEC)));
	public static final Val<MemoryModuleType<Player>> MEM_TALK = MEMORIES.reg("talk", () -> new MemoryModuleType<>(Optional.empty()));
	/**
	 * Whoever just fed her, held for the length of the animation she plays about it.
	 * Its presence is what starts {@code FairyCakeTask}, which eats first and only
	 * then hands something back.
	 */
	public static final Val<MemoryModuleType<Player>> MEM_FEED = MEMORIES.reg("feed", () -> new MemoryModuleType<>(Optional.empty()));
	public static final Val<MemoryModuleType<List<ItemEntity>>> MEM_ITEMS = MEMORIES.reg("nearby_items", () -> new MemoryModuleType<>(Optional.empty()));
	/**
	 * Where the character is visiting. Its presence is what puts her in
	 * {@link #VISITING} instead of following her schedule.
	 */
	public static final Val<MemoryModuleType<GlobalPos>> MEM_VISIT = MEMORIES.reg("visit", () -> new MemoryModuleType<>(Optional.empty()));
	/**
	 * Where a party guest is eating. Its presence is what puts her in
	 * {@link #PARTY} instead of following her schedule.
	 */
	public static final Val<MemoryModuleType<GlobalPos>> MEM_PARTY = MEMORIES.reg("party", () -> new MemoryModuleType<>(Optional.empty()));
	/**
	 * Whoever last hurt her, kept only until the scare wears off. Its presence is
	 * what puts a plain fairy in {@link #FEAR}.
	 */
	public static final Val<MemoryModuleType<LivingEntity>> MEM_FEAR = MEMORIES.reg("fear", () -> new MemoryModuleType<>(Optional.empty()));

	// --- the party machine ---
	//
	// The guest's whole state machine is these seven memories and nothing else: which
	// task runs is decided by which of them are present, so there is no phase enum and
	// no dispatcher. Every one of them carries a codec, which is the whole of how a
	// guest survives a save — a MemoryModuleType with no codec is never written out.
	// See doc/design/party/guest.md.

	/** Has food in hand, and has not finished eating it. Also carries the serve's readings. */
	public static final Val<MemoryModuleType<PartyServe>> MEM_PARTY_SERVE = MEMORIES.reg("party_serve", () -> new MemoryModuleType<>(Optional.of(PartyServe.CODEC)));
	/** Has a food target: the dish she has committed to this round. */
	public static final Val<MemoryModuleType<BlockPos>> MEM_PARTY_DISH = MEMORIES.reg("party_dish", () -> new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
	/** Eat wait - deciding at the dish, ticks left. */
	public static final Val<MemoryModuleType<Integer>> MEM_PARTY_HESITATE = MEMORIES.reg("party_hesitate", () -> new MemoryModuleType<>(Optional.of(Codec.INT)));
	/** The pickup gesture, ticks left. */
	public static final Val<MemoryModuleType<Integer>> MEM_PARTY_GRAB = MEMORIES.reg("party_grab", () -> new MemoryModuleType<>(Optional.of(Codec.INT)));
	/** Eat wait - chewing, ticks left. */
	public static final Val<MemoryModuleType<Integer>> MEM_PARTY_CHEW = MEMORIES.reg("party_chew", () -> new MemoryModuleType<>(Optional.of(Codec.INT)));
	/** Post-eat wait: sitting between dishes, ticks left. */
	public static final Val<MemoryModuleType<Integer>> MEM_PARTY_REST = MEMORIES.reg("party_rest", () -> new MemoryModuleType<>(Optional.of(Codec.INT)));
	/** The unseat wait, and then the walk out. Presence alone means "the party is over". */
	public static final Val<MemoryModuleType<Integer>> MEM_PARTY_LEAVE = MEMORIES.reg("party_leave", () -> new MemoryModuleType<>(Optional.of(Codec.INT)));

	public static final Val<Activity> AT_HOME = ACTIVITIES.reg("at_home", () -> new Activity("at_home"));
	public static final Val<Activity> HUNT = ACTIVITIES.reg("hunt", () -> new Activity("hunt"));
	public static final Val<Activity> DOWN = ACTIVITIES.reg("down", () -> new Activity("down"));
	public static final Val<Activity> TALK = ACTIVITIES.reg("talk", () -> new Activity("talk"));
	/**
	 * Being a guest in someone else's home. Priority 150 puts it between FIGHT(0)
	 * and TALK(100), so a visitor still stops to talk - see the design doc.
	 */
	public static final Val<Activity> VISITING = ACTIVITIES.reg("visiting", () -> new Activity("visiting"));
	/**
	 * Eating at somebody's party. Priority 200 puts it above both TALK(100) and
	 * VISITING(150): a party guest can still talk and trade, but she never wanders
	 * off to visit, and never sleeps or goes home.
	 */
	public static final Val<Activity> PARTY = ACTIVITIES.reg("party", () -> new Activity("party"));
	/**
	 * Running from whatever just hit her. Outranks eating, so a cake offered mid-panic
	 * waits for her to stop running rather than stopping her - see the priority scale
	 * in {@code PlainFairyEntity#constructTaskBoard}.
	 */
	public static final Val<Activity> FEAR = ACTIVITIES.reg("fear", () -> new Activity("fear"));
	/**
	 * Standing still with a cake in her hands. Being fed cuts in on talking and on her
	 * schedule, but not on a panic, and not on a hunt.
	 */
	public static final Val<Activity> FEAST = ACTIVITIES.reg("feast", () -> new Activity("feast"));

	public static void register() {
	}

}

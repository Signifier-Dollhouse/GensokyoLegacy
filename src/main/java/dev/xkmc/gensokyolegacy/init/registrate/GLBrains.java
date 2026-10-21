package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.content.entity.behavior.move.CompoundPath;
import dev.xkmc.gensokyolegacy.content.entity.behavior.sensor.*;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.FairyFeedModule;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.init.reg.simple.SR;
import dev.xkmc.l2core.init.reg.simple.Val;
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
	/** Who just fed her and what she owes them for it, held for the length of the animation she plays about
	 *  it; both halves travel together because the gift depends on the food. Its presence starts
	 *  {@code FairyFeedTask}, which eats first and only then hands something back. */
	public static final Val<MemoryModuleType<FairyFeedModule.Fed>> MEM_FEED =
			MEMORIES.reg("feed", () -> new MemoryModuleType<>(Optional.empty()));
	public static final Val<MemoryModuleType<List<ItemEntity>>> MEM_ITEMS = MEMORIES.reg("nearby_items", () -> new MemoryModuleType<>(Optional.empty()));
	/**
	 * Where the character is visiting. Its presence is what puts her in
	 * {@link #VISITING} instead of following her schedule.
	 */
	public static final Val<MemoryModuleType<GlobalPos>> MEM_VISIT = MEMORIES.reg("visit", () -> new MemoryModuleType<>(Optional.empty()));
	/**
	 * Where a party guest is eating. Its presence is what puts her in {@link #PARTY} instead of following
	 * her schedule. No codec, so it does not survive a save and {@code PartyModule} re-asserts it every tick.
	 */
	public static final Val<MemoryModuleType<GlobalPos>> MEM_PARTY = MEMORIES.reg("party", () -> new MemoryModuleType<>(Optional.empty()));
	/**
	 * Whoever last hurt her, kept only until the scare wears off. Its presence is
	 * what puts a plain fairy in {@link #FEAR}.
	 */
	public static final Val<MemoryModuleType<LivingEntity>> MEM_FEAR = MEMORIES.reg("fear", () -> new MemoryModuleType<>(Optional.empty()));
	/** How long she still intends to lie down. Its presence is what puts her in {@link #NAP}, and its expiry ends
	 *  that - the nap has no other reason to stop, so the memory is the clock, not a flag. <p> No codec, so a nap
	 *  does not survive a save; she wakes up on reload rather than being found lying there. */
	public static final Val<MemoryModuleType<Unit>> MEM_NAP = MEMORIES.reg("nap", () -> new MemoryModuleType<>(Optional.empty()));

	/*
	 * ============================== prioritized activities ==============================
	 *
	 * The scale below, highest first, with the number each activity is registered with. A prioritized
	 * activity's number IS its rank: TaskBoard#build sorts by it, lowest first, and SmartBrain#tick walks the
	 * sorted list taking the first activity whose memory requirements hold, so there is no other priority
	 * mechanism. The rungs are 100 apart so a new activity takes an open hundred rather than a neighbour.
	 *
	 *    -100  DOWN      rumia, fainted by a paralyze; nothing outranks it
	 *       0  FIGHT     vanilla, added by TaskBoard#build; everything else yields
	 *     100  FEAR      a plain fairy running: beats eating and talking, not a fight
	 *     200  FEAST     being fed: beats a conversation, not a panic, not a fight
	 *     300  TALK      a conversation: no guest is too busy to talk, but a fight ends one
	 *     400  PARTY     eating at somebody's party: as TALK, and never wanders off to
	 *                    visit, and never sleeps or goes home
	 *     500  NAP       lying down doing nothing: gets up for a fight or a party - a
	 *                    conversation is answered from where she is lying - and never
	 *                    overrides her own schedule
	 *     600  VISITING  a guest in someone else's home: never sleeps, goes home or hunts,
	 *                    and never naps in the host's house
	 *     700  HUNT      prey in reach: the lowest rung there is
	 *
	 *    MAX   REST / AT_HOME / IDLE / PLAY / WORK - the schedule, which everything above
	 *          outranks. An activity passed to TaskBoard#addScheduledActivity is ranked
	 *          here whether or not the character that owns it puts it in its schedule.
	 *
	 * TaskBoard injects a prioritized activity's memory as VALUE_ABSENT into every LESS prioritized activity,
	 * so a rung is a yes/no answer to one question - what else may she be doing while this goes on - read it
	 * as "everything below me has to be absent for me to run". NAP(500) below PARTY(400) gets her out of bed
	 * for a party; above VISITING(600) it holds a visit off, which costs a few seconds at the start of a visit
	 * and no more, since a nap is only ever offered on her own schedule and a visit is an all-day affair.
	 *
	 * A rung is about the activity, not about the body. TALK(300) above NAP(500) means a conversation is what
	 * actually runs while she is lying there, and YoukaiNapTask deliberately holds the pose across it. An
	 * activity that has been outranked does not stop running, so holding a pose through something that
	 * outranks it belongs in canStillUse, not in the activity: see YoukaiNapTask, and FairyFleeTask for the
	 * other half of that trade, where the activity is what has to be noticed.
	 *
	 * Two rules follow, and between them they decide almost every placement:
	 *
	 *   - Anything below an activity is something she may be talked out of; anything above it interrupts it.
	 *   - An activity registered with a null gate memory never ends, because the test that would end it is
	 *     the one that started it. Every prioritized activity here therefore owns a memory, and where there
	 *     is no other reason to stop it carries a TTL - MEM_NAP is the shape to copy.
	 *
	 * To place a new activity: decide what may interrupt it, decide what it may interrupt, and find the rung
	 * whose answer is closest, then take the open hundred between the two neighbours that bracket it - ties
	 * break by registration order, which is invisible to anyone reading the board later. Each activity's own
	 * javadoc should still say WHY it sits where it does.
	 *
	 * ====================================================================================
	 */

	public static final Val<Activity> AT_HOME = ACTIVITIES.reg("at_home", () -> new Activity("at_home"));
	public static final Val<Activity> HUNT = ACTIVITIES.reg("hunt", () -> new Activity("hunt"));
	public static final Val<Activity> DOWN = ACTIVITIES.reg("down", () -> new Activity("down"));
	public static final Val<Activity> TALK = ACTIVITIES.reg("talk", () -> new Activity("talk"));
	/**
	 * Being a guest in someone else's home. Low enough that a visitor still stops to talk, high enough that
	 * she never naps in the host's house, never hunts in it, and never sleeps or goes home.
	 */
	public static final Val<Activity> VISITING = ACTIVITIES.reg("visiting", () -> new Activity("visiting"));
	/**
	 * Eating at somebody's party. Just below TALK, so a party guest can still talk and trade; well above
	 * VISITING, so she never wanders off while the party is going on; and above everything scheduled.
	 */
	public static final Val<Activity> PARTY = ACTIVITIES.reg("party", () -> new Activity("party"));
	/**
	 * Running from whatever just hit her. Outranks eating, so a snack offered mid-panic
	 * waits for her to stop running rather than stopping her - see the scale above.
	 */
	public static final Val<Activity> FEAR = ACTIVITIES.reg("fear", () -> new Activity("fear"));
	/**
	 * Standing still with something in her hands. Being fed cuts in on talking and on
	 * her schedule, but not on a panic, and not on a hunt.
	 */
	public static final Val<Activity> FEAST = ACTIVITIES.reg("feast", () -> new Activity("feast"));
	/** Lying down and doing nothing at all. Below TALK and PARTY: a conversation is answered from where she is
	 *  lying rather than getting her up - {@code YoukaiNapTask} holds the pose through one - but a party still
	 *  does. Above VISITING and the schedule: a nap is her own free time, not a guest's house. */
	public static final Val<Activity> NAP = ACTIVITIES.reg("nap", () -> new Activity("nap"));

	public static void register() {
	}

}

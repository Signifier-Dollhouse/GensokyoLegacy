package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Drives the phases of every party in this level. Each one registers its chunk here and only
 * those chunks are swept.
 * @see "doc/design/party/state.md"
 */
public class PartyTicker {

	/** How often a party's phases advance, in ticks. */
	private static final int INTERVAL = 20;

	/** Shortest gap between two of the gathering rockets, in ticks. */
	private static final int ROCKET_MIN = 10;

	/** Longest such gap. A drawn one lands on one figure or the other, which is what reads as
	 *  hand-fired rather than metered. */
	private static final int ROCKET_MAX = 20;

	/** How often the rockets are looked for: the floor of the range, so a drawn gap cannot round
	 *  below it and the driver stays at 2 Hz instead of 20. */
	private static final int ROCKET_CHECK = ROCKET_MIN;

	/** A party open this long with nothing left happening is called off. */
	public static final int PARTY_TIMEOUT = 12000;

	/**
	 * How long a closing party waits for its last guest once everyone is off their chairs. It
	 * starts at the last unseat rather than at CLOSING, so the window is entirely about the
	 * walkout: a guest sitting out her unseat wait is not one who cannot leave.
	 */
	public static final int CLOSING_TIMEOUT = 600;

	/** Level -> the chunks currently hosting a party. */
	private static final Map<ServerLevel, Set<ChunkPos>> ACTIVE = new ConcurrentHashMap<>();

	/** Remember a chunk as hosting a party, so its phases get advanced. */
	public static void register(ServerLevel level, ChunkPos pos) {
		ACTIVE.computeIfAbsent(level, e -> Collections.synchronizedSet(new LinkedHashSet<>())).add(pos);
	}

	/** Stop driving a chunk's party: it ended, or the chunk went away. */
	public static void unregister(ServerLevel level, ChunkPos pos) {
		var set = ACTIVE.get(level);
		if (set == null) return;
		synchronized (set) {
			set.remove(pos);
		}
		if (set.isEmpty()) ACTIVE.remove(level, set);
	}

	/** Forget everything about this level; called when it unloads. */
	public static void clear(ServerLevel level) {
		ACTIVE.remove(level);
	}

	/** Advance every party in this level. Cheap when there are none. */
	public static void tick(ServerLevel level) {
		var set = ACTIVE.get(level);
		if (set == null) return;
		long time = level.getGameTime();
		// two cadences on one sweep: the phases are a second a part, the rockets are finer
		boolean phases = time % INTERVAL == 0;
		boolean rockets = time % ROCKET_CHECK == 0;
		if (!phases && !rockets) return;
		List<ChunkPos> snapshot;
		synchronized (set) {
			snapshot = new ArrayList<>(set);
		}
		for (var pos : snapshot) {
			// phases first: a table that fills on this pass is no longer one to celebrate
			if (phases) tickOne(level, pos);
			if (rockets) rocket(level, pos, time);
		}
	}

	private static void tickOne(ServerLevel level, ChunkPos pos) {
		// a chunk that unloaded takes its party with it, so forget it either way
		var party = PartyAttachment.of(level, pos);
		if (party == null || !party.active()) {
			unregister(level, pos);
			return;
		}
		// No sweep for guests who cannot be found: getEntity finds only LOADED entities, so one
		// outside her chunk looks like a corpse; they come off via onKilled or completeLeave (state.md §1.1).
		// refresh what the guests draw from before anyone asks: a stale list means no second course
		party.refresh(level, level.getGameTime());
		// one arrival a second — this 1 Hz pass is the clock, so no timer of its own is needed
		PartySend.admitNext(level, party);
		// admitted() matters: allSeated is vacuously true with no guests, and an empty
		// guest list is normal for the first few seconds of a staggered arrival
		if (party.phase() == PartyAttachment.Phase.GATHERING && party.admitted() && allSeated(level, party)) {
			party.setPhase(PartyAttachment.Phase.FEASTING);
		}
		// two ways out of a feast: the spread gone for two rescans, or the last guest gone
		if (party.phase() == PartyAttachment.Phase.FEASTING
				&& (party.spreadGone() || party.guests().isEmpty() && party.admitted())) {
			PartySend.close(level, party);
		}
		if (level.getGameTime() - party.startedAt() > PARTY_TIMEOUT) {
			PartySend.close(level, party);
		}
		if (!finished(level, party)) return;
		// everyone has walked off, or nobody was ever seated
		party.reset();
		unregister(level, party.chunk() == null ? pos : party.chunk());
	}

	/**
	 * The gathering rockets: one every {@link #ROCKET_MIN}–{@link #ROCKET_MAX} ticks, from the first
	 * guest sitting down to the last one sitting down.
	 * <p>
	 * On its own clock rather than the phase one, for two reasons. A gap drawn out of a range
	 * cannot be honoured at 1 Hz, and {@code sparkAt} starts at 0 — so the first rocket is the
	 * first pass <i>after</i> the first guest sits, not a fixed count from the send.
	 */
	private static void rocket(ServerLevel level, ChunkPos pos, long time) {
		var party = PartyAttachment.of(level, pos);
		if (party == null || party.phase() != PartyAttachment.Phase.GATHERING) return;
		if (time < party.sparkAt()) return;
		// a table nobody has sat at yet, and a roster that is in with the last chair filled
		if (!anySeated(level, party)) return;
		if (party.admitted() && allSeated(level, party)) return;
		PartyFirework.launch(level, party.liveAnchor());
		party.sparkAt(time + ROCKET_MIN + level.getRandom().nextInt(ROCKET_MAX - ROCKET_MIN + 1));
	}

	/** Whether the party is over: a closing one when its roster has drained, an open one when nobody is left
	 * to invite. The {@code admitted()} check keeps staggered arrival from reading an empty guest list as gone.
	 */
	private static boolean finished(ServerLevel level, PartyAttachment party) {
		if (party.guests().isEmpty()) {
			return party.phase() == PartyAttachment.Phase.CLOSING || party.admitted();
		}
		if (party.phase() != PartyAttachment.Phase.CLOSING) return false;
		// the backstop is about the walkout, so the clock does not start until the last guest is
		// out of her chair. Started at CLOSING it would run against the unseat waits as well, and
		// cut off a table that is still sitting there on purpose.
		if (anySeated(level, party)) return false;
		party.startClosingClock(level.getGameTime());
		// a guest who cannot walk off must not pin the chunk's party open
		return level.getGameTime() - party.closingAt() > CLOSING_TIMEOUT;
	}

	/**
	 * Whether anyone is still in a chair. A guest who is not loaded does not count: she is on her
	 * way off somewhere this pass cannot see, and holding the backstop for her is holding it for
	 * every guest.
	 */
	private static boolean anySeated(ServerLevel level, PartyAttachment party) {
		for (var id : party.guests()) {
			if (find(level, id) instanceof YoukaiEntity youkai
					&& youkai.getModule(PartyModule.class).map(PartyModule::seated).orElse(false)) return true;
		}
		return false;
	}

	/**
	 * Whether every guest has a chair. Party-wide on purpose, so the table feasts as a
	 * table; vacuously true with no guests at all.
	 */
	private static boolean allSeated(ServerLevel level, PartyAttachment party) {
		for (var id : party.guests()) {
			var guest = find(level, id);
			// a guest who is not loaded has not proved she has a chair
			if (!(guest instanceof YoukaiEntity youkai)) return false;
			if (youkai.getModule(PartyModule.class).map(m -> m.seated()).orElse(false)) continue;
			return false;
		}
		return true;
	}

	/** The live guest with this id, or null if she died, left, or unloaded. */
	@Nullable
	private static Entity find(ServerLevel level, UUID id) {
		return level.getEntity(id);
	}

	/** Every guest of this party still standing, for the command and for teardown. */
	public static List<YoukaiEntity> guests(ServerLevel level, PartyAttachment party) {
		var ans = new ArrayList<YoukaiEntity>();
		BlockPos anchor = party.anchor();
		if (anchor == null) return ans;
		var area = new AABB(anchor).inflate(PARTY_GUEST_AREA);
		for (var e : level.getEntitiesOfClass(YoukaiEntity.class, area)) {
			boolean ours = e.getModule(PartyModule.class)
					.map(m -> m.partying() && party.guests().contains(e.getUUID()))
					.orElse(false);
			if (ours) ans.add(e);
		}
		return ans;
	}

	/** Guests are restricted well inside this, so a generous box is enough. */
	private static final int PARTY_GUEST_AREA = 64;

	private PartyTicker() {
	}

}

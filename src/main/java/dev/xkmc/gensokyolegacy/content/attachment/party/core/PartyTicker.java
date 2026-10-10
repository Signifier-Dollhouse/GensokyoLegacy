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

/**
 * Drives the phases of any party in this level.
 * <p>
 * Parties are rare — a handful at a time at most — so rather than sweeping every
 * loaded chunk on the level tick, each party registers its chunk here when it
 * opens and is driven from that list. Nothing here runs while no party is active.
 */
public class PartyTicker {

	/** How often a party's phases advance, in ticks. */
	private static final int INTERVAL = 20;
	/** A party open this long with nothing left happening is called off. */
	public static final int PARTY_TIMEOUT = 12000;

	/**
	 * How long a closing party waits for its last guest before giving up on her.
	 * <p>
	 * Closing normally ends when the roster drains, and a guest walks off in a couple of
	 * hundred ticks. This is what stops one who cannot — blocked in a doorway, pathing
	 * into a wall — pinning the chunk's party open for ever.
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
		if (level.getGameTime() % INTERVAL != 0) return;
		List<ChunkPos> snapshot;
		synchronized (set) {
			snapshot = new ArrayList<>(set);
		}
		for (var pos : snapshot) tickOne(level, pos);
	}

	private static void tickOne(ServerLevel level, ChunkPos pos) {
		// a chunk that unloaded takes its party with it, so forget it either way
		var party = PartyAttachment.of(level, pos);
		if (party == null || !party.active()) {
			unregister(level, pos);
			return;
		}
		// drop guests that died or left, so a party cannot stay pinned open by a
		// guest that is no longer standing
		for (var id : Set.copyOf(party.guests())) {
			if (find(level, id) != null) continue;
			party.removeGuest(id);
		}
		// refresh what the guests draw from, before anyone asks: a party whose food list
		// is stale is a party that cannot see a second course, or one still offering a
		// cake three guests have already eaten
		party.refresh(level, level.getGameTime());
		// One arrival a second. This pass is the clock: the ticker runs at 1 Hz, so one
		// admission per pass needs no timer of its own, and a table that fills in a
		// single frame reads as a cutscene rather than as people turning up.
		PartySend.admitNext(level, party);
		// the admitted half matters because allSeated is vacuously true with no guests at
		// all, and with staggered arrival an empty guest list is the normal state of a
		// party in its first few seconds: without it every party would feast before
		// anybody arrived
		if (party.phase() == PartyAttachment.Phase.GATHERING && party.admitted() && allSeated(level, party)) {
			party.setPhase(PartyAttachment.Phase.FEASTING);
		}
		// Two ways out of a feast, and the guests' is the stronger: the spread gone for
		// two rescans running, or the last of them gone with the food still on the table.
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
	 * Whether the party is over.
	 * <p>
	 * A closing party is over when its roster has drained — each guest removes herself as
	 * she finishes leaving, so the list emptying <i>is</i> the last one gone. An open one
	 * is over when the roster is empty and there is nobody left to invite: every guest
	 * died, or the host's furniture turned out to seat nobody.
	 * <p>
	 * The admitted check is what makes staggered arrival safe. Guests arrive one a second,
	 * so an empty guest list is the normal state of a party in its first few seconds, and
	 * reading it as "everyone has gone" would end every party immediately.
	 */
	private static boolean finished(ServerLevel level, PartyAttachment party) {
		if (party.guests().isEmpty()) {
			return party.phase() == PartyAttachment.Phase.CLOSING || party.admitted();
		}
		// a guest who cannot walk off must not pin the chunk's party open
		return party.phase() == PartyAttachment.Phase.CLOSING
				&& party.closingAt() > 0 && level.getGameTime() - party.closingAt() > CLOSING_TIMEOUT;
	}

	/**
	 * Whether every guest has a chair.
	 * <p>
	 * The gate out of GATHERING, and deliberately party-wide rather than per-guest: the
	 * table feasts as a table, and a guest who has just sat down should not start eating
	 * out of an empty room while the others are still walking in.
	 * <p>
	 * The cost of that choice is that one guest with no chair holds the whole table out of
	 * the feast until {@link #PARTY_TIMEOUT} calls the party off. A per-guest gate would
	 * not stall, but it would also have the last guest to arrive eating alone at an
	 * ungathered table, which reads as a bug rather than as a party. Seating is a host
	 * problem either way — the fix is to put out another chair — and the timeout is the
	 * backstop for a host who never does.
	 * <p>
	 * Vacuously true with no guests at all, so a party whose roster is entirely dead moves
	 * on rather than sitting in GATHERING until the timeout.
	 */
	private static boolean allSeated(ServerLevel level, PartyAttachment party) {
		for (var id : party.guests()) {
			var guest = find(level, id);
			if (guest == null) continue;
			if (!(guest instanceof YoukaiEntity youkai)) continue;
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

	/**
	 * Every guest of this party still standing, for the command and for teardown.
	 */
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

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
		boolean overdue = level.getGameTime() - party.startedAt() > PARTY_TIMEOUT;
		if (!party.guests().isEmpty() && !overdue) return;
		// everyone has gone, or the banquet has run long enough
		PartySend.dismissAll(level, party);
		unregister(level, party.chunk() == null ? pos : party.chunk());
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

package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2core.capability.attachment.GeneralCapabilityTemplate;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The state of one party, stored on the chunk holding the centre of its food.
 * <p>
 * Living on the chunk rather than on the host makes the party chunk-scoped for
 * free — unload the chunk and it freezes rather than corrupts — and it survives
 * host logout, because the data belongs to the chunk and not to the player.
 * <p>
 * The roster is frozen when the letter is sent and never recomputed. Re-running
 * the planner on a timer would let a guest who wandered in mid-banquet change the
 * guest list under everyone else, and would make the outcome depend on tick
 * order. Frozen, the roster is a pure function of the spread, the seats and the
 * player's reputation at the moment of sending, which is what lets
 * {@code /gensokyo party preview} predict a send exactly.
 */
@SerialClass
public class PartyAttachment extends GeneralCapabilityTemplate<LevelChunk, PartyAttachment> {

	public enum Phase {
		IDLE, GATHERING, FEASTING, CLOSING
	}

	@SerialField
	private UUID host;

	@SerialField
	private BlockPos anchor;

	/** The chunk this party is owned by; also where the driver looks for it. */
	@SerialField
	private ChunkPos chunk;

	@SerialField
	private Phase phase = Phase.IDLE;

	@SerialField
	private long startedAt;

	@SerialField
	private final ArrayList<EntityType<?>> roster = new ArrayList<>();

	@SerialField
	private final ArrayList<UUID> guests = new ArrayList<>();

	/**
	 * The spread's hunger at send time: how much it could feed, and so the guests'
	 * shared budget. Renamed from {@code appealBudget}, which is the quantity this stopped
	 * being — an existing save's party reads back as a 0 budget until the next send.
	 */
	@SerialField
	private int hungerBudget;

	/** Hunger the guests have actually been served, against {@link #hungerBudget}. */
	@SerialField
	private int hungerServed;

	// not SerialField -> not serialized. GeneralCapabilityTemplate hands the payload
	// no level, so the owning chunk is bound by the lookup helpers instead.
	@Nullable
	private LevelChunk owner;

	/** Whether this chunk already hosts a party; the send-time overlap guard reads this. */
	public boolean active() {
		return phase != Phase.IDLE;
	}

	public Phase phase() {
		return phase;
	}

	@Nullable
	public UUID host() {
		return host;
	}

	@Nullable
	public BlockPos anchor() {
		return anchor;
	}

	@Nullable
	public ChunkPos chunk() {
		return chunk;
	}

	public long startedAt() {
		return startedAt;
	}

	public List<EntityType<?>> roster() {
		return roster;
	}

	public List<UUID> guests() {
		return guests;
	}

	public int hungerBudget() {
		return hungerBudget;
	}

	public int hungerServed() {
		return hungerServed;
	}

	public int seatsUsed() {
		return guests.size();
	}

	/** Freeze the roster and open the party. Every rejection must happen before this. */
	public void start(LevelChunk chunk, long gameTime, UUID host, BlockPos anchor,
			List<EntityType<?>> roster, int hunger) {
		this.owner = chunk;
		this.host = host;
		this.anchor = anchor.immutable();
		this.chunk = chunk.getPos();
		this.roster.clear();
		this.roster.addAll(roster);
		this.guests.clear();
		this.hungerBudget = hunger;
		this.hungerServed = 0;
		this.startedAt = gameTime;
		this.phase = Phase.GATHERING;
		dirty();
	}

	public void addGuest(UUID id) {
		if (guests.add(id)) dirty();
	}

	public void removeGuest(UUID id) {
		if (guests.remove(id)) dirty();
	}

	public void setPhase(Phase phase) {
		if (this.phase != phase) {
			this.phase = phase;
			dirty();
		}
	}

	/** Hunger actually consumed by the guests so far. */
	public void serve(int hunger) {
		if (hunger > 0) {
			hungerServed += hunger;
			dirty();
		}
	}

	/** Back to idle. The roster is kept so a command can still report on it. */
	public void reset() {
		host = null;
		anchor = null;
		phase = Phase.IDLE;
		guests.clear();
		dirty();
	}

	/** The anchor, for a caller that has already established the party is live. */
	public BlockPos liveAnchor() {
		if (anchor == null) throw new IllegalStateException("party is idle");
		return anchor;
	}

	private void dirty() {
		if (owner != null) owner.setUnsaved(true);
	}

	// --- lookup ---

	/** This chunk's party, or null if the chunk is not loaded. Never force-loads. */
	@Nullable
	public static PartyAttachment of(ServerLevel level, ChunkPos pos) {
		LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, false);
		if (chunk == null) return null;
		var party = GLMeta.PARTY.type().getOrCreate(chunk);
		party.owner = chunk;
		return party;
	}

	/**
	 * The live party overlapping these chunks, or null. Unloaded chunks are
	 * skipped rather than loaded: a party only exists while its chunk is loaded,
	 * so an unloaded chunk cannot be hosting one.
	 */
	@Nullable
	public static PartyAttachment existing(ServerLevel level, ChunkPosRange range) {
		PartyAttachment found = null;
		for (var cpos : range.stream().toList()) {
			var party = of(level, cpos);
			if (party != null && party.active()) found = party;
		}
		return found;
	}

	/** The active party in the chunk containing this position, if there is one. */
	@Nullable
	public static PartyAttachment at(ServerLevel level, BlockPos pos) {
		var party = of(level, new ChunkPos(pos));
		return party != null && party.active() ? party : null;
	}

	/**
	 * The active party within {@code radius} blocks of this position, or null.
	 * <p>
	 * A party lives in the chunk holding the <b>centre of its food</b>, which is not
	 * necessarily the chunk holding its anchor table. Asking "is there a party at
	 * this table?" therefore needs the neighbourhood and not the one chunk — the
	 * centre is within the scan's reach of the table by construction, so the scan
	 * always finds it.
	 */
	@Nullable
	public static PartyAttachment near(ServerLevel level, BlockPos pos, int radius) {
		var party = at(level, pos);
		if (party != null) return party;
		return existing(level, ChunkPosRange.ofOwner(pos, radius));
	}

}

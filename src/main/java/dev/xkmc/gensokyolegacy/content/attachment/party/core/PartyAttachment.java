package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * One party's state, on the chunk holding the centre of its food. The roster is frozen at
 * send and never recomputed, which is what lets a preview predict a send exactly.
 *
 * @see "doc/design/party/state.md"
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

	/** The spread's hunger at send time — the guests' shared budget. */
	@SerialField
	private int hungerBudget;

	/** Hunger the guests have actually been served, against {@link #hungerBudget}. */
	@SerialField
	private int hungerServed;

	/** What the table was worth by the end — the counter the host is paid from. */
	@SerialField
	private int appealServed;

	/** Fast: a second course should be noticed while the table is still full of people. */
	private static final int FOOD_RESCAN = 40;

	/** Five times slower than the food: chairs are not eaten, and the list only has to stay roughly right. */
	private static final int LAYOUT_RESCAN = 200;

	/** The spread as of the last food rescan; serialized so a resumed party keeps it. */
	@SerialField
	private final ArrayList<BlockPos> food = new ArrayList<>();

	/** Every chair as of the last layout rescan. */
	@SerialField
	private final ArrayList<BlockPos> seats = new ArrayList<>();

	/**
	 * The seating plan: chair to guest. First-write-wins, and pruned only by the layout
	 * rescan when a chair goes away.
	 */
	@SerialField
	private final HashMap<BlockPos, UUID> claims = new HashMap<>();

	/** When each of the two lists was last rebuilt. */
	@SerialField
	private long foodScannedAt;
	@SerialField
	private long layoutScannedAt;

	/** Bumped whenever the chair list changes, so a guest can notice her chair is gone. */
	@SerialField
	private int layoutVersion;

	/**
	 * Roster entries admitted so far. {@link PartyTicker} admits one per pass, and its pass
	 * is one a second.
	 */
	@SerialField
	private int admitted = 0;

	/** Consecutive empty food rescans; the party ends on the second. */
	@SerialField
	private int emptyScans = 0;

	/** Backstop against one stuck guest pinning the chunk's party open for ever, stamped from the
	 * last unseat rather than from CLOSING — see {@link #startClosingClock}. */
	@SerialField
	private long closingAt = 0;

	// Not a SerialField, so null in a freshly deserialized attachment. Every lookup goes
	// through the helpers below, which rebind it.
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

	/** Appeal the guests have actually been served; what the host is paid from. */
	public int appealServed() {
		return appealServed;
	}

	public int seatsUsed() {
		return guests.size();
	}

	/** The spread as of the last rescan: every dish a guest can eat. */
	public List<BlockPos> food() {
		return food;
	}

	/** Every chair as of the last layout rescan. */
	public List<BlockPos> seats() {
		return seats;
	}

	/** Bumped when the chair list changes; a guest compares it to notice her chair is gone. */
	public int layoutVersion() {
		return layoutVersion;
	}

	/**
	 * The chair this guest gets, claiming it for her: her own while it survives, else the free chair
	 * nearest her.
	 * <p>
	 * <b>Null means there is nothing to give her</b> — every chair claimed or none at all. Callers ask
	 * whether a chair is hers rather than whether one exists: another guest's chair is never offered
	 * to her twice.
	 *
	 * @param guest who is asking, by id: her position is what "nearest" is nearest to
	 * @param hers  the chair she believes she has, or null. Advisory only — the answer comes from the
	 *              party's record
	 */
	@Nullable
	public BlockPos claimSeat(ServerLevel level, UUID guest, @Nullable BlockPos hers) {
		// Her own claim first, from the party's record: she records a chair only once sitting on
		// it, so an interrupted walk would otherwise leave the first one claimed for ever.
		for (var entry : claims.entrySet()) {
			if (!guest.equals(entry.getValue())) continue;
			if (seats.contains(entry.getKey())) return entry.getKey();
			claims.remove(entry.getKey());   // gone: the layout rescan has not caught up
			break;
		}
		// hers can only be stale: not granted by this party, or already pruned above
		if (hers != null) claims.remove(hers);
		var entity = level.getEntity(guest);
		BlockPos from = entity == null ? anchor : entity.blockPosition();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (var pos : seats) {
			if (claims.containsKey(pos)) continue;
			// the list is stale, so a chair broken since would be handed out
			if (!level.getBlockState(pos).is(GLTagGen.PARTY_SEAT)) continue;
			double dist = pos.distSqr(from);
			if (dist >= bestDist) continue;
			bestDist = dist;
			best = pos;
		}
		if (best == null) return null;
		claims.put(best, guest);
		dirty();
		return best;
	}

	/**
	 * Give up this guest's claim on any chair, because she is leaving or has been sent to
	 * look again. Without it the table could never fill.
	 */
	public void releaseSeat(UUID guest) {
		if (claims.values().removeIf(guest::equals)) dirty();
	}

	/** Whether the whole roster has been admitted. */
	public boolean admitted() {
		return admitted >= roster.size();
	}

	/** How many of the roster are still to arrive. */
	public int pending() {
		return Math.max(0, roster.size() - admitted);
	}

	/**
	 * The next character to admit, or null when the roster is exhausted — the two are the
	 * same question to the caller.
	 */
	@Nullable
	public EntityType<?> nextGuest() {
		return admitted < roster.size() ? roster.get(admitted) : null;
	}

	/** Mark one roster entry as dealt with, whether or not it produced a guest. */
	public void admit() {
		if (admitted < roster.size()) {
			admitted++;
			dirty();
		}
	}

	/**
	 * Whether the spread has been gone for two rescans running. The debounce matters: one
	 * empty scan is a party in the moment between the last slice and the second course.
	 */
	public boolean spreadGone() {
		return emptyScans >= 2;
	}

	/** When the last guest stood up to leave, or 0 while one is still sitting. */
	public long closingAt() {
		return closingAt;
	}

	/**
	 * Bring the cached lists up to date, each on its own schedule. Server only: both sweeps
	 * read block entities.
	 */
	public void refresh(ServerLevel level, long gameTime) {
		var at = anchor;
		if (at == null) return;
		boolean changed = false;
		if (gameTime - foodScannedAt >= FOOD_RESCAN) {
			foodScannedAt = gameTime;
			var found = PartyArea.foodAround(level, at);
			if (!found.equals(food)) changed = true;
			food.clear();
			food.addAll(found);
			// two empty scans in a row end the party
			emptyScans = found.isEmpty() ? emptyScans + 1 : 0;
			// what the guests can actually reach, so hunger narrows from "the whole
			// scanned room" at send to "the guest box" afterwards
			var budget = 0;
			for (var pos : food) budget += PartyFoodValue.totalHunger(level, pos, level.getBlockState(pos));
			if (budget != hungerBudget) changed = true;
			hungerBudget = budget;
		}
		if (gameTime - layoutScannedAt >= LAYOUT_RESCAN) {
			layoutScannedAt = gameTime;
			var found = PartyArea.seatsAround(level, at);
			// bumped on a change only, so a guest comparing it every tick is not woken
			// by a rebuild that found exactly what it found last time
			if (!found.equals(seats)) {
				layoutVersion++;
				changed = true;
			}
			seats.clear();
			seats.addAll(found);
			// A gone chair releases its claim; without this the claim outlives the
			// block and holds the party in GATHERING.
			int before = claims.size();
			claims.keySet().removeIf(seat -> !seats.contains(seat));
			if (claims.size() != before) {
				layoutVersion++;
				changed = true;
			}
		}
		// Only when something moved: marking the chunk unsaved every twenty ticks would
		// queue it for a save on a timer for a rebuild that found the same room.
		if (changed) dirty();
	}

	/** Freeze the roster and open the party. Every rejection must happen before this. */
	public void start(LevelChunk chunk, long gameTime, UUID host, BlockPos anchor,
			List<EntityType<?>> roster, int hunger) {
		start(chunk, gameTime, host, anchor, roster, hunger, List.of(), List.of());
	}

	/**
	 * Freeze the roster, open the party, and seed the caches from the send's walk rather than
	 * sweeping again. Only food and chairs inside the guest box are seeded: a guest is only ever
	 * offered what she could walk to.
	 */
	public void start(LevelChunk chunk, long gameTime, UUID host, BlockPos anchor,
			List<EntityType<?>> roster, int hunger, List<BlockPos> food, List<BlockPos> seats) {
		this.owner = chunk;
		this.host = host;
		this.anchor = anchor.immutable();
		this.chunk = chunk.getPos();
		this.roster.clear();
		this.roster.addAll(roster);
		this.guests.clear();
		this.hungerBudget = hunger;
		this.hungerServed = 0;
		this.appealServed = 0;
		this.claims.clear();
		this.startedAt = gameTime;
		this.phase = Phase.GATHERING;
		this.admitted = 0;
		this.emptyScans = 0;
		this.closingAt = 0;
		this.foodScannedAt = gameTime;
		this.layoutScannedAt = gameTime;
		this.food.clear();
		this.food.addAll(food);
		this.seats.clear();
		this.seats.addAll(seats);
		layoutVersion++;
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

	/**
	 * Move to CLOSING. The backstop is deliberately <b>not</b> stamped here: it starts when the
	 * last guest stands up ({@link #startClosingClock}), because a guest still sitting out her
	 * unseat wait is not a guest who cannot leave.
	 */
	public void close() {
		setPhase(Phase.CLOSING);
	}

	/**
	 * Stamp the backstop, once. Called when the last guest is off her chair: from there the party
	 * is only waiting on walks, and a guest who cannot finish one is a stuck guest.
	 */
	public void startClosingClock(long gameTime) {
		if (closingAt > 0) return;
		closingAt = gameTime;
		dirty();
	}

	/** Hunger actually consumed by the guests so far. */
	public void serve(int hunger) {
		if (hunger > 0) {
			hungerServed += hunger;
			dirty();
		}
	}

	/** Hunger and appeal actually consumed by one serve, in their own units. */
	public void serve(int hunger, int appeal) {
		if (hunger > 0) hungerServed += hunger;
		if (appeal > 0) appealServed += appeal;
		if (hunger > 0 || appeal > 0) dirty();
	}

	/** Back to idle. The roster is kept so a command can still report on it. */
	public void reset() {
		host = null;
		anchor = null;
		phase = Phase.IDLE;
		guests.clear();
		food.clear();
		seats.clear();
		claims.clear();
		admitted = 0;
		emptyScans = 0;
		closingAt = 0;
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
	 * The live party overlapping these chunks, or null. Unloaded chunks are skipped rather
	 * than loaded: a party only exists while its chunk is loaded.
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
	 * The active party within {@code radius} blocks. A party lives in the chunk holding the
	 * <b>centre of its food</b>, not of its anchor table, so asking "is there a party at this
	 * table?" needs the neighbourhood rather than one chunk.
	 */
	@Nullable
	public static PartyAttachment near(ServerLevel level, BlockPos pos, int radius) {
		var party = at(level, pos);
		if (party != null) return party;
		return existing(level, ChunkPosRange.ofOwner(pos, radius));
	}

}

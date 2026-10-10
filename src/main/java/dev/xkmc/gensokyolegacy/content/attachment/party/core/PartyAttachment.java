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

	/**
	 * Appeal the guests have actually been served.
	 * <p>
	 * The third counter, and the one the host is paid from: what the table was worth by
	 * the end rather than what it was worth when the letter went out, so a second course
	 * counts and a dish nobody ever touched does not.
	 */
	@SerialField
	private int appealServed;

	/**
	 * How often the spread is looked at again, in ticks. Two seconds.
	 *
	 * <p>
	 * Fast, because food is the thing that changes fastest at a party: every dish a
	 * guest eats is one fewer, and a host putting out a second course wants it noticed
	 * while the table is still full of people rather than after they have gone.
	 */
	private static final int FOOD_RESCAN = 40;

	/**
	 * How often the furniture is looked at again, in ticks. Ten seconds.
	 *
	 * <p>
	 * Five times slower than the food, and for the same reason in reverse: chairs are not
	 * eaten, so a chair that moved a second ago will still be there in four more, and the
	 * list exists only so a guest knows where to sit and so a guest whose chair was taken
	 * away can notice she is standing.
	 */
	private static final int LAYOUT_RESCAN = 200;

	/**
	 * The spread as of the last food rescan: every dish a guest could eat.
	 *
	 * <p>
	 * A party's own copy rather than something each guest sweeps for herself. A guest
	 * needs this list every time she draws a dish, and she draws one every few seconds
	 * for the length of her visit; sweeping a box per guest per dish is the party
	 * costing far more to run than the party itself.
	 *
	 * <p>
	 * Serialized, so a party in a chunk that unloads resumes with the spread it had
	 * rather than with an empty one it would have to notice was wrong.
	 */
	@SerialField
	private final ArrayList<BlockPos> food = new ArrayList<>();

	/** Every chair as of the last layout rescan. */
	@SerialField
	private final ArrayList<BlockPos> seats = new ArrayList<>();

	/**
	 * Which guest holds which chair.
	 * <p>
	 * The party's own seating plan, and the reason guests no longer fight for chairs. Left
	 * to themselves they all walk to the <i>nearest</i> free one, which at a laid table is
	 * the same chair — {@code HomeSearchUtil.setSitting} ejects whoever is sitting there,
	 * so the second arrival wins and the first is stood up mid-party.
	 * <p>
	 * A claim is first-write-wins and survives everything except the chair disappearing:
	 * re-arranging the table does not move anyone, and a guest who stood up for a dish
	 * comes back to the same chair. Only a chair that is no longer there releases it, and
	 * that is what the layout rescan prunes.
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
	 * How many roster entries have been admitted, so the rest arrive one a second.
	 * <p>
	 * The roster is frozen at send (§4) but the guests are not all spawned by the send
	 * itself: {@link PartyTicker} admits one per pass, which is one a second because the
	 * ticker already runs at 1 Hz. A table that fills in a single frame reads as a cutscene.
	 */
	@SerialField
	private int admitted = 0;

	/**
	 * Consecutive food rescans that found nothing on the table.
	 * <p>
	 * One empty scan is not evidence of anything — the host may be carrying the second
	 * course in — so the party ends on the <b>second</b> one, 40 ticks after the first.
	 */
	@SerialField
	private int emptyScans = 0;

	/**
	 * When the party went to CLOSING.
	 * <p>
	 * A backstop for a guest whose walk home never finishes: the closing phase ends when
	 * the last guest has left, and this is what stops one stuck guest pinning the chunk's
	 * party open for ever.
	 */
	@SerialField
	private long closingAt = 0;

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
	 * The chair this guest gets, claiming it for her.
	 * <p>
	 * Her own chair if she has one and it is still on the list — first-write-wins, so a
	 * table rearranged under the party does not walk anyone off a chair they are already
	 * sitting on. Otherwise the free chair nearest where she is standing, so guests take
	 * the chair on their own side of the table rather than all crossing to one end of it.
	 * <p>
	 * <b>Null means there is nothing to give her</b>: every chair on the list is already
	 * claimed, or there are none. That is the whole of "no chair" — the caller asks
	 * whether a chair is hers rather than whether a chair exists, because another guest's
	 * chair is never offered to her twice.
	 *
	 * @param guest who is asking, by id: her position is what "nearest" is nearest to
	 * @param hers  the chair she believes she has, or null when she has none. Advisory
	 *              only — the party answers from its own record, so a guest that lost
	 *              hers to an interrupted walk is not given a second chair
	 */
	@Nullable
	public BlockPos claimSeat(ServerLevel level, UUID guest, @Nullable BlockPos hers) {
		// Her own claim first, whatever she believes about it. Asked by what the party
		// has on record rather than by what she has, because she does not record a chair
		// until she is actually sitting on it: a guest whose walk was interrupted before
		// she arrived would otherwise be handed a second chair on the next attempt and
		// leave the first one claimed for ever.
		for (var entry : claims.entrySet()) {
			if (!guest.equals(entry.getValue())) continue;
			if (seats.contains(entry.getKey())) return entry.getKey();
			claims.remove(entry.getKey());   // gone: the layout rescan has not caught up
			break;
		}
		// hers can only be stale here - it was not the party that granted it, or the chair
		// has already left the list above - so it is safe to give back
		if (hers != null) claims.remove(hers);
		var entity = level.getEntity(guest);
		BlockPos from = entity == null ? anchor : entity.blockPosition();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (var pos : seats) {
			if (claims.containsKey(pos)) continue;
			// the list is up to ten seconds old, so a chair broken since then would
			// otherwise hand out a block that is no longer there
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
	 * Give up this guest's claim on any chair, because she is leaving or has been sent
	 * to look again.
	 * <p>
	 * Without it a guest who left early would sit on the party's books for ever and the
	 * table could never fill: the seat count is not a limit on the roster, but a claim is
	 * a limit on the chairs, and those are the same number.
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
	 * The next character to admit, or null when the roster is exhausted.
	 * <p>
	 * {@code null} rather than a bounds-checked index because "the roster is finished" and
	 * "this entry is missing" are the same question to the caller, and one answer is
	 * enough.
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
	 * Whether the spread has been gone for two rescans running.
	 * <p>
	 * The debounce is the whole point: one empty scan is what a party looks like in the
	 * two seconds between a guest taking the last slice and the host putting down the cake.
	 */
	public boolean spreadGone() {
		return emptyScans >= 2;
	}

	/** When the party entered CLOSING, or 0 while it has not. */
	public long closingAt() {
		return closingAt;
	}

	/**
	 * Bring the cached lists up to date, each on its own schedule.
	 *
	 * <p>
	 * Called from the ticker rather than from a guest, so the cost is paid once per party
	 * per interval instead of once per guest per dish. The two lists are refreshed
	 * independently because they change at very different rates, and because conflating
	 * them would either make a refilled dish take ten seconds to appear or make the chair
	 * list be rebuilt four times a second for nothing.
	 *
	 * <p>
	 * Called on the server only. Both sweeps read block entities — a cutting board's
	 * contents, a rack's racks — and asking for those on the client would be a second,
	 * slightly different answer to a question the server is already answering.
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
			// two empty scans in a row end the party, one never does: the rescan is fast
			// so that a second course is noticed while there is still a table to notice it
			// at, and that same speed makes a single empty scan ambiguous
			emptyScans = found.isEmpty() ? emptyScans + 1 : 0;
			// The budget tracks what the guests can actually reach, and moves as the host
			// refills. Two consequences worth stating: a second course raises the party's
			// capacity instead of leaving the served count overtaking a stale number, and
			// the figure narrows from "the whole scanned room" at send to "the guest box"
			// afterwards - which is the honest answer, since nothing outside that box can
			// be eaten.
			var budget = 0;
			for (var pos : food) budget += PartyFoodValue.totalHunger(level, pos, level.getBlockState(pos));
			if (budget != hungerBudget) changed = true;
			hungerBudget = budget;
		}
		if (gameTime - layoutScannedAt >= LAYOUT_RESCAN) {
			layoutScannedAt = gameTime;
			var found = PartyArea.seatsAround(level, at);
			// a version bump on a change only, so a guest comparing it every tick is not
			// woken up by a rebuild that found exactly what it found last time
			if (!found.equals(seats)) {
				layoutVersion++;
				changed = true;
			}
			seats.clear();
			seats.addAll(found);
			// A chair that is gone releases its claim, and that is the only thing that
			// does. Without the prune the claim outlives the block and the guest cannot
			// be given a seat she has no claim to, which holds the party in GATHERING.
			int before = claims.size();
			claims.keySet().removeIf(seat -> !seats.contains(seat));
			if (claims.size() != before) {
				layoutVersion++;
				changed = true;
			}
		}
		// Only when something actually moved. This runs every twenty ticks for as long as
		// the party is open, and marking the chunk unsaved every time would queue it for a
		// save on a timer for the sake of a rebuild that found the same room.
		if (changed) dirty();
	}

	/** Freeze the roster and open the party. Every rejection must happen before this. */
	public void start(LevelChunk chunk, long gameTime, UUID host, BlockPos anchor,
			List<EntityType<?>> roster, int hunger) {
		start(chunk, gameTime, host, anchor, roster, hunger, List.of(), List.of());
	}

	/**
	 * Freeze the roster, open the party, and seed the caches from the walk that just
	 * found the spread.
	 *
	 * <p>
	 * The lists are passed rather than swept again because the send has just walked the
	 * whole room: asking a second time a moment later for an answer that is already in
	 * hand is the exact waste the cache exists to avoid. Only the dishes and chairs
	 * <i>inside the guest box</i> are seeded, though — the walk covers the room, and a
	 * guest is only ever offered what she could walk to.
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
	 * Move to CLOSING and stamp when it happened.
	 * <p>
	 * The stamp is the backstop for a guest whose walk off never finishes: closing ends
	 * when the last guest has left, and this is what stops one stuck guest from pinning
	 * the chunk's party open for ever.
	 */
	public void close(long gameTime) {
		if (phase == Phase.CLOSING) return;
		closingAt = gameTime;
		setPhase(Phase.CLOSING);
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

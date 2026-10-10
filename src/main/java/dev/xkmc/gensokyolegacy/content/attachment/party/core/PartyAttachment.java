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

	/** When each of the two lists was last rebuilt. */
	@SerialField
	private long foodScannedAt;
	@SerialField
	private long layoutScannedAt;

	/** Bumped whenever the chair list changes, so a guest can notice her chair is gone. */
	@SerialField
	private int layoutVersion;

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
		this.startedAt = gameTime;
		this.phase = Phase.GATHERING;
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
		food.clear();
		seats.clear();
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

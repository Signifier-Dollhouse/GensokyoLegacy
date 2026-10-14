package dev.xkmc.gensokyolegacy.content.entity.module;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.entity.behavior.task.party.PartyServe;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataHolder;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharacterConfig;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLBrains;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.util.BrainUtils;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A character at somebody's party: a temporary clone with no {@link HomeModule} binding, so the real
 * resident keeps living her own day. A character already on site joins as herself instead.
 *
 * @see "doc/design/party/guest.md"
 */
@SerialClass
public class PartyModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("party");

	/** How long a guest waits at the table before the party dismisses her. */
	public static final int ARRIVE_TIMEOUT = 1200;

	/**
	 * Ticks a visitor is given to walk off after a party that outlasted her visit. A
	 * {@link VisitModule#hold}: the visit window has nothing on it to extend.
	 */
	public static final int VISIT_GRACE = 200;

	/**
	 * Wander radius when restoring a character with no {@link CharacterConfig}, as {@code VisitScheduler.spawn} falls
	 * back to.
	 */
	private static final int FALLBACK_WANDER = 12;

	/**
	 * What she was doing before the party. Recorded at join: by the end her visit may
	 * be over and there is nothing left to work it out from.
	 */
	public enum Origin {
		/** A clone the party made: no home, nothing to return to, walks away and is gone. */
		SUMMONED,
		/** A resident of a home near the party; goes back to it. */
		HOME,
		/** A visitor in a home near the party; goes back to the visit. */
		VISITING
	}

	@Nullable
	@SerialField
	private UUID host;

	@SerialField
	private BlockPos table;

	@SerialField
	private Origin origin = Origin.SUMMONED;

	/**
	 * The home or the visit she came from, for the two origins that have one. Null for
	 * {@link Origin#SUMMONED}: a clone has nothing to go back to.
	 */
	@SerialField
	private StructureKey originKey;

	/**
	 * Whether she has been told to leave. The whole of the closing phase from her side: the party asks, it does not
	 * discard.
	 */
	@SerialField
	private boolean leaving = false;

	/**
	 * The chunk that owns the party - not derivable from {@link #table}, which is often in the next
	 * chunk over, and looking the party up by the table makes her dismiss herself.
	 */
	@SerialField
	private ChunkPos partyChunk;

	/**
	 * The chair she claimed. Held here rather than in the behaviour: a visit spans many dish
	 * cycles and the task is restarted on chunk unload.
	 */
	@SerialField
	private BlockPos seat;

	/** Whether she has claimed it. Gates the party's GATHERING -> FEASTING move. */
	@SerialField
	private boolean seated;

	/** Hunger this guest has personally been served; measured against her fulfillment. */
	@SerialField
	private int eaten = 0;

	/**
	 * Appeal this guest has personally been served - what her gift and the host's payout are sized on,
	 * since a delicacy moves her without feeding her any more than bread would.
	 */
	@SerialField
	private int appealEaten = 0;

	/** Whether she has left a gift: one per visit, not per departure - both leaving doors pay twice. */
	@SerialField
	private boolean gifted = false;

	// Durable visit bookkeeping, not routing state: a reload must not reset the count, or a guest
	// who cannot find a chair gives up in three ticks instead of nine seconds.

	@SerialField
	private int seatFailures = 0;

	@SerialField
	private boolean seatWarned = false;

	/** Paces the attempts; {@link #seatDeadline} is the backstop for the whole episode. */
	@SerialField
	private long seatCheckAt = 0;

	@SerialField
	private long seatDeadline = 0;

	/** The layout version when she last checked her own chair was still on the list. */
	@SerialField
	private int layoutVersion = -1;

	/**
	 * The block she was told to walk to, which is also the one the teleport fallback puts her on.
	 * A deadline from issuing the order is the wrong test — a long walk to a far chair is slow but
	 * fine — so the closest approach is kept instead.
	 */
	@SerialField
	private BlockPos walkTarget;

	@SerialField
	private double walkClosest = Double.MAX_VALUE;

	@SerialField
	private long walkClosestAt = 0;

	// Serialized here rather than in brain memories, which only survive a save if they
	// carry a codec. One convention: 0 is not running and a null dish or serve is not held.

	/** The dish she has committed to this round. */
	@SerialField
	public BlockPos serveDish;

	/** What she is carrying and what it is worth; cleared when the chew banks it. */
	@SerialField
	public PartyServe serve;

	@SerialField
	public int hesitate = 0;

	@SerialField
	public int grab = 0;

	@SerialField
	public int chew = 0;

	@SerialField
	public int rest = 0;

	/**
	 * Whether the departure is in progress. Its own flag rather than a sentinel in the timer,
	 * because zero has to mean "armed and already on her way".
	 */
	@SerialField
	public boolean leaveArmed = false;

	/** Ticks before she stands up. */
	@SerialField
	public int leave = 0;

	/**
	 * Where she is going on her way out, drawn once: a walk-away destination is random, so
	 * drawn per tick she never arrives anywhere.
	 */
	@SerialField
	public BlockPos leaveDest;

	/** Remember the one place she is walking to. First writer wins. */
	public void leaveTo(BlockPos dest) {
		if (leaveDest == null) leaveDest = dest.immutable();
	}

	/** The whole machine, wiped. Called on the way in and on the way out, never mid-visit. */
	public void clearMachine() {
		serveDish = null;
		serve = null;
		hesitate = 0;
		grab = 0;
		chew = 0;
		rest = 0;
		leaveArmed = false;
		leave = 0;
	}

	public PartyModule(YoukaiEntity self) {
		super(ID, self);
	}

	public boolean partying() {
		return host != null;
	}

	@Nullable
	public UUID host() {
		return host;
	}

	@Nullable
	public BlockPos table() {
		return table;
	}

	public int eaten() {
		return eaten;
	}

	/** Appeal she has personally been served; what her gift is sized on. */
	public int appealEaten() {
		return appealEaten;
	}

	/** Whether she has already left a gift this visit. */
	public boolean gifted() {
		return gifted;
	}

	/** Mark her gift as given. First writer wins, which is the whole of "one gift". */
	public void markGifted() {
		gifted = true;
	}

	/** Her claimed chair, or null before she has found one. */
	@Nullable
	public BlockPos seat() {
		return seat;
	}

	/** First claim wins, so a table rearranged under her cannot walk her off her chair. */
	public void claimSeat(BlockPos pos) {
		if (seat != null) return;
		this.seat = pos.immutable();
		this.seated = true;
	}

	/** Whether she has claimed a chair. Gates the party's GATHERING -> FEASTING move. */
	public boolean seated() {
		return seated;
	}

	/**
	 * Give up her chair because it is no longer there. A claim on a non-chair holds the party in GATHERING for ever.
	 */
	public void dropSeat(ServerLevel sl) {
		seat = null;
		seated = false;
		// the seating plan must hear about it, or that chair stays claimed for ever
		if (partyChunk != null) {
			var attachment = PartyAttachment.of(sl, partyChunk);
			if (attachment != null) attachment.releaseSeat(self.getUUID());
		}
	}

	/**
	 * This character's taste, or null when she has no authored {@code party_taste} — only
	 * reachable if a datapack removes the row mid-party.
	 */
	public PartyTaste taste() {
		return PartyTaste.of(self.registryAccess(), self.getType());
	}

	/** Begin attending. Called once, at spawn, by {@code PartySend}. */
	public void arrive(UUID host, BlockPos table, ChunkPos partyChunk) {
		enter(host, table, partyChunk, Origin.SUMMONED, null);
	}

	/** Begin attending as herself, found on site rather than summoned.
	 *
	 * @param origin    what she was doing before, which is what she goes back to
	 * @param originKey the home or the visit that was, or null for a plain clone
	 */
	public void join(@Nullable UUID host, BlockPos table, @Nullable ChunkPos partyChunk,
			Origin origin, @Nullable StructureKey originKey) {
		enter(host, table, partyChunk, origin, originKey);
	}

	private void enter(@Nullable UUID host, BlockPos table, @Nullable ChunkPos partyChunk,
			Origin origin, @Nullable StructureKey originKey) {
		this.host = host;
		this.table = table.immutable();
		this.partyChunk = partyChunk;
		this.origin = origin;
		this.originKey = originKey;
		this.leaving = false;
		self.setFlag(YoukaiFlags.PARTYING, true);
		clearMachine();
		// the flag alone stops her hovering; the flying navigation and no-gravity are
		// already installed by then
		self.navCtrl.setWalking();
	}

	/** Where she was before the party, and so what she is given back to. */
	public Origin origin() {
		return origin;
	}

	/** The home or visit she came from, or null for a summoned clone. */
	@Nullable
	public StructureKey originKey() {
		return originKey;
	}

	/** A request, not an action: the staggered exit is visible, so the task walks her out. */
	public void beginLeave() {
		leaving = true;
	}

	/** Whether she has been told to leave. */
	public boolean leaving() {
		return leaving;
	}

	/**
	 * Account for one dish just eaten. Both readings are taken before the block is consumed,
	 * and both are kept because they answer different questions.
	 */
	public void eat(int hunger, int appeal, PartyAttachment party) {
		eaten += hunger;
		appealEaten += appeal;
		party.serve(hunger, appeal);
	}

	public boolean satisfied() {
		var taste = ofOrDefault();
		// no taste means she should not be here at all, so she never counts as satisfied
		return eaten >= taste.fulfillment();
	}

	/**
	 * How long she sits between dishes while still hungry. The fallback for a tasteless
	 * guest is a real number, not zero — a sit of zero draws, eats and draws inside one tick.
	 */
	public int hungrySeat() {
		return ofOrDefault().hungrySeat();
	}

	/** How long she sits between dishes once she is satisfied. See {@link #hungrySeat}. */
	public int fullSeat() {
		return ofOrDefault().fullSeat();
	}

	/**
	 * Chance she leaves after a satisfied sit, 0..1 — the only reason a guest ever stops
	 * eating, since being satisfied only lengthens her sit.
	 */
	public double leaveChance() {
		return ofOrDefault().leaveChance();
	}

	/**
	 * Her tuning numbers, or the template if her row has gone: a row pulled mid-visit should
	 * cost her her manners, not her meal.
	 */
	public PartyTaste ofOrDefault() {
		var taste = taste();
		return taste == null ? PartyTaste.DEF : taste;
	}

	// --- seat episode ---

	/** Start a fresh attempt at finding a chair, forgetting the previous failures. */
	public void beginSeatEpisode(long gameTime, int retry, int attempts) {
		seatFailures = 0;
		seatWarned = false;
		seatDeadline = gameTime + (long) attempts * retry;
		seatCheckAt = gameTime;
	}

	/** When the next "is there a chair?" check counts, so it counts once per window. */
	public long seatCheckAt() {
		return seatCheckAt;
	}

	public void nextSeatCheck(long gameTime, int retry) {
		seatCheckAt = gameTime + retry;
	}

	/** Count one failed attempt at a chair, warning the host on the first.
	 *
	 * @return whether this is the first failure, so the caller can send the message
	 */
	public boolean countSeatFailure() {
		boolean first = !seatWarned;
		if (first) {
			seatWarned = true;
			seatFailures = 0;
		}
		seatFailures++;
		return first;
	}

	public boolean outOfSeatAttempts(long gameTime, int attempts) {
		return seatFailures >= attempts || gameTime > seatDeadline;
	}

	/**
	 * Whether the layout rescan has changed since she last looked, and if so whether her own
	 * chair is the thing that changed.
	 */
	public boolean layoutChanged(PartyAttachment party) {
		if (layoutVersion < 0 || layoutVersion == party.layoutVersion()) return false;
		layoutVersion = party.layoutVersion();
		return seat != null && !party.seats().contains(seat);
	}

	public void forgetLayoutVersion() {
		layoutVersion = -1;
	}

	// --- walk fallback ---

	/** Remember where she is walking to, and how close she has ever got. */
	public void walkingTo(@Nullable BlockPos target, long gameTime) {
		if (walkTarget != null && walkTarget.equals(target)) return;
		walkTarget = target;
		noteWalk(gameTime);
	}

	public void arrived() {
		walkTarget = null;
		walkClosest = Double.MAX_VALUE;
	}

	/** The block she is currently walking to, for the teleport fallback. */
	@Nullable
	public BlockPos walkTarget() {
		return walkTarget;
	}

	/**
	 * Called every tick while she is walking somewhere. Records a new closest approach, and
	 * with it the moment she last got nearer.
	 */
	public void noteWalk(long gameTime) {
		if (walkTarget == null) return;
		double distance = self.distanceToSqr(walkTarget.getCenter());
		if (distance >= walkClosest) return;
		walkClosest = distance;
		walkClosestAt = gameTime;
	}

	/**
	 * Whether she has stopped getting nearer to {@link #walkTarget} for {@code timeout} ticks.
	 * A guest crossing a long room resets this every time she gains a block.
	 */
	public boolean walkStalled(long gameTime, int timeout) {
		return walkTarget != null && gameTime - walkClosestAt >= timeout;
	}

	@Override
	public void tickServer() {
		if (!(self.level() instanceof ServerLevel sl)) return;
		if (host == null) return;
		// the party's state, not the host's presence: chunk unload pauses everyone
		var party = party(sl);
		if (party == null) {
			dismiss();
			return;
		}
		BrainUtils.setMemory(self, GLBrains.MEM_PARTY.get(), new GlobalPos(sl.dimension(), table));
	}

	/** The host's party, or null once it has ended or its chunk unloaded. */
	@Nullable
	public PartyAttachment party(ServerLevel sl) {
		var where = partyChunk != null ? partyChunk : (table == null ? null : new ChunkPos(table));
		if (where == null) return null;
		var party = PartyAttachment.of(sl, where);
		if (party == null || !party.active()) return null;
		// a CLOSING party is still hers: she needs it to run her exit, and it only goes null
		// once the party has reset, by which time she has left
		return party;
	}

	/** Take her off the party's roster, so nobody has to go looking for her body. */
	private void dropFromRoster(ServerLevel sl) {
		if (partyChunk == null) return;
		var party = PartyAttachment.of(sl, partyChunk);
		if (party != null) party.removeGuest(self.getUUID());
	}

	/**
	 * The hard exit, for when there is nothing to walk away from. A summoned clone is discarded; one
	 * who came as herself is put back, since discarding a resident deletes a real character.
	 */
	public void dismiss() {
		if (!(self.level() instanceof ServerLevel sl)) {
			leave();
			return;
		}
		completeLeave(sl);
	}

	/**
	 * Come off the party's books, then restore {@link #origin}. The origin is read before
	 * {@link #leave()} clears it, or she gets her schedule back while still walking to the table.
	 */
	public void completeLeave(ServerLevel sl) {
		// the exit runs from two places in one tick — the module's own tick and the director — and
		// leave() has already reset origin to SUMMONED, so a second run would read a resident as a
		// clone and discard her
		if (host == null) return;
		var was = origin;
		var key = originKey;
		if (partyChunk != null) {
			var party = PartyAttachment.of(sl, partyChunk);
			if (party != null) {
				party.releaseSeat(self.getUUID());
				// paid before the party is dropped: only the party knows what the
				// table was worth
				payHost(sl, party);
				party.removeGuest(self.getUUID());
			}
		}
		leave();
		switch (was) {
			case SUMMONED -> self.discard();
			case HOME -> restoreWander(sl, key);
			case VISITING -> restoreVisit(sl, key);
		}
	}

	/**
	 * Pay the host a share of the appeal <b>she personally consumed</b>, the same quantity her gift is
	 * sized on. Skipped when she never ate, or too few chairs becomes a way to farm reputation.
	 */
	private void payHost(ServerLevel sl, PartyAttachment party) {
		if (appealEaten <= 0) return;
		var host = party.host();
		if (host == null) return;
		var player = sl.getPlayerByUUID(host);
		if (player == null) return;
		var taste = ofOrDefault();
		int value = taste.reputationFor(appealEaten);
		if (value <= 0) return;
		CharDataHolder.getUnbounded(player, self.getType()).gain(value, 0, 0, 0);
	}

	/**
	 * Put a resident back inside her own wander area. {@code restrictTo} has no getter, so
	 * it is re-applied from her home — otherwise she keeps the party's guest radius for life.
	 */
	private void restoreWander(ServerLevel sl, @Nullable StructureKey key) {
		if (key == null) return;
		var home = IHomeHolder.of(sl, key);
		var center = home == null ? null : home.getWanderCenter();
		if (home == null || center == null) return;
		var config = CharacterConfig.of(self.getType());
		int wander = config == null ? FALLBACK_WANDER : config.wanderRadius();
		self.restrictTo(center, home.getWanderBaseRadius() + wander);
	}

	/**
	 * Give a visitor back to her visit, or ten seconds to walk off if her window closed while she was eating.
	 * A {@link VisitModule#hold} rather than a discard: the visit module refuses to remove a visible character.
	 */
	private void restoreVisit(ServerLevel sl, @Nullable StructureKey key) {
		var visit = self.getModule(VisitModule.class).orElse(null);
		if (visit == null) return;
		if (visit.remaining(sl) <= 0) {
			visit.hold(VISIT_GRACE);
			return;
		}
		restoreWander(sl, key != null ? key : visit.host());
	}

	/** Clear the party state but leave her standing, so the caller decides. */
	public void leave() {
		BrainUtils.clearMemory(self, GLBrains.MEM_PARTY.get());
		clearMachine();
		self.setFlag(YoukaiFlags.PARTYING, false);
		host = null;
		table = null;
		partyChunk = null;
		seat = null;
		seated = false;
		origin = Origin.SUMMONED;
		originKey = null;
		leaving = false;
		walkTarget = null;
		walkClosest = Double.MAX_VALUE;
		leaveDest = null;
		seatFailures = 0;
		seatWarned = false;
		layoutVersion = -1;
	}

	@Override
	public void onKilled() {
		// Off the roster here, at the source: getEntity only finds loaded entities, so a
		// sweep for "missing" guests cannot tell an unloaded guest from a corpse.
		if (self.level() instanceof ServerLevel sl) dropFromRoster(sl);
		leave();
	}

}

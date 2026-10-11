package dev.xkmc.gensokyolegacy.content.entity.module;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
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
 * A character who is at somebody's party: a temporary clone with no {@link HomeModule}
 * binding, so the real resident keeps living her own day. One exception — a character who
 * was already on site joins as herself, and {@link #origin} is what she is given back to.
 *
 * @see doc/design/party/guest.md
 */
@SerialClass
public class PartyModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("party");

	/** How long a guest waits at the table before the party dismisses her. */
	public static final int ARRIVE_TIMEOUT = 1200;

	/**
	 * Ticks a visitor is given to walk off after a party that outlasted her visit. A
	 * {@link VisitModule#hold} rather than an extended window: the window is a pure
	 * function of (structure pos, type, day) with nothing to extend.
	 */
	public static final int VISIT_GRACE = 200;

	/** Wander radius when restoring a character with no {@link CharacterConfig}, as {@code VisitScheduler.spawn} falls back to. */
	private static final int FALLBACK_WANDER = 12;

	/**
	 * What she was doing before the party. Recorded at join, not worked out at the end:
	 * by then her visit may be over and there is nothing left to work it out from.
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

	/** Whether she has been told to leave. The whole of the closing phase from her side: the party asks, it does not discard. */
	@SerialField
	private boolean leaving = false;

	/**
	 * The chunk that owns the party — <b>not</b> derivable from {@link #table}, which is
	 * often in the next chunk over. Looking the party up by the table would miss it and the
	 * guest would dismiss herself on her first tick.
	 */
	@SerialField
	private ChunkPos partyChunk;

	/**
	 * The chair she claimed, held here rather than in the behaviour: a visit spans many dish
	 * cycles and the task is restarted on chunk unload, which would strand her.
	 */
	@SerialField
	private BlockPos seat;

	/**
	 * Whether she has claimed it. The party stays in GATHERING until every guest has,
	 * and nobody eats until then.
	 */
	@SerialField
	private boolean seated;

	/** Hunger this guest has personally been served; measured against her fulfillment. */
	@SerialField
	private int eaten = 0;

	/**
	 * Appeal this guest has personally been served — the food measurement her gift depends
	 * on, since a delicacy moves her without feeding her any more than bread would.
	 */
	@SerialField
	private int appealEaten = 0;

	/**
	 * Whether she has left a gift — one per visit, not one per departure. She can pass
	 * through both leaving doors in one visit, which would pay the host twice.
	 */
	@SerialField
	private boolean gifted = false;

	// --- seat episode ---
	//
	// Durable visit bookkeeping rather than routing state, which is why it lives here and
	// not in a brain memory: it records what happened to her, not which task she is in.
	// A chunk unload or a restart must not reset the count, or a guest who cannot find a
	// chair gives up in three ticks instead of nine seconds.

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
	 * The thing she is walking to, and when she stopped closing on it.
	 *
	 * <p>
	 * A deadline from issuing the order was the wrong test: a long walk to a far chair is
	 * slow but fine, and it would be declared stuck while she was halfway across a room.
	 * What actually distinguishes a stuck guest is that she stops getting nearer, so the
	 * closest approach is kept and reset whenever she beats it.
	 */
	@SerialField
	private BlockPos walkTarget;

	@SerialField
	private double walkClosest = Double.MAX_VALUE;

	@SerialField
	private long walkClosestAt = 0;

	/**
	 * Where she is going on her way out, drawn once.
	 *
	 * <p>
	 * Drawn once and kept, because the obvious implementation draws it afresh every time it
	 * is asked and a walk-away destination is random. That made the guest re-roll her
	 * destination every tick, so she never arrived anywhere, never looked stuck — because
	 * she was walking, constantly — and never triggered the stall repair either, since the
	 * target it watches was itself moving. She paced about the party for ever.
	 */
	@SerialField
	private BlockPos leaveDest;

	/** Remember the one place she is walking to. First writer wins. */
	public void leaveTo(BlockPos dest) {
		if (leaveDest == null) leaveDest = dest.immutable();
	}

	@Nullable
	public BlockPos leaveDestination() {
		return leaveDest;
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
	 * Give up her chair because it is no longer there. A claim on a block that is no longer
	 * a chair would hold the party in GATHERING for ever.
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

	/**
	 * Begin attending as herself, having been found on site rather than summoned.
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
		// and she arrives on the ground: the flag alone stops her hovering, but the
		// flying navigation and no-gravity are already installed by then
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
	 * Account for one dish just eaten. Both readings are taken before the block was
	 * removed, and both are kept because they answer different questions.
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
	 * guest is a real number, not zero — a sit of zero would have her draw, eat and draw
	 * again inside one tick.
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
	 * Her tuning numbers, or the template if her row has gone — for reading a guest already
	 * at the table, where a row pulled mid-visit should cost her her manners, not her meal.
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

	/**
	 * Count one failed attempt at a chair, warning the host on the first.
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

	/** The thing she is currently walking to, for the teleport fallback. */
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
	 * Whether she has stopped getting nearer to it for {@code timeout} ticks.
	 *
	 * <p>
	 * Read together with {@link #noteWalk}, which is what makes this true: a guest
	 * crossing a long room resets it every time she gains a block.
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
		// a CLOSING party is still hers: she needs it to run her exit, and it only goes
		// null once the party has reset, by which time she has left
		return party;
	}

	/**
	 * The hard exit, for when there is nothing to walk away from. A summoned clone is
	 * discarded; a character who came as herself is put back — discarding a resident would
	 * delete a real character out of the world over a party.
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
	 * {@link #leave()} clears it: clearing first hands her back her schedule while she is
	 * still walking to the table.
	 */
	public void completeLeave(ServerLevel sl) {
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
	 * Pay the host a share of the appeal <b>she personally consumed</b> — the same quantity
	 * her gift is sized on, because a host is paid for feeding <i>her</i>, not for the size
	 * of the table she sat at.
	 * <p>
	 * Skipped when she never ate: a guest who found no chair enjoyed nothing, and paying
	 * out for her would make putting out too few chairs a way to farm reputation.
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
	 * it has to be re-applied from her home — otherwise she keeps the party's guest radius
	 * for life and a resident can never go home again.
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
	 * Give a visitor back to her visit, or ten seconds to walk off if her window closed
	 * while she was eating. A {@link VisitModule#hold} rather than a discard: the visit
	 * module refuses to remove a character somebody can see, for as long as the hold runs.
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
		BrainUtils.clearMemories(self,
				GLBrains.MEM_PARTY.get(),
				GLBrains.MEM_PARTY_SERVE.get(),
				GLBrains.MEM_PARTY_DISH.get(),
				GLBrains.MEM_PARTY_HESITATE.get(),
				GLBrains.MEM_PARTY_GRAB.get(),
				GLBrains.MEM_PARTY_CHEW.get(),
				GLBrains.MEM_PARTY_REST.get(),
				GLBrains.MEM_PARTY_LEAVE.get());
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
		leave();
	}

}

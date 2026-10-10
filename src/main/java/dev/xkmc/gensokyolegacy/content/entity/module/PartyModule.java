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
 * A character who is at somebody's party.
 * <p>
 * Like {@link VisitModule} she is a temporary clone with no {@link HomeModule}
 * binding, so she can neither sleep nor claim a bed and the real resident keeps
 * living her own day. What she is here for is different: she eats, and she leaves
 * a gift.
 * <p>
 * One exception to the clone rule: a character who was <i>already</i> on site — a visitor,
 * or a resident standing at her own house — joins as herself rather than as a second copy
 * of her. She then needs somewhere to go back to, which is what {@link #origin} records
 * and what {@link #completeLeave} restores.
 * <p>
 * {@code eaten} is her own tally of the hunger she has been served, and
 * {@code appealEaten} the matching tally of appeal — the two quantities the food is
 * measured in (design doc §5.2). The planner guarantees the *planned* total fits the
 * spread, but the player can break a cake mid-party and two guests can converge on the
 * same dish, so fulfillment is checked at runtime rather than assumed from the plan.
 */
@SerialClass
public class PartyModule extends AbstractYoukaiModule {

	private static final ResourceLocation ID = GensokyoLegacy.loc("party");

	/**
	 * How long a guest waits at the table before the host's party gives up on
	 * her and dismisses her.
	 */
	public static final int ARRIVE_TIMEOUT = 1200;

	/**
	 * Ticks a visitor is given to walk off after a party that outlasted her visit.
	 * <p>
	 * Ten seconds, and a {@link VisitModule#hold} rather than an extended window: the
	 * window is a pure function of (structure pos, type, day) with no field to extend,
	 * and {@code hold} already exists for exactly this kind of "not quite yet".
	 */
	public static final int VISIT_GRACE = 200;

	/**
	 * Wander radius used when restoring a character with no {@link CharacterConfig}
	 * entry — the same default {@code VisitScheduler.spawn} falls back to.
	 */
	private static final int FALLBACK_WANDER = 12;

	/**
	 * What she was doing before the party, so it can be given back to her afterwards.
	 * <p>
	 * Recorded when she joins rather than worked out at the end, because by then there is
	 * nothing left to work it out from: the party is closing, her visit may be over, and
	 * the only version of "where does she go" that was ever true is the one from before.
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
	 * The home or the visit she came from, for the two origins that have one.
	 * <p>
	 * Null for {@link Origin#SUMMONED}, which is the ordinary case — a clone has nothing
	 * to go back to, and that is precisely why leaving her is one line.
	 */
	@SerialField
	private StructureKey originKey;

	/**
	 * Whether she has been told to leave.
	 * <p>
	 * Set by the party when it closes, and read by the task to run the exit. It is the
	 * whole of the closing phase from the guest's side: the party does not discard her,
	 * it asks her to go.
	 */
	@SerialField
	private boolean leaving = false;

	/**
	 * The chunk that owns the party.
	 * <p>
	 * Not derivable from {@link #table}: a party is owned by the chunk holding the
	 * <b>centre of its food</b>, which is anywhere within the scan's reach of the
	 * anchor table and so often in the next chunk over. Looking the party up with
	 * {@code new ChunkPos(table)} would miss it and the guest would dismiss herself
	 * on her first tick.
	 */
	@SerialField
	private ChunkPos partyChunk;

	/**
	 * The chair she claimed, so she can go back to it between dishes.
	 * <p>
	 * Held here rather than in the behaviour: a guest's visit spans many dish cycles and
	 * can be interrupted at any point by the chunk unloading, which stops the task and
	 * starts a fresh one. Losing her chair across that would strand her standing at
	 * whichever table she happened to be eating from.
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
	 * Appeal this guest has personally been served, tracked alongside {@link #eaten}.
	 * <p>
	 * The second of the two food measurements (design doc §5.2), and the only one her
	 * gift depends on: a guest who ate rare-effect delicacies is generous even where they
	 * fed her no more than plain bread would have.
	 */
	@SerialField
	private int appealEaten = 0;

	/**
	 * Whether she has left a gift.
	 * <p>
	 * One gift, not one per departure. She leaves through two doors — running out of food
	 * to reach, or rolling {@code leaveChance} — and a guest who is written off mid-party
	 * can pass through both in a way that would otherwise pay the host twice for one
	 * visit. A flag rather than a check on the tallies because "has she eaten" and "has
	 * she said thank you" are different questions.
	 */
	@SerialField
	private boolean gifted = false;

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

	/**
	 * Claim {@code pos} as her chair. First claim wins: a guest who has already sat down
	 * keeps her seat even if another chair is nearer on a later pass, so a guest cannot be
	 * walked off her chair by a table that has been rearranged under her.
	 */
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
	 * Give up her chair, because it is no longer there.
	 *
	 * <p>
	 * Not first-write-wins any more: {@link #claimSeat} keeps her on the chair she took,
	 * but the layout rescan can find that the block is gone, and a claim to a block that
	 * is no longer a chair is a claim that would hold the party in GATHERING for ever.
	 */
	public void dropSeat(ServerLevel sl) {
		seat = null;
		seated = false;
		// the chair is not hers any more, and the party's seating plan has to hear about
		// it or that chair stays claimed for ever and a later guest is sent to look for
		// one that is being sat on
		if (partyChunk != null) {
			var attachment = PartyAttachment.of(sl, partyChunk);
			if (attachment != null) attachment.releaseSeat(self.getUUID());
		}
	}

	/**
	 * This character's taste, or null when she has no authored {@code party_taste}.
	 * <p>
	 * Null is only reachable by a guest who should not be here: she is spawned from a
	 * roster that requires a taste, so this can happen if a datapack removes the entry
	 * mid-party.
	 * {@link #satisfied()} treats it as "she cannot be satisfied" rather than throwing.
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

	/**
	 * The party has closed: stand up and go, on the task's own schedule.
	 * <p>
	 * A request rather than an action, and that is the point. The staggered exit is
	 * visible — a table that empties in one frame reads as a cutscene — so the party does
	 * not drop her where she stands and let the task walk her out.
	 */
	public void beginLeave() {
		leaving = true;
	}

	/** Whether she has been told to leave. */
	public boolean leaving() {
		return leaving;
	}

	/**
	 * Account for one dish just eaten.
	 * <p>
	 * Both readings are taken before the block was removed, and both are kept, because
	 * they answer different questions: {@code hunger} is what decides whether she is
	 * {@link #satisfied()}, and {@code appeal} is what her gift is later sized on. A guest
	 * fed plain bread is satisfied by it and pays for it accordingly; a guest fed
	 * delicacies that fill her up no better pays more.
	 */
	public void eat(int hunger, int appeal, PartyAttachment party) {
		eaten += hunger;
		appealEaten += appeal;
		party.serve(hunger, appeal);
	}

	public boolean satisfied() {
		var taste = ofOrDefault();
		// no taste means she should not be here at all, so she never counts as done
		return eaten >= taste.fulfillment();
	}

	/**
	 * How long she sits at her chair between dishes while she is still hungry.
	 *
	 * <p>
	 * Falls back to the plain number for a guest with no taste, which should not exist -
	 * see {@link #taste()}. The fallback is a real value rather than a zero: a sit of zero
	 * would have her drawing, eating and drawing again inside a single tick, and it only
	 * ever applies to a guest who was never going to be satisfied anyway.
	 */
	public int hungrySeat() {
		return ofOrDefault().hungrySeat();
	}

	/** How long she sits between dishes once she is satisfied. See {@link #hungrySeat}. */
	public int fullSeat() {
		return ofOrDefault().fullSeat();
	}

	/**
	 * Chance she leaves after a satisfied sit, 0..1.
	 *
	 * <p>
	 * The reason a guest ever stops eating. Being satisfied only lengthens her sit; on its
	 * own that would keep her at the table until the host's food ran out, which is a long
	 * time to occupy a chair at a party the player is trying to enjoy.
	 */
	public double leaveChance() {
		return ofOrDefault().leaveChance();
	}

	/**
	 * Her tuning numbers, or the template if her row has gone.
	 * <p>
	 * {@link #taste()} is the strict read, for the questions where an absent row means
	 * "this character is not party-eligible". This one is for reading a guest who is
	 * <i>already</i> at the table, where a row pulled mid-visit should cost her the rest
	 * of her manners rather than the rest of her meal.
	 */
	public PartyTaste ofOrDefault() {
		var taste = taste();
		return taste == null ? PartyTaste.DEF : taste;
	}

	@Override
	public void tickServer() {
		if (!(self.level() instanceof ServerLevel sl)) return;
		if (host == null) return;
		// the party's own state, not the host's presence: a guest stays at the
		// table until the party ends, so chunk unload simply pauses everyone
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
		// A CLOSING party is still hers: she needs it to run her exit, and it only goes
		// null once the party has reset, by which time she has already left.
		return party;
	}

	/**
	 * End the party and drop the guest.
	 * <p>
	 * The hard exit, for the cases where there is nothing to walk away from — she died,
	 * her chunk went away, the party's chunk is gone. A summoned clone is discarded; a
	 * character who came as herself is put back where she came from instead, because
	 * discarding a resident would delete a real character out of the world over a party.
	 */
	public void dismiss() {
		if (!(self.level() instanceof ServerLevel sl)) {
			leave();
			return;
		}
		completeLeave(sl);
	}

	/**
	 * Finish leaving: come off the party's books, then do whatever {@link #origin} says
	 * she owes the rest of the world.
	 * <p>
	 * The origin is read before {@link #leave()} clears it, and the walk home has already
	 * happened by the time this is called — clearing the party state first would hand her
	 * back her schedule while she was still walking to the table.
	 */
	public void completeLeave(ServerLevel sl) {
		var was = origin;
		var key = originKey;
		if (partyChunk != null) {
			var party = PartyAttachment.of(sl, partyChunk);
			if (party != null) {
				party.releaseSeat(self.getUUID());
				// paid before the party is dropped off the books: the payout is a
				// function of what the table was worth, which only the party knows
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
	 * Pay the host for this guest's visit, in reputation with her.
	 *
	 * <p>
	 * A share of the appeal <b>she personally consumed</b>, which is the same quantity her
	 * gift is sized on (§6.4) and for the same reason: a host is paid for feeding
	 * <i>her</i>, not for the size of the table she happened to sit at. Two guests at one
	 * party can be worth wildly different amounts, and that is the honest reading — a
	 * character who was served a delicacy and left content is a better advertisement for
	 * the host than one who was handed a slice of bread and left because she was hungry.
	 *
	 * <p>
	 * It also ties the loop shut: the spread decides who comes (§5.2), the guests decide
	 * how long the party lasts (§6.6), and what she was actually served decides what the
	 * host earned from her.
	 *
	 * <p>
	 * Skipped when she never ate. A guest who found no chair and walked away enjoyed
	 * nothing, and paying out for her would make putting out too few chairs a way to farm
	 * reputation rather than a mistake.
	 *
	 * <p>
	 * Nothing is paid to a host who has logged off or changed dimension, exactly as with
	 * the party's messages: there is no queue of things that happened while they were not
	 * here.
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
	 * Put a resident back inside her own wander area.
	 * <p>
	 * {@code restrictTo} has no getter, so the restriction cannot be read back and has to
	 * be re-applied from her home — which is the same pair of numbers
	 * {@code CharacterConfig.create} gives her when she is first spawned. Without this a
	 * reused character would keep the party's guest radius for the rest of her life, and
	 * a resident who wandered to a party could never go home again.
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
	 * Give a visitor back to her visit — or, if the window closed while she was eating,
	 * ten seconds to walk off and then let the visit's own expiry take her.
	 * <p>
	 * A {@link VisitModule#hold} rather than a discarded entity: the visit module already
	 * refuses to remove a character somebody can see, and it keeps refusing for as long as
	 * the hold runs. So she is never blinked out in front of the host, and needs no special
	 * case for being on her way out.
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
		self.setFlag(YoukaiFlags.PARTYING, false);
		host = null;
		table = null;
		partyChunk = null;
		seat = null;
		seated = false;
		origin = Origin.SUMMONED;
		originKey = null;
		leaving = false;
	}

	@Override
	public void onKilled() {
		leave();
	}

}

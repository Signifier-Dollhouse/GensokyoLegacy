package dev.xkmc.gensokyolegacy.content.entity.module;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyTaste;
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

	@Nullable
	@SerialField
	private UUID host;

	@SerialField
	private BlockPos table;

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
	public void dropSeat() {
		seat = null;
		seated = false;
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
		this.host = host;
		this.table = table.immutable();
		this.partyChunk = partyChunk;
		self.setFlag(YoukaiFlags.PARTYING, true);
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
		party.serve(hunger);
	}

	public boolean satisfied() {
		var taste = taste();
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
		var taste = taste();
		return taste.hungrySeat();
	}

	/** How long she sits between dishes once she is satisfied. See {@link #hungrySeat}. */
	public int fullSeat() {
		var taste = taste();
		return taste.fullSeat();
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
		var taste = taste();
		return taste.leaveChance();
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
		// a party that has moved on to CLOSING or IDLE is no longer feeding us
		return party.phase() == PartyAttachment.Phase.CLOSING ? null : party;
	}

	/** End the party and drop the guest. */
	public void dismiss() {
		if (!(self.level() instanceof ServerLevel sl)) {
			leave();
			return;
		}
		if (partyChunk != null) {
			var party = PartyAttachment.of(sl, partyChunk);
			if (party != null) party.removeGuest(self.getUUID());
		}
		leave();
		self.discard();
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
	}

	@Override
	public void onKilled() {
		leave();
	}

}

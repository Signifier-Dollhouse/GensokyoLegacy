package dev.xkmc.gensokyolegacy.content.entity.module;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiFlags;
import dev.xkmc.gensokyolegacy.content.attachment.party.core.PartyAttachment;
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
 * {@code eaten} is her own tally of the appeal she has been served. The planner
 * guarantees the *planned* total fits the spread, but the player can break a
 * cake mid-party and two guests can converge on the same dish, so fulfillment is
 * checked at runtime rather than assumed from the plan.
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

	/** Appeal this guest has personally been served. */
	@SerialField
	private int eaten = 0;

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

	/**
	 * This character's taste, or null when she has no authored {@code party_taste}.
	 * <p>
	 * Null is only reachable by a guest who should not be here: she is spawned from a
	 * roster that requires a taste, so this can happen if a datapack removes the entry
 * mid-party, or for an entity saved before the entry became the eligibility list.
	 * {@link #satisfied()} and {@link #fulfillment()} both treat it as "she cannot be
	 * satisfied" rather than throwing.
	 */
	@Nullable
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
	 * Account for one dish just eaten: her own tally, and the party's shared total.
	 * Read the appeal before the block was removed.
	 */
	public void eat(int appeal, PartyAttachment party) {
		eaten += appeal;
		party.serve(appeal);
	}

	public boolean satisfied() {
		var taste = taste();
		// no taste means she should not be here at all, so she never counts as done
		return taste != null && eaten >= taste.fulfillment();
	}

	/**
	 * 0..1 — how well she was actually served, against what she wanted. A guest
	 * who got less than she needed still leaves a gift, but a smaller one.
	 */
	public double fulfillment() {
		var taste = taste();
		return taste == null ? 0.0 : Math.min(1.0, eaten / (double) taste.fulfillment());
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
	}

	@Override
	public void onKilled() {
		leave();
	}

}

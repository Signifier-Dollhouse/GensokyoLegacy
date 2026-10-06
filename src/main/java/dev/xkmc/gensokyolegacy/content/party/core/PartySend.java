package dev.xkmc.gensokyolegacy.content.party.core;

import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * The one place a party is started, and the one place guests are spawned.
 * <p>
 * Every rejection happens before the letter is consumed, so a refused send costs
 * the player nothing and tells them why.
 */
public class PartySend {

	/**
	 * A whole send attempt. Reusable by {@code /gensokyo party preview}, which
	 * wants the plan without any of the side effects.
	 */
	public record Plan(PartyArea area, List<PartyPlanner.Candidate> pool, List<EntityType<?>> roster) {

		public int appeal() {
			return area.appeal();
		}

		public int seats() {
			return area.seats().size();
		}

		public boolean empty() {
			return roster.isEmpty();
		}
	}

	/** Why a send was refused. */
	public enum Refusal {
		NO_FOOD, NO_SEATS, PARTY_EXISTS, NOBODY_CAME
	}

	/** The full outcome, so the caller can message the player without re-deriving anything. */
	public record Outcome(@Nullable Plan plan, @Nullable Refusal refusal,
			@Nullable Map.Entry<PartyPlanner.Candidate, PartyPlanner.Miss> miss) {

		public boolean ok() {
			return refusal == null && plan != null;
		}
	}

	/**
	 * Work out who would come, without touching anything.
	 *
	 * @param reputation how a character is to be scored; supplied so preview and
	 *                   send cannot drift apart
	 */
	public static Outcome plan(ServerLevel level, ServerPlayer sp, BlockPos anchor,
			java.util.function.ToIntFunction<EntityType<?>> reputation) {
		var area = PartyArea.scan(level, anchor, GLModConfig.SERVER.partyScanRadius.get());
		if (area.food().isEmpty()) return new Outcome(null, Refusal.NO_FOOD, null);
		// chairs are a hard requirement: no seats, no party
		if (area.seats().isEmpty()) return new Outcome(null, Refusal.NO_SEATS, null);
		var touched = PartyArea.touchedChunks(area.food());
		if (touched != null && PartyAttachment.existing(level, touched) != null)
			return new Outcome(null, Refusal.PARTY_EXISTS, null);

		var pool = new ArrayList<PartyPlanner.Candidate>();
		for (var type : BuiltInRegistries.ENTITY_TYPE) {
			// every character is party-eligible; the taste only sets how fussy she is.
			// Only youkai are candidates, so the chair entity and the doll stay out.
			if (!isCharacter(type)) continue;
			var taste = PartyTaste.of(level.registryAccess(), type);
			int rep = reputation.applyAsInt(type);
			pool.add(new PartyPlanner.Candidate(type,
					PartyMath.weight(taste, area.appeal(), rep), taste.fulfillment(), rep));
		}
		var roster = PartyPlanner.select(pool, area.appeal(), area.seats().size());
		if (roster.isEmpty()) {
			return new Outcome(null, Refusal.NOBODY_CAME, PartyPlanner.nearestMiss(pool, area.appeal()));
		}
		return new Outcome(new Plan(area, pool, roster), null, null);
	}

	/** The player's own reputation with a character type, for {@link #plan}. */
	public static ToIntFunction<EntityType<?>> reputationOf(ServerPlayer sp) {
		var chars = GLMeta.CHAR.type().getOrCreate(sp);
		return type -> chars.getUnbounded(sp, type).data().reputation;
	}

	/**
	 * Whether this entity type is a character who could be invited. Every one of
	 * them is eligible; the {@code party_taste} data map only makes a particular
	 * character pickier or more generous than the default.
	 * <p>
	 * All fourteen characters are registered as {@link MobCategory#MONSTER}, which is
	 * what separates them from the chair, the broom, the doll and the projectile
	 * entities — all {@code MISC}. Structural rather than a hand-kept list, so a
	 * character added later is party-eligible without a second edit.
	 */
	public static boolean isCharacter(EntityType<?> type) {
		return type.getCategory() == MobCategory.MONSTER;
	}

	/**
	 * A real send: plan it, then commit.
	 *
	 * @return what happened, for the caller to report
	 */
	public static Outcome send(ServerPlayer sp, BlockPos anchor, ItemStack letter) {
		var level = sp.serverLevel();
		if (!level.getBlockState(anchor).is(GLTagGen.PARTY_TABLE)) return new Outcome(null, Refusal.NO_FOOD, null);
		var outcome = plan(level, sp, anchor, reputationOf(sp));
		if (!outcome.ok()) return outcome;

		var plan = outcome.plan();
		var center = PartyArea.center(plan.area().food());
		if (center == null) return new Outcome(null, Refusal.NO_FOOD, null);

		// the party is owned by the chunk holding the food centre, which need not be
		// the chunk holding the anchor table, so guests are told which chunk it is
		var partyChunk = new ChunkPos(center);
		var party = PartyAttachment.of(level, partyChunk);
		if (party == null || party.active()) return new Outcome(null, Refusal.PARTY_EXISTS, null);
		party.start(level.getChunkAt(center), level.getGameTime(), sp.getUUID(), anchor,
				plan.roster(), plan.appeal());
		PartyTicker.register(level, partyChunk);

		for (var type : plan.roster()) {
			var guest = spawn(level, sp.getUUID(), anchor, partyChunk, type);
			if (guest != null) party.addGuest(guest.getUUID());
		}

		level.playSound(null, anchor, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.4F);
		if (!sp.getAbilities().instabuild) letter.shrink(1);
		return outcome;
	}

	/**
	 * End a party: dismiss every guest on site and free the chunk for another send.
	 * Leftover food is left exactly where it is — an interrupted party costs the
	 * player nothing.
	 */
	public static void dismissAll(ServerLevel level, PartyAttachment party) {
		party.setPhase(PartyAttachment.Phase.CLOSING);
		for (var youkai : PartyTicker.guests(level, party)) {
			youkai.getModule(PartyModule.class).ifPresent(PartyModule::dismiss);
		}
		for (var id : new ArrayList<>(party.guests())) party.removeGuest(id);
		party.reset();
		var chunk = party.chunk();
		if (chunk != null) PartyTicker.unregister(level, chunk);
	}

	/**
	 * Put a guest on site.
	 * <p>
	 * Deliberately does not touch {@code HomeModule}: with no home binding she
	 * never gets a {@code HOME} memory, so she can neither sleep nor claim a bed,
	 * and the resident back home keeps living her ordinary day — the same trick
	 * {@code VisitScheduler} uses, for the same reason.
	 *
	 * @return the guest, or null if she could not be created
	 */
	@Nullable
	public static YoukaiEntity spawn(ServerLevel level, UUID host, BlockPos table, ChunkPos partyChunk,
			EntityType<?> type) {
		if (!(type.create(level) instanceof YoukaiEntity youkai)) return null;
		var module = youkai.getModule(PartyModule.class);
		if (module.isEmpty()) return null;
		var center = table.getCenter();
		youkai.setPos(center.x, center.y + 1, center.z);
		youkai.initSpellCard();
		youkai.restrictTo(table, GLModConfig.SERVER.partyGuestRadius.get());
		module.get().arrive(host, table, partyChunk);
		level.addFreshEntity(youkai);
		return youkai;
	}

}

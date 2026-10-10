package dev.xkmc.gensokyolegacy.content.attachment.party.core;

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
import java.util.Random;
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

		/** The spread's total attraction. Scales weights; is not the budget. */
		public int appeal() {
			return area.appeal();
		}

		/** How much the spread can actually feed — the budget the planner spent. */
		public int capacity() {
			return area.fulfillment();
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

	/**
	 * The full outcome, so the caller can message the player without re-deriving anything.
	 * <p>
	 * The scanned {@link #area} rides along even on a refusal, because that is the
	 * interesting half of a refusal: the debug wand wants to show what the planner
	 * could and could not see around that table, and re-scanning it would be a second
	 * walk over the same blocks.
	 */
	public record Outcome(@Nullable PartyArea area, @Nullable Plan plan, @Nullable Refusal refusal,
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
			ToIntFunction<EntityType<?>> reputation) {
		var area = PartyArea.scan(level, anchor, GLModConfig.SERVER.partyScanDistance.get());
		if (area.food().isEmpty()) return new Outcome(area, null, Refusal.NO_FOOD, null);
		// chairs are a hard requirement: no seats, no party
		if (area.seats().isEmpty()) return new Outcome(area, null, Refusal.NO_SEATS, null);
		var touched = PartyArea.touchedChunks(area.food());
		if (touched != null && PartyAttachment.existing(level, touched) != null)
			return new Outcome(area, null, Refusal.PARTY_EXISTS, null);

		var pool = new ArrayList<PartyPlanner.Candidate>();
		for (var type : BuiltInRegistries.ENTITY_TYPE) {
			// Two gates. Only youkai are candidates at all, so the chair entity, the
			// broom, the doll and the projectiles stay out; and a character with no
			// authored party_taste is not party-eligible, so she stays out too.
			if (!isCharacter(type)) continue;
			var taste = PartyTaste.of(level.registryAccess(), type);
			if (taste == null) continue;
			int rep = reputation.applyAsInt(type);
			pool.add(new PartyPlanner.Candidate(type,
					PartyMath.weight(taste, area.appeal(), rep), taste.fulfillment(), rep));
		}
		var roster = PartyPlanner.select(pool, area.fulfillment(), area.seats().size(),
				drawOf(PartyArea.center(area.food())));
		if (roster.isEmpty()) {
			return new Outcome(area, null, Refusal.NOBODY_CAME,
					PartyPlanner.nearestMiss(pool, area.fulfillment(), roster));
		}
		return new Outcome(area, new Plan(area, pool, roster), null, null);
	}

	/**
	 * The random the guest draw runs on, seeded from where the food is.
	 * <p>
	 * The seed is the <b>centre of the spread</b> and not the block the player clicked,
	 * so clicking either end of a long table draws the same party. That is what keeps
	 * {@code /gensokyo party preview} an exact prediction: preview, a real send and a
	 * wand simulation all seed from the same place, so they cannot disagree, while moving
	 * the party's food a block or two gives a different draw.
	 * <p>
	 * The coordinates are scrambled rather than packed straight into the seed. Adjacent
	 * centres must not produce adjacent draws, or neighbouring parties would come out
	 * alike — which is exactly what a plain {@code (x << 32) ^ (z << 16) ^ y} gives you,
	 * since neighbouring centres then differ only in the low bits.
	 */
	private static Random drawOf(BlockPos center) {
		long seed = (long) center.getX() * 0x9E3779B97F4A7C15L
				+ (long) center.getY() * 0xC2B2AE3D27D4EB4FL
				+ (long) center.getZ() * 0x165667B19E3779F9L;
		// splitmix64 finaliser, so a one-block move reshuffles the whole draw
		seed ^= seed >>> 33;
		seed *= 0xFF51AFD7ED558CCDL;
		seed ^= seed >>> 33;
		return new Random(seed);
	}

	/** The player's own reputation with a character type, for {@link #plan}. */
	public static ToIntFunction<EntityType<?>> reputationOf(ServerPlayer sp) {
		var chars = GLMeta.CHAR.type().getOrCreate(sp);
		return type -> chars.getUnbounded(sp, type).data().reputation;
	}

	/**
	 * Whether this entity type is a character at all, as opposed to furniture, a
	 * projectile or a summon. Necessary but <b>not sufficient</b> for party
	 * eligibility: a character also needs an authored {@code party_taste} entry, so
	 * this only answers the cheaper half of the question.
	 * <p>
	 * All fourteen characters are registered as {@link MobCategory#MONSTER}, which is
	 * what separates them from the chair, the broom, the doll and the projectile
	 * entities — all {@code MISC}. Structural rather than a hand-kept list, so
	 * {@code party_taste} is the only place eligibility is written down.
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
		if (!level.getBlockState(anchor).is(GLTagGen.PARTY_TABLE)) return new Outcome(null, null, Refusal.NO_FOOD, null);
		var outcome = plan(level, sp, anchor, reputationOf(sp));
		if (!outcome.ok()) return outcome;

		var plan = outcome.plan();
		var center = PartyArea.center(plan.area().food());
		if (center == null) return new Outcome(plan.area(), null, Refusal.NO_FOOD, null);

		// the party is owned by the chunk holding the food centre, which need not be
		// the chunk holding the anchor table, so guests are told which chunk it is
		var partyChunk = new ChunkPos(center);
		var party = PartyAttachment.of(level, partyChunk);
		if (party == null || party.active()) return new Outcome(plan.area(), null, Refusal.PARTY_EXISTS, null);
		party.start(level.getChunkAt(center), level.getGameTime(), sp.getUUID(), anchor,
				plan.roster(), plan.capacity(),
				PartyArea.foodAround(level, anchor), PartyArea.seatsAround(level, anchor));
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

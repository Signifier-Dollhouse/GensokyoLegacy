package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.entity.module.HomeModule;
import dev.xkmc.gensokyolegacy.content.entity.module.PartyModule;
import dev.xkmc.gensokyolegacy.content.entity.module.VisitModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * The one place a party is started, and the one place guests are spawned. Every rejection
 * happens before the letter is consumed, so a refused send costs the player nothing.
 *
 * @see doc/design/party/send.md
 */
public class PartySend {

	/** A whole send attempt, reusable by preview, which wants the plan without the effects. */
	public record Plan(PartyArea area, List<PartyPlanner.Candidate> pool, List<EntityType<?>> roster) {

		/** The spread's attraction, each food counted once. Scales weights; is not the budget. */
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
	 * {@link #area} rides along even on a refusal — that is the interesting half of one.
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
	 * @param reputation how a character is to be scored, supplied so preview and send
	 *                   cannot drift apart
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
	 * The random the guest draw runs on, seeded from the <b>centre of the spread</b> so
	 * either end of a long table draws the same party and preview cannot disagree with a
	 * send. select.md §4.2.
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
	 * Whether this type is a character rather than furniture or a projectile. Only the
	 * cheap half of eligibility: the other half is an authored {@code party_taste}.
	 */
	public static boolean isCharacter(EntityType<?> type) {
		return true;
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

		// guests are told which chunk owns the party: the food centre, not the anchor table
		var partyChunk = new ChunkPos(center);
		var party = PartyAttachment.of(level, partyChunk);
		if (party == null || party.active()) return new Outcome(plan.area(), null, Refusal.PARTY_EXISTS, null);
		// a send opens a party, it does not fill a table: the roster is frozen here and
		// admitted from the ticker, one a second
		party.start(level.getChunkAt(center), level.getGameTime(), sp.getUUID(), anchor,
				plan.roster(), plan.capacity(),
				PartyArea.foodAround(level, anchor), PartyArea.seatsAround(level, anchor));
		PartyTicker.register(level, partyChunk);

		level.playSound(null, anchor, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.4F);
		if (!sp.getAbilities().instabuild) letter.shrink(1);
		return outcome;
	}

	/**
	 * Admit the next character of the roster, one arrival a second. The entry is always
	 * dealt with, so a type that cannot be created costs one second rather than stalling
	 * admission for ever.
	 */
	public static void admitNext(ServerLevel level, PartyAttachment party) {
		// a closing party admits nobody: a guest arriving into one is told to leave
		// on her first tick
		if (party.phase() == PartyAttachment.Phase.CLOSING) return;
		var type = party.nextGuest();
		if (type == null) return;
		var anchor = party.anchor();
		if (anchor == null) {
			party.admit();
			return;
		}
		var host = party.host();
		var chunk = party.chunk();
		var existing = findOnSite(level, anchor, type);
		YoukaiEntity guest;
		if (existing != null) {
			guest = existing;
			join(level, guest, host, anchor, chunk);
		} else {
			guest = spawn(level, host, anchor, chunk, type);
		}
		if (guest != null) party.addGuest(guest.getUUID());
		party.admit();
	}

	/**
	 * A character of this type already on site who could come as herself — only a
	 * {@link VisitModule visitor} or a {@link HomeModule resident} standing at her own
	 * house qualifies: both have somewhere to go back to.
	 */
	@Nullable
	private static YoukaiEntity findOnSite(ServerLevel level, BlockPos anchor, EntityType<?> type) {
		var reach = GLModConfig.SERVER.partyScanDistance.get();
		var area = new AABB(anchor).inflate(reach);
		return level.getEntities(EntityTypeTest.forClass(YoukaiEntity.class), area,
						y -> y.getType() == type && !y.isPartying() && isOnSite(y, reach))
				.stream().findFirst().orElse(null);
	}

	/** Whether she is visiting, or a resident standing at her own home. */
	private static boolean isOnSite(YoukaiEntity e, int reach) {
		if (e.getModule(VisitModule.class).map(VisitModule::visiting).orElse(false)) return true;
		var home = e.getModule(HomeModule.class).map(HomeModule::home).orElse(null);
		if (home == null) return false;
		if (!e.level().isLoaded(home.pos())) return false;
		return e.distanceToSqr(Vec3.atCenterOf(home.pos())) <= (double) reach * reach;
	}

	/**
	 * Put an already-existing character on the party's books. As {@link #spawn}, but her
	 * {@code HomeModule} is left alone. Where she came from is what {@link
	 * PartyModule#join} records.
	 */
	private static void join(ServerLevel level, YoukaiEntity guest, @Nullable UUID host,
			BlockPos table, @Nullable ChunkPos partyChunk) {
		guest.restrictTo(table, GLModConfig.SERVER.partyGuestRadius.get());
		var visit = guest.getModule(VisitModule.class).map(VisitModule::host).orElse(null);
		var home = guest.getModule(HomeModule.class).map(HomeModule::home).orElse(null);
		guest.getModule(PartyModule.class).ifPresent(m -> m.join(host, table, partyChunk,
				visit != null ? PartyModule.Origin.VISITING
						: home != null ? PartyModule.Origin.HOME : PartyModule.Origin.SUMMONED,
				visit != null ? visit : home));
	}

	/**
	 * End a party: every guest is told to leave, staggered, and the party finishes when the
	 * last of them is gone. Deliberately not a {@code discard()} over the roster — a host
	 * calling {@code end} mid-banquet should watch a table wind down, not vanish.
	 */
	public static void close(ServerLevel level, PartyAttachment party) {
		party.close(level.getGameTime());
		for (var youkai : PartyTicker.guests(level, party)) {
			youkai.getModule(PartyModule.class).ifPresent(PartyModule::beginLeave);
		}
	}

	/**
	 * Put a guest on site. Deliberately does not touch {@code HomeModule}: with no home
	 * binding the resident back home keeps living her ordinary day — send.md §3.
	 *
	 * @return the guest, or null if she could not be created or nowhere was found
	 */
	@Nullable
	public static YoukaiEntity spawn(ServerLevel level, UUID host, BlockPos table, ChunkPos partyChunk,
			EntityType<?> type) {
		if (!(type.create(level) instanceof YoukaiEntity youkai)) return null;
		var module = youkai.getModule(PartyModule.class);
		if (module.isEmpty()) return null;
		var arrival = arrivalNear(level, table, youkai);
		if (arrival == null) return null;
		youkai.setPos(arrival);
		youkai.initSpellCard();
		youkai.restrictTo(table, GLModConfig.SERVER.partyGuestRadius.get());
		module.get().arrive(host, table, partyChunk);
		level.addFreshEntity(youkai);
		return youkai;
	}

	/**
	 * Somewhere a guest can appear: random, within the scan distance, on the ground if
	 * there is any, never inside the furniture. Deliberately not the guest radius — that is
	 * how far she may wander once she is <i>at</i> the party, not how far away she appears.
	 */
	@Nullable
	private static Vec3 arrivalNear(ServerLevel level, BlockPos anchor, YoukaiEntity youkai) {
		var reach = GLModConfig.SERVER.partyScanDistance.get();
		var random = youkai.getRandom();
		var pos = RandomPos.generateRandomPos(youkai, () -> {
			var spot = groundNear(level, new BlockPos(
					anchor.getX() + random.nextInt(-reach, reach + 1),
					anchor.getY(),
					anchor.getZ() + random.nextInt(-reach, reach + 1)));
			if (spot == null || !youkai.getNavigation().isStableDestination(spot)) return null;
			return LandRandomPos.movePosUpOutOfSolid(youkai, spot);
		});
		if (pos != null && !furniture(level, pos)) return pos;
		// ten random spots found nothing; stand her somewhere definite rather than not at all
		return freeSpaceNear(level, anchor, 4);
	}

	/**
	 * The walk target for both a chair and a dish, neither of which is walked to directly:
	 * a chair is furniture to sit down from <i>beside</i>, and the block beside a dish on
	 * a table is air at table height. Two rings in order — the block's own neighbours, then
	 * their neighbours one support down.
	 * <p>
	 * Not checked for reachability: that is the pathfinder's question.
	 *
	 * @return the block to walk to, or null when there is nowhere to stand near it
	 */
	@Nullable
	public static BlockPos standingNear(ServerLevel level, BlockPos pos) {
		for (var face : SIDES) {
			var beside = pos.relative(face);
			if (isStandable(level, beside)) return beside;
		}
		// on a table, or on a chair: the floor beside whatever is holding it up
		var support = PartyArea.supportOf(level, pos, SUPPORT_DROP);
		if (support == null) return null;
		for (var face : SIDES) {
			var beside = support.relative(face);
			if (isStandable(level, beside)) return beside;
		}
		return null;
	}

	/** The four horizontal directions: a chair and a dish are both reached from the side. */
	private static final Direction[] SIDES = {
			Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

	/** How far down to look for whatever a dish is sitting on. */
	private static final int SUPPORT_DROP = 4;

	private static boolean isStandable(ServerLevel level, BlockPos pos) {
		return level.isLoaded(pos) && standable(level, pos) && !furniture(level, pos.getCenter());
	}

	/**
	 * A free, standable block within {@code radius} rings of {@code near}, or null. Rings
	 * outward rather than a box, so the closest free space wins — the caller wants beside
	 * the chair, not as far from it as the search reached.
	 */
	@Nullable
	public static Vec3 freeSpaceNear(ServerLevel level, BlockPos near, int radius) {
		for (int r = 0; r <= radius; r++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					for (int dy = 2; dy >= -2; dy--) {
						var pos = near.offset(dx, dy, dz);
						if (!level.isLoaded(pos) || !standable(level, pos)) continue;
						if (furniture(level, pos.getCenter())) continue;
						return pos.getCenter();
					}
				}
			}
		}
		return null;
	}

	/**
	 * The first spot around {@code pos} with air to stand in and something solid under it,
	 * looking up then down. Bounded, or a tall empty shaft would run to the bottom of the
	 * world.
	 */
	@Nullable
	private static BlockPos groundNear(ServerLevel level, BlockPos pos) {
		for (int dy = 0; dy <= 8; dy++) {
			var up = pos.offset(0, dy, 0);
			if (level.isLoaded(up) && standable(level, up)) return up;
			var down = pos.offset(0, -dy, 0);
			if (level.isLoaded(down) && standable(level, down)) return down;
		}
		return null;
	}

	private static boolean standable(ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return false;
		var below = pos.below();
		return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
	}

	/**
	 * Whether this spot is party furniture. Asked separately of {@link #standable} because
	 * it is the rule, not a consequence.
	 */
	private static boolean furniture(ServerLevel level, Vec3 pos) {
		var at = BlockPos.containing(pos);
		var state = level.getBlockState(at);
		return state.is(GLTagGen.PARTY_SEAT) || state.is(GLTagGen.PARTY_TABLE);
	}

}

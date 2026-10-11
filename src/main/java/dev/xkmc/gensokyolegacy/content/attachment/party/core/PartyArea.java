package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;

/**
 * Everything one party occupies, found by walking out from the table the player
 * clicked.
 * <p>
 * The scan is the unit of "a party": food, seats, tables, the anchor and therefore
 * the guests are all inside it. It runs once per send and once per preview, never
 * per tick.
 *
 * @param anchor the table block the player clicked
 * @param food   every party-food block laid out for the spread, in walk order
 * @param seats  every seat block the walk reached
 * @param tables every table block the walk reached
 * @param appeal attraction of {@link #food} counting each food once; scales
 *               {@link PartyMath#weight} and is otherwise unused — it is not what the
 *               spread can feed, and it is not a count of servings
 * @param fulfillment total hunger in {@link #food}; this is the capacity the planner
 *                    budgets and the number a guest's own fulfillment is measured against.
 *                    Unlike {@link #appeal} this <b>does</b> scale with what is left on
 *                    the table, because a second cake really does feed seven more people
 */
public record PartyArea(BlockPos anchor, List<BlockPos> food, List<BlockPos> seats,
		List<BlockPos> tables, int appeal, int fulfillment) {

	/**
	 * How far the walk reaches around each chair or table it finds: a 5 x 3 x 5 box,
	 * centred on the block. Two out sideways and one up and down is about as far as
	 * a dish can be from a seat and still read as put out for that seat.
	 */
	private static final int POKE_X = 2;
	private static final int POKE_Y = 1;
	private static final int POKE_Z = 2;

	/** The six faces, for the walk itself. */
	private static final Direction[] FACES = Direction.values();

	/**
	 * The five faces a dish may touch a seat or a table on — every face but the
	 * underside, which is the separate "standing on a table" case.
	 */
	private static final Direction[] TOUCHING = {
			Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

	/**
	 * A ceiling on how many blocks one scan may look at. The distance limit on its
	 * own allows the better part of a million blocks of open air — a stall the player
	 * cannot see or stop — so the walk stops here instead. It only ever bites in a
	 * large empty space, where there is by then nothing to find.
	 */
	private static final int MAX_VISITED = 1 << 16;

	/**
	 * Walks out from the anchor and collects the furniture and the food of one party.
	 * <p>
	 * The walk passes only through space a guest could stand in, so a party is the
	 * room it was set in and not the building around it. A wall ends it; a doorway,
	 * and anything else with no collision, does not.
	 * <p>
	 * A chair or a table is solid, so the walk stops when it meets one. That is fine,
	 * because finding furniture is the walk's purpose: it cannot go <em>through</em>
	 * a chair, only reach it. What it would otherwise miss is the space just past it
	 * — the run of chairs along a long table, the far end of the spread — and that is
	 * what the box below is for. Every chair and table found queues a
	 * {@value #POKE_X} x {@value #POKE_Y} x {@value #POKE_Z} box around itself, whose
	 * contents are then read and walked through in the ordinary way. The walk resumes
	 * from the air on the guest side of the chair and carries on, so a whole row of
	 * seating carries the walk along its length without the rule being applied twice.
	 * <p>
	 * It also reaches furniture the walk could never have stood near: a chair pushed
	 * into a nook behind a pillar, a dish tight against a wall. The box is read
	 * regardless of what surrounds it.
	 * <p>
	 * Nothing beyond {@code distance} from the anchor counts, however it was reached,
	 * and nothing in an unloaded chunk is read at all: the scan runs inside one
	 * click, so it must not drag a chunk in to answer a question about a party.
	 * <p>
	 * A dish counts when it stands on a party table or touches a seat or a table from
	 * any face but below. That is the whole of "food laid out for the party": a cake
	 * on the floor, or a dropped plate two blocks from the nearest chair, is neither
	 * counted towards the spread nor ever eaten.
	 * <p>
	 * A block that is tagged as party food but is not <i>valid</i> food — no hunger
	 * value, nothing to eat, something refused outright — is dropped here as well. It
	 * is left standing in the world, but it is neither counted towards the spread nor
	 * ever walked to, so a datapack entry that turns out to be a dud costs the player
	 * nothing and costs a guest no time.
	 */
	public static PartyArea scan(ServerLevel level, BlockPos anchor, int distance) {
		var access = level.registryAccess();
		var food = new ArrayList<BlockPos>();
		var seats = new ArrayList<BlockPos>();
		var tables = new ArrayList<BlockPos>();
		int appeal = 0;
		int fulfillment = 0;
		long reach = (long) distance * distance;
		var seen = new HashSet<BlockPos>();
		// The foods already paid for. This is the dedupe the appeal rule needs and the
		// only place it can live: the scan is the one walk over the whole spread, so it
		// is the one place that has seen all of it. Everything downstream gets a number.
		var counted = new HashSet<ResourceLocation>();
		var queue = new ArrayDeque<BlockPos>();
		queue.add(anchor.immutable());
		while (!queue.isEmpty() && seen.size() < MAX_VISITED) {
			var pos = queue.poll();
			if (!seen.add(pos)) continue;
			if (!within(anchor, pos, reach) || !level.isLoaded(pos)) continue;
			var state = level.getBlockState(pos);
			var furniture = false;
			if (state.is(GLTagGen.PARTY_SEAT)) {
				seats.add(pos);
				furniture = true;
			}
			if (state.is(GLTagGen.PARTY_TABLE)) {
				tables.add(pos);
				furniture = true;
			}
			// A chair or table is solid, so the walk stops at one. Its box is what
			// carries it past: the guest-side air, the dishes on top, and the floor
			// beyond are all inside, and the walk resumes from those.
			if (furniture) poke(queue, pos);
			if (PartyFoodValue.isFood(level, pos, state) && isLaidOut(level, pos, state)) {
				// what it is worth to the party, by food - never by block, so the set
				// decides what is new since the last block and the rest is dropped
				var worth = PartyFoodValue.appealPerFood(access, level, pos, state);
				if (!worth.isEmpty()) {
					// Into the spread whatever the dedupe says. A second cake adds no
					// appeal but still feeds seven more guests, and dropping it here
					// would take it out of the budget and off the guest's walk.
					food.add(pos);
					for (var entry : worth) {
						if (counted.add(entry.id())) appeal += entry.appeal();
					}
					// gathered alongside the appeal, but it is the one the planner
					// budgets: see PartyFoodValue's two-quantities note
					fulfillment += PartyFoodValue.totalHunger(level, pos, state);
				}
			}
			// everything else the walk can pass through: dishes are solid, so the
			// walk only ever meets one by reading it off a seat's or table's box
			if (!furniture && state.getCollisionShape(level, pos).isEmpty()) {
				for (var face : FACES) queue.add(pos.relative(face));
			}
		}
		return new PartyArea(anchor.immutable(), food, seats, tables, appeal, fulfillment);
	}

	/**
	 * Whether a position is within the scan distance of the anchor.
	 */
	private static boolean within(BlockPos anchor, BlockPos pos, long reach) {
		long dx = pos.getX() - anchor.getX();
		long dy = pos.getY() - anchor.getY();
		long dz = pos.getZ() - anchor.getZ();
		return dx * dx + dy * dy + dz * dz <= reach;
	}

	/**
	 * How far around the party table a guest looks for a chair and for a dish.
	 *
	 * <p>
	 * A cube rather than a sphere, because the party is a room and the number is there to
	 * be a cheap bound on a box sweep — a box is a triple loop, a sphere needs a square
	 * root per candidate.
	 *
	 * <p>
	 * Much smaller than the scan distance, and on purpose. The scan walks the whole room
	 * once, for the party; this is swept per guest and refreshed on a timer, so the cost
	 * of widening it is paid over and over for food no guest would walk that far to reach
	 * anyway.
	 */
	public static final int GUEST_RADIUS = 6;

	/**
	 * Every dish of the spread within {@link #GUEST_RADIUS} of the anchor, by the same
	 * rules the scan uses.
	 *
	 * <p>
	 * The box form of {@link #scan}, and the one the party re-runs while it is open. A
	 * scan is a BFS over the whole room for one answer; this is a triple loop for an
	 * answer that has to be current, and it deliberately does not try to walk through
	 * walls — a guest eats what she can see from the table, and a party in a walled room
	 * two rooms away is not hers.
	 */
	public static List<BlockPos> foodAround(Level level, BlockPos anchor) {
		var access = level.registryAccess();
		var ans = new ArrayList<BlockPos>();
		for (var pos : box(anchor)) {
			var state = level.getBlockState(pos);
			if (!PartyFoodValue.isFood(level, pos, state)) continue;
			if (!isLaidOut(level, pos, state)) continue;
			// per block, not per party: this list is positions and nothing else, and a
			// duplicate of a food the party already has is still a dish a guest may walk
			// to and eat. The appeal rule dedupes in the scan, which is the only walk
			// that has seen the whole spread.
			if (!PartyFoodValue.attracts(access, level, pos, state)) continue;
			ans.add(pos.immutable());
		}
		return ans;
	}

	/**
	 * Every chair within {@link #GUEST_RADIUS} of the anchor.
	 *
	 * <p>
	 * Not filtered by layout, because a chair is a chair: whether it can be reached
	 * through a wall is the walk's problem and not the list's. A guest that cannot get to
	 * one gives up looking after ten seconds and leaves — see {@code YoukaiPartyTask}.
	 */
	public static List<BlockPos> seatsAround(Level level, BlockPos anchor) {
		var ans = new ArrayList<BlockPos>();
		for (var pos : box(anchor)) {
			if (level.getBlockState(pos).is(GLTagGen.PARTY_SEAT)) ans.add(pos.immutable());
		}
		return ans;
	}

	/** Every block of the guest box around the anchor, inclusive. */
	private static Iterable<BlockPos> box(BlockPos anchor) {
		return BlockPos.betweenClosed(
				anchor.offset(-GUEST_RADIUS, -GUEST_RADIUS, -GUEST_RADIUS),
				anchor.offset(GUEST_RADIUS, GUEST_RADIUS, GUEST_RADIUS));
	}

	/** Queue the box around one piece of furniture, for the walk to read from. */
	private static void poke(Deque<BlockPos> queue, BlockPos from) {
		for (int dx = -POKE_X; dx <= POKE_X; dx++) {
			for (int dy = -POKE_Y; dy <= POKE_Y; dy++) {
				for (int dz = -POKE_Z; dz <= POKE_Z; dz++) {
					queue.add(new BlockPos(from.getX() + dx, from.getY() + dy, from.getZ() + dz));
				}
			}
		}
	}

	/**
	 * Whether a dish was put out for the party: standing on a table or a chair, or
	 * touching a seat or a table. The one test, shared with {@code YoukaiPartyTask}, so a
	 * guest can only eat a dish the scan would have counted — otherwise a guest could be
	 * handed something off the floor that never counted towards the spread she is fed by.
	 * <p>
	 * A chair counts as a surface as well as a seat, so a plate set on a guest's own seat
	 * is hers to eat. That is not a special case bolted on for the eating rule: the same
	 * test decides what the scan collects, so a dish on a chair is part of the spread and
	 * is budgeted for like any other, rather than being edible but unaccounted.
	 * <p>
	 * This is the test for food that describes itself by its block state. Container food
	 * — a cutting board, a steamer rack — is judged by its own handler instead, because
	 * where those count from differs by block; see {@link #isLaidOut}.
	 */
	public static boolean touchingFurniture(Level level, BlockPos pos) {
		var under = level.getBlockState(pos.below());
		if (under.is(GLTagGen.PARTY_TABLE) || under.is(GLTagGen.PARTY_SEAT)) return true;
		for (var face : TOUCHING) {
			var touching = level.getBlockState(pos.relative(face));
			if (touching.is(GLTagGen.PARTY_TABLE) || touching.is(GLTagGen.PARTY_SEAT)) return true;
		}
		return false;
	}

	/**
	 * Whether a dish was put out for the party, whichever kind of food it is.
	 * <p>
	 * Dispatches to the container's own rule where there is one, and to
	 * {@link #touchingFurniture} otherwise. The two are not the same question: a cutting
	 * board on a table is the spread, a steamer pot standing on the ground is not — but
	 * the rack inside that pot is, because the pot is what carries it. That is a fact
	 * about steamers rather than about parties, so it belongs to the handler and not
	 * here.
	 */
	public static boolean isLaidOut(Level level, BlockPos pos, BlockState state) {
		var container = PartyFoodHandler.containerOf(state.getBlock());
		return container != null
				? container.isPutOut(level, pos, state)
				: touchingFurniture(level, pos);
	}

	/**
	 * The first solid block under {@code pos}, found by falling through anything with no
	 * collision shape.
	 * <p>
	 * For finding what a thing is standing <i>in</i>: the rack of a steamer is inside
	 * the pot, not on it, so the block directly beneath it is pot interior rather than
	 * the pot itself. Bounded, because an unbounded fall through a tall empty shaft would
	 * otherwise run to the bottom of the world.
	 *
	 * @return that block, or null if nothing solid is within {@code maxDrop}
	 */
	@Nullable
	public static BlockPos supportOf(Level level, BlockPos pos, int maxDrop) {
		for (int d = 1; d <= maxDrop; d++) {
			var below = pos.below(d);
			if (!level.isLoaded(below)) return null;
			var state = level.getBlockState(below);
			if (state.getCollisionShape(level, below).isEmpty()) continue;
			return below.immutable();
		}
		return null;
	}

	/**
	 * The mean of the food positions. The chunk containing this owns the party.
	 * <p>
	 * The mean rather than the midpoint of the bounding box, so a spread of one
	 * cake and one distant pie centres nearer the cake.
	 */
	@Nullable
	public static BlockPos center(List<BlockPos> food) {
		if (food.isEmpty()) return null;
		long x = 0, y = 0, z = 0;
		for (var p : food) {
			x += p.getX();
			y += p.getY();
			z += p.getZ();
		}
		int n = food.size();
		// floorDiv, not /, so a negative-coordinate spread does not bias the centre
		return new BlockPos((int) Math.floorDiv(x, n), (int) Math.floorDiv(y, n), (int) Math.floorDiv(z, n));
	}

	/** Every chunk the food touches. A party may not overlap another party's. */
	@Nullable
	public static ChunkPosRange touchedChunks(List<BlockPos> food) {
		if (food.isEmpty()) return null;
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		for (var p : food) {
			minX = Math.min(minX, p.getX());
			maxX = Math.max(maxX, p.getX());
			minY = Math.min(minY, p.getY());
			maxY = Math.max(maxY, p.getY());
			minZ = Math.min(minZ, p.getZ());
			maxZ = Math.max(maxZ, p.getZ());
		}
		return ChunkPosRange.ofBlocks(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
	}

}
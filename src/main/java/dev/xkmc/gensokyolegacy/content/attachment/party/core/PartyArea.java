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
 * Everything one party occupies, found by walking out from the clicked table. The scan
 * is the unit of "a party" and runs once per send, never per tick.
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
 * @see doc/design/party/area.md
 */
public record PartyArea(BlockPos anchor, List<BlockPos> food, List<BlockPos> seats,
		List<BlockPos> tables, int appeal, int fulfillment) {

	/** How far the walk reaches around each chair or table: a 5 x 3 x 5 box, centred. */
	private static final int POKE_X = 2;
	private static final int POKE_Y = 1;
	private static final int POKE_Z = 2;

	/** The six faces, for the walk itself. */
	private static final Direction[] FACES = Direction.values();

	/** Every face but the underside, which is the separate "standing on a table" case. */
	private static final Direction[] TOUCHING = {
			Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP};

	/** A safety valve: the distance limit alone allows the better part of a million blocks of open air. */
	private static final int MAX_VISITED = 1 << 16;

	/**
	 * Walks out from the anchor and collects the furniture and the food of one party.
	 *
	 * <p>
	 * The walk passes only through space a guest could stand in, so a party is the room
	 * it was set in: a wall ends it, a doorway does not. Furniture is solid, so the walk
	 * only ever reaches one — which is why each queues a box around itself, letting the
	 * walk resume from the air on the guest's side and carry on along a long table. That
	 * box also reaches what the walk could never have stood near, a chair in a nook or a
	 * dish tight against a wall.
	 *
	 * <p>
	 * Nothing beyond {@code distance} counts, and an unloaded chunk is never read: the
	 * scan runs inside one click and must not drag a chunk in.
	 *
	 * <p>
	 * A dish counts when it stands on a table or touches one from any face but below. A
	 * block that is recognised but not <i>valid</i> food is dropped too — left standing in
	 * the world, but never counted and never walked to, so a dud costs nobody anything.
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

	/** Whether a position is within the scan distance of the anchor. */
	private static boolean within(BlockPos anchor, BlockPos pos, long reach) {
		long dx = pos.getX() - anchor.getX();
		long dy = pos.getY() - anchor.getY();
		long dz = pos.getZ() - anchor.getZ();
		return dx * dx + dy * dy + dz * dz <= reach;
	}

	/**
	 * How far around the table a guest looks. A cheap cube for a triple loop, and much
	 * smaller than the scan distance: the scan walks the room once for the party, this is
	 * swept per guest on a timer.
	 */
	public static final int GUEST_RADIUS = 6;

	/**
	 * Every dish within {@link #GUEST_RADIUS} of the anchor — the box form of
	 * {@link #scan}, and the one the party re-runs while it is open so its answer stays
	 * current. Deliberately does not walk through walls: a guest eats what she can see
	 * from the table.
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

	/** Every chair within {@link #GUEST_RADIUS}, unfiltered: reachability is the walk's problem, not the list's. */
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
	 * touching a seat or a table. Shared with {@code YoukaiPartyTask} so a guest can only
	 * eat a dish the scan would have counted. A chair counts as a surface too, so a plate
	 * on a guest's own seat is budgeted rather than merely edible.
	 * <p>
	 * For state-described food only; containers judge their own placement — see
	 * {@link #isLaidOut}.
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
	 * Dispatches to the container's own rule where there is one, and to
	 * {@link #touchingFurniture} otherwise: a board on a table is the spread, a steamer pot
	 * on the ground is not, but the rack inside it is. That is a fact about steamers
	 * rather than about parties, so the handler owns it.
	 */
	public static boolean isLaidOut(Level level, BlockPos pos, BlockState state) {
		var container = PartyFoodHandler.containerOf(state.getBlock());
		return container != null
				? container.isPutOut(level, pos, state)
				: touchingFurniture(level, pos);
	}

	/**
	 * The first solid block under {@code pos}, falling through anything with no collision
	 * shape — for what a thing is standing <i>in</i>, since a steamer's rack is inside the
	 * pot. Bounded, so an empty shaft does not run to the bottom of the world.
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

	/** The mean of the food positions — nearer the cake, where a far pie barely moves it. The chunk containing this owns the party. */
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
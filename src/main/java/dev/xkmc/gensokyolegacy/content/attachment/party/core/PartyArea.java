package dev.xkmc.gensokyolegacy.content.attachment.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
 * @param appeal total appeal of {@link #food}
 */
public record PartyArea(BlockPos anchor, List<BlockPos> food, List<BlockPos> seats,
		List<BlockPos> tables, int appeal) {

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
		long reach = (long) distance * distance;
		var seen = new HashSet<BlockPos>();
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
			if (PartyFoodValue.isFood(access, state) && isLaidOut(level, pos)) {
				int worth = PartyFoodValue.appeal(access, state);
				if (worth > 0 && PartyFoodValue.result(state).valid()) {   // claimed, but not food
					food.add(pos);
					appeal += worth;
				}
			}
			// everything else the walk can pass through: dishes are solid, so the
			// walk only ever meets one by reading it off a seat's or table's box
			if (!furniture && state.getCollisionShape(level, pos).isEmpty()) {
				for (var face : FACES) queue.add(pos.relative(face));
			}
		}
		return new PartyArea(anchor.immutable(), food, seats, tables, appeal);
	}

	/** Whether a position is within the scan distance of the anchor. */
	private static boolean within(BlockPos anchor, BlockPos pos, long reach) {
		long dx = pos.getX() - anchor.getX();
		long dy = pos.getY() - anchor.getY();
		long dz = pos.getZ() - anchor.getZ();
		return dx * dx + dy * dy + dz * dz <= reach;
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
	 * Whether a dish was put out for the party: on a table, or touching a seat or a
	 * table. The one test, shared with {@code YoukaiPartyTask}, so a guest can only
	 * eat a dish the scan would have counted — otherwise a guest could be handed
	 * something off the floor that never counted towards the spread she is fed by.
	 */
	public static boolean isLaidOut(ServerLevel level, BlockPos pos) {
		if (level.getBlockState(pos.below()).is(GLTagGen.PARTY_TABLE)) return true;
		for (var face : TOUCHING) {
			var touching = level.getBlockState(pos.relative(face));
			if (touching.is(GLTagGen.PARTY_TABLE) || touching.is(GLTagGen.PARTY_SEAT)) return true;
		}
		return false;
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
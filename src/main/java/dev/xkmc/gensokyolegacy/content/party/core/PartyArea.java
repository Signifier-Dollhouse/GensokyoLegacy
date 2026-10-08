package dev.xkmc.gensokyolegacy.content.party.core;

import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.init.data.GLTagGen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything one party occupies, found in a single pass over a box around the
 * table the player clicked.
 * <p>
 * The scan is the unit of "a party": food, seats, the anchor and therefore the
 * guests are all inside it. It runs once per send and once per preview, never
 * per tick.
 *
 * @param anchor the table block the player clicked
 * @param food   every party-food block standing on a party table, in scan order
 * @param seats  every seat block in the box
 * @param appeal total appeal of {@link #food}
 */
public record PartyArea(BlockPos anchor, List<BlockPos> food, List<BlockPos> seats, int appeal) {

	/**
	 * Collects food, seats and appeal in one pass over the box around the anchor.
	 * <p>
	 * A food block only counts when the block directly below it is a party table.
	 * That single check is the whole of "block food placed on a table": a cake on
	 * the floor is neither counted nor ever eaten.
	 * <p>
	 * A block that is tagged as party food but is not <i>valid</i> food — no hunger
	 * value, nothing to eat, something refused outright — is dropped here as well. It
	 * is left standing in the world, but it is neither counted towards the spread nor
	 * ever walked to, so a datapack entry that turns out to be a dud costs the player
	 * nothing and costs a guest no time.
	 */
	public static PartyArea scan(ServerLevel level, BlockPos anchor, int radius) {
		var food = new ArrayList<BlockPos>();
		var seats = new ArrayList<BlockPos>();
		int appeal = 0;
		for (var pos : BlockPos.betweenClosed(anchor, anchor.offset(radius, radius, radius))) {
			var here = level.getBlockState(pos);
			if (here.is(GLTagGen.PARTY_SEAT)) seats.add(pos.immutable());
			if (!here.is(GLTagGen.PARTY_FOOD)) continue;
			if (!level.getBlockState(pos.below()).is(GLTagGen.PARTY_TABLE)) continue;
			int worth = PartyFoodValue.appeal(level.registryAccess(), here);
			if (worth <= 0 || !PartyFoodValue.result(here).valid()) continue;   // tagged, but not food
			food.add(pos.immutable());
			appeal += worth;
		}
		return new PartyArea(anchor.immutable(), food, seats, appeal);
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

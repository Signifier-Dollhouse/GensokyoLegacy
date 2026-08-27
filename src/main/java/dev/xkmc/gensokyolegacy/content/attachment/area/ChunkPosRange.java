package dev.xkmc.gensokyolegacy.content.attachment.area;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.stream.Stream;

public record ChunkPosRange(int minCX, int minCZ, int maxCX, int maxCZ) {

	public static ChunkPosRange ofChunks(ChunkPos min, ChunkPos max) {
		return new ChunkPosRange(min.x, min.z, max.x, max.z);
	}

	public static ChunkPosRange ofRadius(ChunkPos center, int radius) {
		return new ChunkPosRange(center.x - radius, center.z - radius, center.x + radius, center.z + radius);
	}

	public static ChunkPosRange ofOwner(BlockPos ownerPos, int radiusChunks) {
		ChunkPos center = new ChunkPos(ownerPos);
		return ofRadius(center, radiusChunks);
	}

	public static ChunkPosRange ofBlocks(BlockPos min, BlockPos max) {
		int minCX = min.getX() >> 4;
		int minCZ = min.getZ() >> 4;
		int maxCX = max.getX() >> 4;
		int maxCZ = max.getZ() >> 4;
		return new ChunkPosRange(Math.min(minCX, maxCX), Math.min(minCZ, maxCZ), Math.max(minCX, maxCX), Math.max(minCZ, maxCZ));
	}

	public static ChunkPosRange ofBoundingBox(BoundingBox box) {
		int minCX = box.minX() >> 4;
		int minCZ = box.minZ() >> 4;
		int maxCX = box.maxX() >> 4;
		int maxCZ = box.maxZ() >> 4;
		return new ChunkPosRange(Math.min(minCX, maxCX), Math.min(minCZ, maxCZ), Math.max(minCX, maxCX), Math.max(minCZ, maxCZ));
	}

	public boolean contains(ChunkPos pos) {
		return pos.x >= minCX && pos.x <= maxCX && pos.z >= minCZ && pos.z <= maxCZ;
	}

	public boolean contains(BlockPos pos) {
		return contains(new ChunkPos(pos));
	}

	public Stream<ChunkPos> stream() {
		return Stream.iterate(new ChunkPos(minCX, minCZ), p -> p != null,
						p -> {
							int nx = p.x + 1;
							int nz = p.z;
							if (nx > maxCX) {
								nx = minCX;
								nz++;
							}
							if (nz > maxCZ) return null;
							return new ChunkPos(nx, nz);
						})
				.filter(p -> p != null);
	}

	public void forEach(java.util.function.Consumer<ChunkPos> consumer) {
		for (int x = minCX; x <= maxCX; x++) {
			for (int z = minCZ; z <= maxCZ; z++) {
				consumer.accept(new ChunkPos(x, z));
			}
		}
	}

	public int chunkCount() {
		return (maxCX - minCX + 1) * (maxCZ - minCZ + 1);
	}
}

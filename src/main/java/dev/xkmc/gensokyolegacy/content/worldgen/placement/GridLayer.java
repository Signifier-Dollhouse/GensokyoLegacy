package dev.xkmc.gensokyolegacy.content.worldgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import javax.annotation.Nullable;

/**
 * A jittered grid: the world is cut into {@code cell} x {@code cell} squares, each hosting at most
 * one point, kept {@code margin} blocks away from the cell border. Points are a pure function of
 * world seed, salt and cell, so every chunk (and every other layer) recomputes the same points
 * without reading the world: results are independent of chunk generation order, two points of one
 * layer are never closer than {@code 2 * margin}, and layers can exclude each other.
 * <p>
 * The activation roll is drawn first and compared as {@code u < chance}: layers sharing cell,
 * margin and salt but differing in chance yield nested point sets, so one grid can serve several
 * densities (e.g. per forest type).
 */
public record GridLayer(int cell, int margin, int salt, float chance) {

	public static final MapCodec<GridLayer> CODEC = RecordCodecBuilder.<GridLayer>mapCodec(i -> i.group(
			Codec.intRange(4, 512).fieldOf("cell").forGetter(GridLayer::cell),
			Codec.intRange(0, 255).fieldOf("margin").forGetter(GridLayer::margin),
			Codec.INT.fieldOf("salt").forGetter(GridLayer::salt),
			Codec.floatRange(0, 1).fieldOf("chance").forGetter(GridLayer::chance)
	).apply(i, GridLayer::new)).validate(e -> e.margin * 2 < e.cell ? DataResult.success(e) :
			DataResult.error(() -> "margin must be less than half of cell"));

	/**
	 * The point of a cell at y = 0, or null when the cell is inactive.
	 */
	@Nullable
	public BlockPos point(long seed, int cx, int cz) {
		long lo = RandomSupport.mixStafford13(seed ^ salt * 0x9E3779B97F4A7C15L);
		long hi = RandomSupport.mixStafford13(cx * 0x632BE59BD9B4E019L + cz * 0x2545F4914F6CDD1DL);
		RandomSource random = new XoroshiroRandomSource(lo ^ hi, hi);
		float u = random.nextFloat();
		int x = cx * cell + margin + random.nextInt(cell - 2 * margin);
		int z = cz * cell + margin + random.nextInt(cell - 2 * margin);
		return u < chance ? new BlockPos(x, 0, z) : null;
	}

	/**
	 * True when an active point of this layer lies within {@code distance} (XZ) of the position.
	 */
	public boolean isNear(long seed, BlockPos pos, int distance) {
		long limit = (long) distance * distance;
		for (int cx = Math.floorDiv(pos.getX() - distance, cell); cx <= Math.floorDiv(pos.getX() + distance, cell); cx++) {
			for (int cz = Math.floorDiv(pos.getZ() - distance, cell); cz <= Math.floorDiv(pos.getZ() + distance, cell); cz++) {
				BlockPos point = point(seed, cx, cz);
				if (point == null) continue;
				long dx = point.getX() - pos.getX(), dz = point.getZ() - pos.getZ();
				if (dx * dx + dz * dz < limit) return true;
			}
		}
		return false;
	}

}

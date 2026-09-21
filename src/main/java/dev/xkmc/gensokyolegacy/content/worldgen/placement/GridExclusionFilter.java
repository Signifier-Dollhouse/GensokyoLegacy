package dev.xkmc.gensokyolegacy.content.worldgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.List;

/**
 * Rejects positions too close to the points of other {@link GridLayer}s, e.g. large trees keep
 * away from giant trees. Recomputes the points instead of reading the world. A point counts even
 * when its feature failed to place, which leaves a small natural gap.
 */
public class GridExclusionFilter extends PlacementFilter {

	public record Entry(GridLayer layer, int distance) {

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				GridLayer.CODEC.forGetter(Entry::layer),
				Codec.intRange(1, 64).fieldOf("distance").forGetter(Entry::distance)
		).apply(i, Entry::new));

	}

	public static final MapCodec<GridExclusionFilter> CODEC = Entry.CODEC.listOf().fieldOf("layers")
			.xmap(GridExclusionFilter::new, e -> e.layers);

	private final List<Entry> layers;

	public GridExclusionFilter(List<Entry> layers) {
		this.layers = layers;
	}

	@Override
	protected boolean shouldPlace(PlacementContext ctx, RandomSource random, BlockPos pos) {
		long seed = ctx.getLevel().getSeed();
		for (Entry e : layers) {
			if (e.layer().isNear(seed, pos, e.distance())) return false;
		}
		return true;
	}

	@Override
	public PlacementModifierType<?> type() {
		return GLWorldGen.GRID_EXCLUSION.get();
	}

}

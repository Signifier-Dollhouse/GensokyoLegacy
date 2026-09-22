package dev.xkmc.gensokyolegacy.content.worldgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Tells shaded forest floor from open ground by comparing two live heightmaps: under a canopy
 * WORLD_SURFACE sits on the leaves while MOTION_BLOCKING_NO_LEAVES sees through them. Both are
 * kept up to date during the features step, so this works right after the trees were placed.
 */
public class CanopyFilter extends PlacementFilter {

	public static final MapCodec<CanopyFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(1, 64).fieldOf("min_gap").forGetter(e -> e.minGap),
			Codec.BOOL.fieldOf("under").forGetter(e -> e.under)
	).apply(i, CanopyFilter::new));

	private final int minGap;
	private final boolean under;

	/**
	 * @param minGap canopy height above the floor that counts as shade
	 * @param under  true: keep shaded positions, false: keep open positions
	 */
	public CanopyFilter(int minGap, boolean under) {
		this.minGap = minGap;
		this.under = under;
	}

	@Override
	protected boolean shouldPlace(PlacementContext ctx, RandomSource random, BlockPos pos) {
		int top = ctx.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
		int floor = ctx.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
		boolean shaded = top - floor >= minGap;
		return shaded == under;
	}

	@Override
	public PlacementModifierType<?> type() {
		return GLWorldGen.CANOPY.get();
	}

}

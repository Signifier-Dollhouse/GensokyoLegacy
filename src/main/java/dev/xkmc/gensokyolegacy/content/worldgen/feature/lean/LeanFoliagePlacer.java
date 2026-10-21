package dev.xkmc.gensokyolegacy.content.worldgen.feature.lean;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/**
 * A crown that follows the trunk it grows on.
 * <p>
 * The spruce crown is one cone of layers hanging from a single point, which is right for a straight trunk and wrong
 * for a leaning one: the whole crown would hang in the air beside the tree. This placer places one layer per
 * attachment instead, and {@link LeanTrunkPlacer} hands it one attachment per layer of the crown, each on the
 * trunk's own line — so the crown leans with the trunk.
 * <p>
 * The layer's radius is the spruce crown's own, counted down from the top of the crown, and the attachment carries
 * that count in its radius offset.
 *
 * @see "doc/design/magical_forest_vegetation.md" §9.1
 */
public class LeanFoliagePlacer extends FoliagePlacer {

	public static final MapCodec<LeanFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(i -> foliagePlacerParts(i)
			.and(IntProvider.codec(0, 24).fieldOf("trunk_height").forGetter(p -> p.trunkHeight))
			.apply(i, LeanFoliagePlacer::new));

	private final IntProvider trunkHeight;

	public LeanFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider trunkHeight) {
		super(radius, offset);
		this.trunkHeight = trunkHeight;
	}

	@Override
	protected FoliagePlacerType<?> type() {
		return GLWorldGen.LEAN_FOLIAGE_PLACER.get();
	}

	@Override
	protected void createFoliage(LevelSimulatedReader level, FoliageSetter blockSetter, RandomSource random,
								 TreeConfiguration config, int maxFreeTreeHeight, FoliageAttachment attachment, int foliageHeight,
								 int foliageRadius, int offset) {
		int radius = layerRadius(attachment.radiusOffset(), foliageRadius);
		placeLeavesRow(level, blockSetter, random, config, attachment.pos(), radius, 0, attachment.doubleTrunk());
		// the spruce crown reaches above the trunk it grows on, and so does this one
		if (attachment.radiusOffset() == 0 && offset > 0) {
			placeLeavesRow(level, blockSetter, random, config, attachment.pos().above(), radius, 0, attachment.doubleTrunk());
		}
	}

	/**
	 * The radius of a layer of the crown, which is the spruce crown's own: the top layer is a single block, and the
	 * layers below it widen a ring at a time.
	 */
	private static int layerRadius(int layer, int max) {
		int radius = 0, step = 2;
		for (int l = 0; l < layer; l++) {
			if (radius >= step) {
				radius -= 1;
				step = Math.min(step + 1, max);
			} else radius++;
		}
		return radius;
	}

	@Override
	public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
		return Math.max(4, height - this.trunkHeight.sample(random));
	}

	@Override
	protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
		return localX == range && localZ == range && range > 0;
	}

}

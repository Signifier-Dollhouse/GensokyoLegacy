package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/**
 * The hot dry cell of the mountain band in {@code doc/design/gensokyo_dimension_biome_guide.md}: the windswept foot of
 * the youkai mountain, which is what makes it a foothill and not a second cliff.
 * <p>
 * The first maple grove up the mountain, and the thinnest of the three: the wind is what thins it, so the maples here
 * stand a long way apart and the litter under them is mostly grass.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class YoukaiMountainFoothillsBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("youkai_mountain_foothills");

	private static final ResourceKey<PlacedFeature> MAPLE_TREES_PF = pf("foothills_maple_trees");

	public YoukaiMountainFoothillsBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.6f, 0.5f, 0x46707d, 0x8496a8, 0x6f8a58, 0x617c4e, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(CLIFF_FLOOR, List.of(MAPLE_TREES_PF, GensokyoBiomeFeatures.STONES_PF,
								GensokyoBiomeFeatures.FOOTHILL_MOSS_PF,
								GensokyoBiomeFeatures.MAPLE_FALLEN_LEAVES_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.trees(MAPLE_TREES_PF, reg.feature(GensokyoBiomeFeatures.MAPLE_TREES), CountPlacement.of(2));
	}

}

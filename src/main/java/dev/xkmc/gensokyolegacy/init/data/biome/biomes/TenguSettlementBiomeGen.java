package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The band above the mountain in {@code doc/design/gensokyolegacy_fixed_biomes_guide.md}: the tengu settlement, forest
 * ground with the foothills' moss and its own gravel paths.
 * <p>
 * A maple grove rather than a birch one: the settlement sits in the same maple belt as the valley below it, and the
 * paths and the moss are what tell it apart from the open valley floor.
 *
 * @see "doc/design/gensokyo_biomes.md" §7
 */
public class TenguSettlementBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("tengu_settlement");

	private static final ResourceKey<ConfiguredFeature<?, ?>> SETTLEMENT_PATHS = cf("settlement_paths");
	private static final ResourceKey<PlacedFeature> SETTLEMENT_PATHS_PF = pf("settlement_paths");
	private static final ResourceKey<PlacedFeature> MAPLE_TREES_PF = pf("settlement_maple_trees");

	public TenguSettlementBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.4f, 0.5f, 0x46707d, 0x93a6b8, 0x7a9464, 0x6b8456, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(FOREST_FLOOR, List.of(MAPLE_TREES_PF, SETTLEMENT_PATHS_PF,
								GensokyoBiomeFeatures.FOOTHILL_MOSS_PF,
								GensokyoBiomeFeatures.MAPLE_FALLEN_LEAVES_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		// paths are a feature and not a surface rule, so that they stay patchy and walkable instead of painting a biome
		FeatureUtils.register(ctx, SETTLEMENT_PATHS, Feature.RANDOM_PATCH, onDirt(14, new Palette()
				.add(Blocks.GRAVEL, 3).add(Blocks.COARSE_DIRT, 3).add(Blocks.COBBLESTONE, 2)
				.add(Blocks.MOSSY_COBBLESTONE, 1).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.trees(MAPLE_TREES_PF, reg.feature(GensokyoBiomeFeatures.MAPLE_TREES), CountPlacement.of(6));
		reg.cover(SETTLEMENT_PATHS_PF, reg.feature(SETTLEMENT_PATHS), RarityFilter.onAverageOnceEvery(4));
	}

}
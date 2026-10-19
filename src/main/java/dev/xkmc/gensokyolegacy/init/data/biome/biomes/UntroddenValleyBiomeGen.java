package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/**
 * The hot humid cell of the mountain band in {@code doc/design/gensokyo_dimension_biome_guide.md}: the untrodden
 * valley, taiga ground and loose stone and nothing else.
 * <p>
 * The maple grove the mountain band is built around, and the densest of the three: the conifers the taiga ground
 * carries are gone and the valley floor is half maple, half the oak and birch that grew here before it, at the ten
 * per chunk the pines used to be.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class UntroddenValleyBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("untrodden_valley");

	private static final ResourceKey<ConfiguredFeature<?, ?>> VALLEY_TREES = cf("valley_mixed_trees");
	private static final ResourceKey<PlacedFeature> VALLEY_TREES_PF = pf("valley_mixed_trees");

	public UntroddenValleyBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.5f, 0.8f, 0x3c6f7d, 0x7d8f9c, 0x6b8560, 0x5d7854, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(TAIGA_FLOOR, List.of(VALLEY_TREES_PF, VegetationPlacements.PATCH_BERRY_COMMON,
								GensokyoBiomeFeatures.STONES_PF, GensokyoBiomeFeatures.MAPLE_FALLEN_LEAVES_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		HolderGetter<ConfiguredFeature<?, ?>> trees = ctx.lookup(Registries.CONFIGURED_FEATURE);
		// the valley is the only grove the maples do not have to themselves, so it gets a selector of its own that
		// takes the shared maple one as a single entry: the odds compound, so half each of the list and the default
		FeatureUtils.register(ctx, VALLEY_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
				List.of(new WeightedPlacedFeature(
						PlacementUtils.inlinePlaced(trees.getOrThrow(GensokyoBiomeFeatures.MAPLE_TREES)), 0.5F),
						new WeightedPlacedFeature(
								PlacementUtils.inlinePlaced(trees.getOrThrow(TreeFeatures.OAK)), 0.5F)),
				PlacementUtils.inlinePlaced(trees.getOrThrow(TreeFeatures.BIRCH))));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.trees(VALLEY_TREES_PF, reg.feature(VALLEY_TREES), CountPlacement.of(10));
	}

}
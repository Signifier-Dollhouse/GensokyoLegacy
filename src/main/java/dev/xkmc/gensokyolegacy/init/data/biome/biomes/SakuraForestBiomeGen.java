package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * The high-erosion dry cell of the lower ground: Gensokyo's own cherry grove, and the biome a shrine sits in.
 * <p>
 * The only Gensokyo biome that takes vanilla's {@code addDefaultOres} chain rather than writing its steps out, which
 * is why it uses {@link #raw} instead of {@link #layers} — it is the one place the overworld's own ground cover is
 * wanted wholesale, with cherry added on top.
 * <p>
 * The pink cherry stands in for vanilla's one, at vanilla's own density. The purple one is the graveyard's tree and
 * grows in Muenzuka alone. The petals a vanilla grove drops are the mod's own fallen-leaf carpet instead.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class SakuraForestBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("sakura_forest");

	private static final ResourceKey<ConfiguredFeature<?, ?>> CHERRY_LITTER = cf("cherry_litter");
	private static final ResourceKey<PlacedFeature> CHERRY_TREES_PF = pf("cherry_trees");
	private static final ResourceKey<PlacedFeature> CHERRY_LITTER_PF = pf("cherry_litter");

	public SakuraForestBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		var builder = addDefaultOres(raw(ctx))
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_PLAIN)
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_CHERRY)
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CHERRY_TREES_PF)
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, CHERRY_LITTER_PF);
		return biome(true, 0.5f, 0.8f, 0xc0d8ff, 11983713, 11983713, new MobSpawnSettings.Builder(), builder, null);
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		// the grove is pink only, so the petals under it are the pink ones
		FeatureUtils.register(ctx, CHERRY_LITTER, Feature.RANDOM_PATCH,
				fallenLeaves(40, 6, GLNaturalBlocks.PINK_CHERRY_FALLEN_LEAVES));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.trees(CHERRY_TREES_PF, reg.feature(TreeType.PINK_CHERRY.cfKey), CountPlacement.of(15));
		reg.litter(CHERRY_LITTER_PF, reg.feature(CHERRY_LITTER), CountPlacement.of(2));
	}

}

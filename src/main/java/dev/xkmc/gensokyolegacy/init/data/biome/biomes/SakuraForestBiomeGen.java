package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * The high-erosion dry cell of the lower ground: Gensokyo's own cherry grove, and the biome a shrine sits in.
 * <p>
 * The only Gensokyo biome that takes vanilla's {@code addDefaultOres} chain rather than writing its steps out, which
 * is why it uses {@link #raw} instead of {@link #layers} — it is the one place the overworld's own ground cover is
 * wanted wholesale, with cherry added on top.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class SakuraForestBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("sakura_forest");

	public SakuraForestBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		var builder = addDefaultOres(raw(ctx))
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_PLAIN)
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_CHERRY)
				.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_CHERRY);
		return biome(true, 0.5f, 0.8f, 0xc0d8ff, 11983713, 11983713, new MobSpawnSettings.Builder(), builder, null);
	}

}
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

import java.util.List;

/**
 * The cold dry cell of the upper ground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the hill of the
 * nameless, a flowered meadow with the mountain's own rock under it.
 * <p>
 * Grass at meadow brightness, foliage well under it: the hill is open, so the only canopy it has is its own trees, and
 * a shadowed one is what makes it the hill of the nameless rather than a field of daisies.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class HillOfTheNamelessBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("hill_of_the_nameless");

	private static final ResourceKey<ConfiguredFeature<?, ?>> HILL_FLOWERS = cf("hill_of_the_nameless_flowers");
	private static final ResourceKey<PlacedFeature> HILL_FLOWERS_PF = pf("hill_of_the_nameless_flowers");

	public HillOfTheNamelessBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.3f, 0.4f, 0x4a7a86, 0x8aa0b0, 0x87a06a, 0x5f724b, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(MEADOW_GROUND,
								List.of(GensokyoBiomeFeatures.STONES_PF, HILL_FLOWERS_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, HILL_FLOWERS, Feature.RANDOM_PATCH, onDirt(32, new Palette()
				.add(Blocks.LILY_OF_THE_VALLEY, 4).add(Blocks.OXEYE_DAISY, 3)
				.add(Blocks.ALLIUM, 2).add(Blocks.TALL_GRASS, 2).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		// denser than the village's four: the hill is the one biome named for being flowered rather than tilled, and
		// meadow's own sparser flower_meadow sits on top of it
		reg.cover(HILL_FLOWERS_PF, reg.feature(HILL_FLOWERS), CountPlacement.of(7));
	}

}
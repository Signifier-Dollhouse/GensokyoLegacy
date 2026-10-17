package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
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
 * The dry half of the settlement's band, which {@code doc/design/gensokyolegacy_fixed_biomes_guide.md} does not name.
 * <p>
 * It is the only one of the fixed locations that humidity picks at all: continentalness still fixes its height, and
 * humidity alone splits it from the settlement beside it. It is a cliff ledge rather than a second town. The stone is
 * carried by the surface rule's steep test in the dimension rather than by a palette here, and precipitation stays on,
 * which is what keeps it off the snow line.
 *
 * @see "doc/design/gensokyo_biomes.md" §7
 */
public class SecretHeavenCliffBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("secret_heaven_cliff");

	private static final ResourceKey<ConfiguredFeature<?, ?>> CLIFF_SCREE = cf("cliff_scree");
	private static final ResourceKey<PlacedFeature> CLIFF_SCREE_PF = pf("cliff_scree");

	public SecretHeavenCliffBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.35f, 0.2f, 0x3f7482, 0x9aa8b8, 0x8c8b5a, 0x7d7d50, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN,
								VegetationPlacements.TREES_WINDSWEPT_FOREST, VegetationPlacements.FLOWER_DEFAULT,
								VegetationPlacements.PATCH_GRASS_BADLANDS, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
								VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
								VegetationPlacements.PATCH_PUMPKIN),
								List.of(GensokyoBiomeFeatures.STONES_PF, CLIFF_SCREE_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		// tuff and gravel against a little dry scrub: the cliff is barer than the town beside it, not bare like the
		// summit above it, so the palette keeps a grass and a bush weight and leans on tuff for the rest
		FeatureUtils.register(ctx, CLIFF_SCREE, Feature.RANDOM_PATCH, onDirt(18, new Palette()
				.add(Blocks.GRAVEL, 3).add(Blocks.TUFF, 3).add(Blocks.COARSE_DIRT, 2)
				.add(Blocks.DEAD_BUSH, 1).add(Blocks.SHORT_GRASS, 1).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		// sparse, where the settlement's moss covers four times over
		reg.cover(CLIFF_SCREE_PF, reg.feature(CLIFF_SCREE), CountPlacement.of(1));
	}

}
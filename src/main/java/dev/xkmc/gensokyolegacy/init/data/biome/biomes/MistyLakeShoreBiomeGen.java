package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
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
 * The strip above the Misty Lake waterline, of {@code doc/design/gensokyolegacy_fixed_biomes_guide.md}.
 * <p>
 * Its reeds are the lake's own cattails at a denser count, planted by a placed feature of its own so the two bands
 * can be tuned apart.
 *
 * @see "doc/design/gensokyo_biomes.md" §7
 */
public class MistyLakeShoreBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("misty_lake_shore");

	private static final ResourceKey<ConfiguredFeature<?, ?>> SHORE_SAND = cf("shore_sand");
	private static final ResourceKey<PlacedFeature> SHORE_SAND_PF = pf("shore_sand");
	private static final ResourceKey<PlacedFeature> SHORE_REEDS_PF = pf("shore_reeds");
	private static final ResourceKey<ConfiguredFeature<?, ?>> SHORE_FLOWERS = cf("shore_flowers");
	private static final ResourceKey<PlacedFeature> SHORE_FLOWERS_PF = pf("shore_flowers");

	public MistyLakeShoreBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.4f, 0.7f, 0x3a7f8c, 0x9cc0cc, 0x7da863, 0x6c965a, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN, VegetationPlacements.FLOWER_DEFAULT,
										VegetationPlacements.PATCH_GRASS_BADLANDS, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
										VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
										VegetationPlacements.PATCH_PUMPKIN),
								List.of(GensokyoBiomeFeatures.STONES_PF, SHORE_SAND_PF, SHORE_REEDS_PF,
										SHORE_FLOWERS_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, SHORE_SAND, Feature.RANDOM_PATCH, onGround(18, new Palette()
						.add(Blocks.SAND, 3).add(Blocks.GRAVEL, 2).add(Blocks.MUD, 2).add(Blocks.COARSE_DIRT, 1).build(),
				Blocks.SAND, Blocks.GRAVEL, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.CLAY));
		FeatureUtils.register(ctx, SHORE_FLOWERS, Feature.RANDOM_PATCH, onDirt(24, new Palette()
				.add(GLNaturalBlocks.BRACKEN, 3).add(Blocks.FERN, 2).add(Blocks.LILY_OF_THE_VALLEY, 2)
				.add(Blocks.OXEYE_DAISY, 2).add(Blocks.SHORT_GRASS, 2).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(SHORE_SAND_PF, reg.feature(SHORE_SAND), CountPlacement.of(4));
		reg.cover(SHORE_REEDS_PF, reg.feature(GensokyoBiomeFeatures.LAKE_CATTAILS), CountPlacement.of(3));
		reg.cover(SHORE_FLOWERS_PF, reg.feature(SHORE_FLOWERS), CountPlacement.of(3));
	}

}
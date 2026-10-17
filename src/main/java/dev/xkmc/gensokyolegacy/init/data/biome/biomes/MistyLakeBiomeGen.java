package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
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
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The drowned set of {@code doc/design/gensokyolegacy_fixed_biomes_guide.md}: a broad open lake, and the band it owns
 * is the one below the waterline.
 * <p>
 * Squid and salmon, no drowned mobs, because there is nothing here to drown in. {@code minecraft:trees_water} is the
 * one feature this biome does not take from a base: mangrove and dark oak water trees are a swamp signal, and the
 * guide asks for a lake that does not read as one.
 *
 * @see "doc/design/gensokyo_biomes.md" §7
 */
public class MistyLakeBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("misty_lake");

	private static final ResourceKey<ConfiguredFeature<?, ?>> LAKE_CLAY = cf("lake_clay");
	private static final ResourceKey<PlacedFeature> LAKE_CLAY_PF = pf("lake_clay");
	private static final ResourceKey<PlacedFeature> LILY_PADS_PF = pf("misty_lake_lily_pads");

	public MistyLakeBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.3f, 0.9f, 0x3a7f8c, 0x86b4c8, 0x6f9c74, 0x5f8a66, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN, VegetationPlacements.FLOWER_DEFAULT,
								VegetationPlacements.PATCH_GRASS_BADLANDS, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
								VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
								VegetationPlacements.PATCH_PUMPKIN, AquaticPlacements.SEAGRASS_RIVER),
								List.of(LAKE_CLAY_PF, GensokyoBiomeFeatures.LAKE_CATTAILS_PF, LILY_PADS_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		// the lakebed is sand and gravel under the surface rule, so this sits on those and not on BlockTags.DIRT
		FeatureUtils.register(ctx, LAKE_CLAY, Feature.RANDOM_PATCH, onGround(14, new Palette()
				.add(Blocks.CLAY, 3).add(Blocks.GRAVEL, 2).add(Blocks.MUD, 1).build(),
				Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY, Blocks.DIRT));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(LAKE_CLAY_PF, reg.feature(LAKE_CLAY), CountPlacement.of(2));
		// vanilla's lily pad *configured* feature, which already knows it needs water under it, behind a placed feature
		// of our own: a vanilla placed feature has a fixed position in vanilla's chains, and appending one to a tail is
		// a cycle waiting for a biome that already uses it mid-chain
		reg.cover(LILY_PADS_PF, reg.feature(VegetationFeatures.PATCH_WATERLILY), RarityFilter.onAverageOnceEvery(3));
	}

}
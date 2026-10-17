package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/**
 * The cold humid cell of the mountain band in {@code doc/design/gensokyo_dimension_biome_guide.md}: the Genbu ravine,
 * a wet cut in the mountain with the lush caves' own growth hanging off its walls.
 * <p>
 * Swamp is what makes it a wet ravine on the surface, plus lush caves below it: the biome's own moss and mud stay,
 * because lush caves brings growth and vines rather than a ground cover to replace them with. {@code ore_clay} is
 * lush caves' one special ore, and it goes where vanilla puts it — after the default set, before the soft disks. The
 * springs are its own — water at three times vanilla's count and no lava, which is what replacing the shared pair
 * does.
 *
 * @see "doc/design/gensokyo_biomes.md" §4
 */
public class GenbuRavineBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("genbu_ravine");

	private static final ResourceKey<ConfiguredFeature<?, ?>> RAVINE_MUD_DISK = cf("ravine_mud_disk");
	private static final ResourceKey<PlacedFeature> RAVINE_MUD_PF = pf("ravine_mud");
	private static final ResourceKey<ConfiguredFeature<?, ?>> RAVINE_MOSS = cf("ravine_moss");
	private static final ResourceKey<PlacedFeature> RAVINE_MOSS_PF = pf("ravine_moss");
	private static final ResourceKey<PlacedFeature> RAVINE_SPRING_WATER_PF = pf("ravine_spring_water");

	/** Dinosaur bones. The ravine is the one place below ground where they are still bones and not a theme. */
	private static final List<ResourceKey<PlacedFeature>> FOSSILS = List.of(
			CavePlacements.FOSSIL_UPPER, CavePlacements.FOSSIL_LOWER);

	/**
	 * The ravine's springs: vanilla's own {@code spring_water} at three times the count, and no lava at all.
	 * <p>
	 * It <b>replaces</b> the shared {@link AbstractBiomeGen#SPRINGS} rather than joining it — the feature is in the same
	 * step, so carrying vanilla's as well would place every spring twice and make the count a lie. Replacing rather
	 * than adding is also what takes the lava with it, and that is the point: a ravine is a wet place and the
	 * shared {@code spring_lava} was the only lava in it.
	 */
	private static final List<ResourceKey<PlacedFeature>> RAVINE_SPRINGS = List.of(RAVINE_SPRING_WATER_PF);

	public GenbuRavineBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.35f, 0.9f, 0x39604f, 0x5f7a6a, 0x5a7a5c, 0x4e6e52, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.structures(FOSSILS)
						.ores(join(OVERWORLD_ORES, LUSH_CAVES_ORES,
								List.of(MiscOverworldPlacements.DISK_CLAY, RAVINE_MUD_PF)))
						.springs(RAVINE_SPRINGS)
						.vegetation(join(SWAMP_GROUND, LUSH_CAVES,
								List.of(GensokyoBiomeFeatures.STONES_PF, RAVINE_MOSS_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, RAVINE_MUD_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.MUD),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK, Blocks.DIRT), UniformInt.of(3, 6), 1));
		FeatureUtils.register(ctx, RAVINE_MOSS, Feature.RANDOM_PATCH, onDirt(40, new Palette()
				.add(Blocks.MOSS_CARPET, 3).add(Blocks.LARGE_FERN, 2)
				.add(GLNaturalBlocks.BRACKEN, 2).add(Blocks.FERN, 2).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(RAVINE_MUD_PF, reg.feature(RAVINE_MUD_DISK), CountPlacement.of(3));
		reg.cover(RAVINE_MOSS_PF, reg.feature(RAVINE_MOSS), CountPlacement.of(6));
		// The ravine's springs, over vanilla's own configured feature and its own height range, with the count
		// raised and nothing else touched. It replaces SPRINGS rather than joining them: the feature is in the
		// same step, so carrying vanilla's as well would place every spring twice and make the count a lie. The
		// ravine is wet and is left with no lava spring at all, neither its own nor the shared one — the water is
		// the whole of what a ravine has to say about fluid.
		reg.placed(RAVINE_SPRING_WATER_PF, reg.feature(MiscOverworldFeatures.SPRING_WATER),
				CountPlacement.of(75), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(192)),
				BiomeFilter.biome());
	}

}
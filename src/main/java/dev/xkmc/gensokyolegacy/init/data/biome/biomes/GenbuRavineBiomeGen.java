package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.stair.StairPoolConfig;
import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.CaveFeatures;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
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
 * <p>
 * The growth is lush caves' whole set, in the order the feature sorter reads: the floor growth, the two vine sets, the
 * ceiling growth and the rooted azalea all hang off the same cave surfaces, so taking some of them would be taking a
 * fragment of one decoration rather than a style. The one piece the ravine lays itself is the clay patch, in basalt.
 * <p>
 * The ravine's own four — the plates, the pools cut into the steps, the moss and the stones — come first, and the
 * swamp's trees and grass after them: the plates have to be down before anything grows on them, or a pool of them
 * ends up under a patch of grass that grew first.
 *
 * @see "doc/design/gensokyo_biomes.md" §1
 */
public class GenbuRavineBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("genbu_ravine");

	private static final ResourceKey<ConfiguredFeature<?, ?>> RAVINE_MUD_DISK = cf("ravine_mud_disk");
	private static final ResourceKey<PlacedFeature> RAVINE_MUD_PF = pf("ravine_mud");
	private static final ResourceKey<ConfiguredFeature<?, ?>> RAVINE_MOSS = cf("ravine_moss");
	private static final ResourceKey<PlacedFeature> RAVINE_MOSS_PF = pf("ravine_moss");
	private static final ResourceKey<PlacedFeature> RAVINE_SPRING_WATER_PF = pf("ravine_spring_water");

	/**
	 * The ravine's own floor plates: the patch lush caves lays in clay, laid here in basalt.
	 * <p>
	 * Two branches, as vanilla has: a dry plate and a pool of them, with dripleaves growing on either.
	 */
	private static final ResourceKey<ConfiguredFeature<?, ?>> RAVINE_BASALT_POOL = cf("ravine_basalt_pool");
	private static final ResourceKey<PlacedFeature> RAVINE_BASALT_PF = pf("ravine_basalt");

	/**
	 * The ravine's own loose stone: the shared {@code stones} configured feature under a placed feature of its own.
	 * <p>
	 * The shared one is in seven other biomes' tails and cannot move, and this one has to: the ravine lays its floor
	 * before the swamp's trees and grass, so the plates are down before anything grows on them.
	 */
	private static final ResourceKey<PlacedFeature> RAVINE_STONES_PF = pf("ravine_stones");

	/**
	 * The pools cut into the ravine's basalt steps: a tread in the middle of a slope becomes a pool of its own.
	 */
	private static final ResourceKey<ConfiguredFeature<?, ?>> RAVINE_STAIR_POOL = cf("ravine_stair_pool");
	private static final ResourceKey<PlacedFeature> RAVINE_STAIR_POOL_PF = pf("ravine_stair_pool");

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
						.ores(join(OVERWORLD_ORES, List.of(OrePlacements.ORE_CLAY, MiscOverworldPlacements.DISK_CLAY,
								RAVINE_MUD_PF)))
						.springs(RAVINE_SPRINGS)
						.vegetation(List.of(
								RAVINE_BASALT_PF, RAVINE_STAIR_POOL_PF, RAVINE_MOSS_PF, RAVINE_STONES_PF,
								CavePlacements.GLOW_LICHEN, VegetationPlacements.TREES_SWAMP,
								VegetationPlacements.FLOWER_SWAMP, VegetationPlacements.PATCH_GRASS_NORMAL,
								VegetationPlacements.PATCH_DEAD_BUSH, VegetationPlacements.PATCH_WATERLILY,
								VegetationPlacements.BROWN_MUSHROOM_SWAMP, VegetationPlacements.RED_MUSHROOM_SWAMP,
								VegetationPlacements.BROWN_MUSHROOM_NORMAL, VegetationPlacements.RED_MUSHROOM_NORMAL,
								VegetationPlacements.PATCH_SUGAR_CANE_SWAMP, VegetationPlacements.PATCH_PUMPKIN,
								AquaticPlacements.SEAGRASS_SWAMP,
								CavePlacements.LUSH_CAVES_CEILING_VEGETATION, CavePlacements.CAVE_VINES,
								CavePlacements.LUSH_CAVES_VEGETATION, CavePlacements.ROOTED_AZALEA_TREE,
								CavePlacements.SPORE_BLOSSOM, CavePlacements.CLASSIC_VINES))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, RAVINE_MUD_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.MUD),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK, Blocks.DIRT), UniformInt.of(2, 4), 1));
		FeatureUtils.register(ctx, RAVINE_MOSS, Feature.RANDOM_PATCH, onDirt(20, 5, new Palette()
				.add(Blocks.MOSS_CARPET, 3).add(Blocks.LARGE_FERN, 2)
				.add(GLNaturalBlocks.BRACKEN, 2).add(Blocks.FERN, 2).build()));
		// The ravine's own version of lush caves' clay patch: a pool of basalt with dripleaves growing in it, the
		// same numbers vanilla's own pool branch uses. The dripleaf is vanilla's own configured feature, referenced
		// by id, so the JSON stays a reference to it rather than a copy. The patch type is ours: a dripleaf's stem
		// wants a support it can live on, and basalt is not one, so the plant gets mud under it.
		var features = ctx.lookup(Registries.CONFIGURED_FEATURE);
		var dripleaf = PlacementUtils.inlinePlaced(features.getOrThrow(CaveFeatures.DRIPLEAF));
		FeatureUtils.register(ctx, RAVINE_BASALT_POOL, GLWorldGen.MUD_WATERLOGGED_VEGETATION_PATCH.get(),
				basaltPlate(dripleaf, 5, 0.1F));
		// The pools cut into the basalt steps: a tread in the middle of a slope becomes a pool of its own, and the run
		// of treads beside it a rim of water along the slope. Seven blocks is how far one rim runs, and the heightmap
		// is the one the plates are laid on.
		FeatureUtils.register(ctx, RAVINE_STAIR_POOL, GLWorldGen.STAIR_POOL.get(), new StairPoolConfig(
				Blocks.BASALT.defaultBlockState(), Blocks.WATER.defaultBlockState(), 7,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES));
	}

	/**
	 * One basalt plate. The numbers are vanilla's own pool branch: it reaches five blocks up and grows a dripleaf one
	 * time in ten.
	 */
	private static VegetationPatchConfiguration basaltPlate(Holder<PlacedFeature> dripleaf, int verticalRange,
															float vegetationChance) {
		return new VegetationPatchConfiguration(
				BlockTags.LUSH_GROUND_REPLACEABLE,
				BlockStateProvider.simple(Blocks.BASALT),
				dripleaf,
				CaveSurface.FLOOR,
				ConstantInt.of(3),
				0.8F,
				verticalRange,
				vegetationChance,
				UniformInt.of(4, 7),
				0.7F);
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(RAVINE_MUD_PF, reg.feature(RAVINE_MUD_DISK), CountPlacement.of(2));
		// The ravine's own floor, in the order the ravine's list reads it: the plates, the pools cut into the steps,
		// the moss, the stones, and only then the swamp's trees and grass. The plates have to be down first — a pool
		// of them under a patch of grass that grew before it is grass standing on water.
		// The plates are on the surface: the ravine is a cut in the mountain, and the floor of the cut is the floor
		// of the ravine. The no-leaves heightmap is the one that finds it, because the swamp's trees are on it.
		reg.floor(RAVINE_BASALT_PF, reg.feature(RAVINE_BASALT_POOL), CountPlacement.of(8));
		reg.floor(RAVINE_STAIR_POOL_PF, reg.feature(RAVINE_STAIR_POOL), CountPlacement.of(24));
		reg.cover(RAVINE_MOSS_PF, reg.feature(RAVINE_MOSS), CountPlacement.of(3));
		reg.cover(RAVINE_STONES_PF, reg.feature(GensokyoBiomeFeatures.STONES), CountPlacement.of(2));
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
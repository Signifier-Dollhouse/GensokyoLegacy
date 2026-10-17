package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.NetherFeatures;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The hot dry cell of the underground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the hell of blazing
 * fires, a hall of basalt with lava in the floor.
 * <p>
 * Basalt deltas. The column runs are the find here: vanilla lays them on the nether's open floor, and in a cave they
 * stand up off the floor instead, which is a hall of basalt rather than a delta. {@code minecraft:delta} is the other
 * half and was missing: it is what pours the lava into the pockets the columns stand on, and the biome had the
 * columns with nothing to fill them. spring_delta comes with them and is the one nether spring that does: the delta's
 * lava is what the biome is named for, and the surface rule only gives it magma block, which glows but does not flow.
 *
 * @see "doc/design/gensokyo_biomes.md" §2.2
 */
public class HellOfBlazingFiresBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("hell_of_blazing_fires");

	/**
	 * The three surface features of the deltas, in vanilla's order: the lava pools, then the two basalt column
	 * runs, which is what makes a lava hall a delta and not a cave. {@code minecraft:delta} is the one that fills
	 * them — forty attempts on every layer, each dropping a pool of lava with a magma rim into the one-block air
	 * pockets in the floor. Without it the biome gets a lava spring every sixteenth try and nothing else, which is
	 * a single block of lava rather than a delta.
	 */
	private static final List<ResourceKey<PlacedFeature>> BASALT_COLUMNS = List.of(
			NetherPlacements.DELTA, NetherPlacements.SMALL_BASALT_COLUMNS, NetherPlacements.LARGE_BASALT_COLUMNS);

	/** The deltas' lava, for this biome alone: every other nether spring is left out, this one is the lava itself. */
	private static final List<ResourceKey<PlacedFeature>> DELTA_LAVA = List.of(NetherPlacements.SPRING_DELTA);

	private static final ResourceKey<PlacedFeature> BLAZING_HELL_LAVA_PF = pf("blazing_hell_lava_spring");

	/** The blazing fires' own lava spring, over the nether spring's block list. See {@link #placed}. */
	private static final List<ResourceKey<PlacedFeature>> BLAZING_HELL_LAVA = List.of(BLAZING_HELL_LAVA_PF);

	private static final ResourceKey<ConfiguredFeature<?, ?>> FIRE_VENT = cf("fire_vent");
	private static final ResourceKey<PlacedFeature> FIRE_VENT_PF = pf("fire_vent");
	private static final ResourceKey<ConfiguredFeature<?, ?>> ASH_PATCH = cf("ash_patch");
	private static final ResourceKey<PlacedFeature> ASH_PATCH_PF = pf("ash_patch");

	public HellOfBlazingFiresBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 1.5f, 0.0f, 0xc2410a, 0x8c2f0e, 0x6b4030, 0x5a3528, new MobSpawnSettings.Builder(),
				layers(ctx)
						.surfaceStructures(BASALT_COLUMNS)
						.ores(OVERWORLD_ORES)
						.underground(join(NETHER_COMMON,
								List.of(NetherPlacements.BASALT_BLOBS, NetherPlacements.BLACKSTONE_BLOBS),
								List.of(FIRE_VENT_PF, ASH_PATCH_PF)))
						.springs(join(SPRINGS, DELTA_LAVA, BLAZING_HELL_LAVA)));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, FIRE_VENT, Feature.RANDOM_PATCH, onStone(16, new Palette()
				.add(Blocks.MAGMA_BLOCK, 3).add(Blocks.BLACKSTONE, 2).add(Blocks.BASALT, 2).add(Blocks.GRAVEL, 1).build()));
		FeatureUtils.register(ctx, ASH_PATCH, Feature.RANDOM_PATCH, onStone(24, new Palette()
				.add(Blocks.SOUL_SOIL, 3).add(Blocks.BLACKSTONE, 2).add(Blocks.GRAVEL, 1).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.deposit(FIRE_VENT_PF, reg.feature(FIRE_VENT), RarityFilter.onAverageOnceEvery(4));
		reg.deposit(ASH_PATCH_PF, reg.feature(ASH_PATCH), CountPlacement.of(5));
		// A lava spring, and the reason it is its own placed feature rather than vanilla's `spring_lava`. The
		// overworld one will not place a single block in the hell biomes: its `valid_blocks` are stone, granite,
		// diorite, andesite, deepslate, tuff, calcite and dirt, and their floors are netherrack, blackstone, magma
		// and basalt. The nether spring's valid blocks are netherrack, soul sand, gravel, magma and blackstone,
		// which is two thirds of what the blazing fires stand on.
		reg.placed(BLAZING_HELL_LAVA_PF, reg.feature(NetherFeatures.SPRING_LAVA_NETHER),
				CountPlacement.of(6), InSquarePlacement.spread(),
				PlacementUtils.RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT, BiomeFilter.biome());
	}

}
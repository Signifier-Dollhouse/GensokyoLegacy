package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLFurniture;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The cold humid cell of the lower ground in {@code doc/design/gensokyo_dimension_biome_guide.md}: podzol underfoot, a
 * stone marker now and then, lilies of the valley gone to seed.
 * <p>
 * The soil disk runs in the ore step, which is what puts the podzol under the grass rather than on top of it.
 * <p>
 * A burial grove: the locust is half of what grows here, because it is the one tree that grows back over a grave,
 * and the oak and birch that grew here before it make up the rest. It is thin enough at four a chunk that the
 * ground still reads as a graveyard rather than as a wood.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class MuenzukaBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("muenzuka");

	private static final ResourceKey<ConfiguredFeature<?, ?>> PODZOL_DISK = cf("muenzuka_podzol_disk");
	private static final ResourceKey<PlacedFeature> PODZOL_PF = pf("muenzuka_podzol");
	private static final ResourceKey<ConfiguredFeature<?, ?>> GRAVE_MARKER = cf("grave_marker");
	private static final ResourceKey<PlacedFeature> GRAVE_MARKER_PF = pf("grave_marker");
	private static final ResourceKey<ConfiguredFeature<?, ?>> GRAVESTONE = cf("gravestone");
	private static final ResourceKey<PlacedFeature> GRAVESTONE_PF = pf("gravestone");
	private static final ResourceKey<ConfiguredFeature<?, ?>> GHOST_FLOWERS = cf("ghost_flowers");
	private static final ResourceKey<PlacedFeature> GHOST_FLOWERS_PF = pf("ghost_flowers");
	private static final ResourceKey<ConfiguredFeature<?, ?>> MUENZUKA_TREES = cf("muenzuka_trees");
	private static final ResourceKey<PlacedFeature> MUENZUKA_TREES_PF = pf("muenzuka_trees");

	public MuenzukaBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.3f, 0.9f, 0x38605c, 0x3f4a46, 0x6a7058, 0x5a604e, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, List.of(PODZOL_PF)))
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN, VegetationPlacements.FOREST_FLOWERS,
								VegetationPlacements.FLOWER_DEFAULT,
								VegetationPlacements.PATCH_GRASS_FOREST, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
								VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
								VegetationPlacements.PATCH_PUMPKIN),
								List.of(MUENZUKA_TREES_PF, GensokyoBiomeFeatures.STONES_PF, GRAVE_MARKER_PF,
										GRAVESTONE_PF, GHOST_FLOWERS_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, PODZOL_DISK, Feature.DISK, new DiskConfiguration(
				RuleBasedBlockStateProvider.simple(Blocks.PODZOL),
				BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK), UniformInt.of(2, 4), 1));
		FeatureUtils.register(ctx, GRAVE_MARKER, Feature.RANDOM_PATCH, onDirt(8, new Palette()
				.add(Blocks.COBBLESTONE, 3).add(Blocks.MOSSY_COBBLESTONE, 2).add(Blocks.CRACKED_STONE_BRICKS, 1).build()));
		// one marker standing up, where the patch above it is low rubble lying on the ground
		FeatureUtils.register(ctx, GRAVESTONE, Feature.RANDOM_PATCH, patch(4, 7, 0,
				new Palette().add(GLFurniture.WASTELAND_GRAVESTONE_SMALL, 1).build(), GLWorldGen.DOUBLE_BLOCK.get()));
		FeatureUtils.register(ctx, GHOST_FLOWERS, Feature.RANDOM_PATCH, onDirt(24, new Palette()
				.add(Blocks.LILY_OF_THE_VALLEY, 3).add(Blocks.OXEYE_DAISY, 3).add(Blocks.ALLIUM, 2)
				.add(Blocks.DEAD_BUSH, 2).add(GLNaturalBlocks.EUGUNE_GHOST_FIRE, 1).build()));

		// the odds compound down the list, so half each of the two entries leaves a quarter for the default
		HolderGetter<ConfiguredFeature<?, ?>> trees = ctx.lookup(Registries.CONFIGURED_FEATURE);
		FeatureUtils.register(ctx, MUENZUKA_TREES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(
				List.of(new WeightedPlacedFeature(
						PlacementUtils.inlinePlaced(trees.getOrThrow(TreeType.LOCUST.cfKey)), 0.5F),
						new WeightedPlacedFeature(
								PlacementUtils.inlinePlaced(trees.getOrThrow(TreeFeatures.OAK)), 0.5F)),
				PlacementUtils.inlinePlaced(trees.getOrThrow(TreeFeatures.BIRCH))));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(PODZOL_PF, reg.feature(PODZOL_DISK), CountPlacement.of(3));
		reg.trees(MUENZUKA_TREES_PF, reg.feature(MUENZUKA_TREES), CountPlacement.of(4));
		reg.cover(GRAVE_MARKER_PF, reg.feature(GRAVE_MARKER), RarityFilter.onAverageOnceEvery(4));
		reg.floor(GRAVESTONE_PF, reg.feature(GRAVESTONE), CountPlacement.of(1));
		reg.cover(GHOST_FLOWERS_PF, reg.feature(GHOST_FLOWERS), CountPlacement.of(3));
	}

}
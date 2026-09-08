package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.MushroomFeatures.MushroomTreeType;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

import java.util.List;

import static net.minecraft.data.worldgen.features.TreeFeatures.HUGE_BROWN_MUSHROOM;
import static net.minecraft.data.worldgen.features.TreeFeatures.HUGE_RED_MUSHROOM;

public class GLFeatureGen {

	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_GRASS = cf("magical_forest_grass");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_FLOWERS = cf("magical_forest_flowers");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_MUSHROOMS = cf("magical_forest_mushrooms");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_VEGETATION = cf("magical_forest_vegetation");

	public static final ResourceKey<PlacedFeature> MAGICAL_FOREST_GRASS_PLACED = pf("magical_forest_grass");
	public static final ResourceKey<PlacedFeature> MAGICAL_FOREST_FLOWERS_PLACED = pf("magical_forest_flowers");
	public static final ResourceKey<PlacedFeature> MAGICAL_FOREST_MUSHROOMS_PLACED = pf("magical_forest_mushrooms");
	public static final ResourceKey<PlacedFeature> MAGICAL_FOREST_VEGETATION_PLACED = pf("magical_forest_vegetation");

	public static void init(DataProviderInitializer init) {
		init.add(Registries.CONFIGURED_FEATURE, ctx -> {
			for (var type : MushroomTreeType.values()) {
				var set = type.set.get();
				ctx.register(type.cfKey, new ConfiguredFeature<>(
						GLWorldGen.MUSHROOM_TREES.get(type).get(),
						new HugeMushroomFeatureConfiguration(
								BlockStateProvider.simple(set.block.get()),
								BlockStateProvider.simple(set.stem.get()),
								type.radius
						)
				));
			}
			for (var type : TreeType.values()) {
				ctx.register(type.cfKey, type.createConfiguredFeature());
			}
			var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
			FeatureUtils.register(ctx, MAGICAL_FOREST_GRASS, Feature.RANDOM_PATCH,
					FeatureUtils.simpleRandomPatchConfiguration(32,
							PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
									new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
											.add(GLNaturalBlocks.BROOM_GRASS.get().defaultBlockState(), 6)
											.add(GLNaturalBlocks.BRACKEN.get().defaultBlockState(), 2)
											.build())))));
			FeatureUtils.register(ctx, MAGICAL_FOREST_FLOWERS, Feature.RANDOM_PATCH,
					FeatureUtils.simpleRandomPatchConfiguration(32,
							PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
									new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
											.add(GLNaturalBlocks.STAR_FLOWER.get().defaultBlockState(), 3)
											.add(GLNaturalBlocks.EUGUNE_RED.get().defaultBlockState(), 2)
											.add(GLNaturalBlocks.EUGUNE_BROWN.get().defaultBlockState(), 2)
											.add(GLNaturalBlocks.EUGUNE_GHOST_FIRE.get().defaultBlockState(), 1)
											.build())))));
			FeatureUtils.register(ctx, MAGICAL_FOREST_MUSHROOMS, Feature.RANDOM_PATCH,
					FeatureUtils.simpleRandomPatchConfiguration(32,
							PlacementUtils.onlyWhenEmpty(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
									new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
											.add(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap.get().defaultBlockState(), 2)
											.add(GLNaturalBlocks.DREAM_MUSHROOM_SET.cap.get().defaultBlockState(), 2)
											.add(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap.get().defaultBlockState(), 1)
											.build())))));
			FeatureUtils.register(ctx, MAGICAL_FOREST_VEGETATION, Feature.RANDOM_SELECTOR,
					new RandomFeatureConfiguration(List.of(
							new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(MushroomTreeType.GHOST_FIRE.cfKey)), 0.15F),
							new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(MushroomTreeType.DREAM.cfKey)), 0.1F),
							new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(MushroomTreeType.DEMONIC_MIASMA.cfKey)), 0.05F),
							new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(HUGE_BROWN_MUSHROOM)), 0.1F),
							new WeightedPlacedFeature(PlacementUtils.inlinePlaced(cf.getOrThrow(HUGE_RED_MUSHROOM)), 0.05F)
					), PlacementUtils.inlinePlaced(cf.getOrThrow(TreeType.BLUE_FIR.cfKey))));
		});
		init.add(Registries.PLACED_FEATURE, ctx -> {
			var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
			PlacementUtils.register(ctx, MAGICAL_FOREST_VEGETATION_PLACED, cf.getOrThrow(MAGICAL_FOREST_VEGETATION),
					CountPlacement.of(14), InSquarePlacement.spread(), SurfaceWaterDepthFilter.forMaxDepth(0),
					PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, BiomeFilter.biome());
			PlacementUtils.register(ctx, MAGICAL_FOREST_GRASS_PLACED, cf.getOrThrow(MAGICAL_FOREST_GRASS),
					CountPlacement.of(4), InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP_WORLD_SURFACE, BiomeFilter.biome());
			PlacementUtils.register(ctx, MAGICAL_FOREST_FLOWERS_PLACED, cf.getOrThrow(MAGICAL_FOREST_FLOWERS),
					CountPlacement.of(3), InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
			PlacementUtils.register(ctx, MAGICAL_FOREST_MUSHROOMS_PLACED, cf.getOrThrow(MAGICAL_FOREST_MUSHROOMS),
					CountPlacement.of(2), InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP, BiomeFilter.biome());
		});
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> cf(String id) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc(id));
	}

	private static ResourceKey<PlacedFeature> pf(String id) {
		return ResourceKey.create(Registries.PLACED_FEATURE, GensokyoLegacy.loc(id));
	}

}
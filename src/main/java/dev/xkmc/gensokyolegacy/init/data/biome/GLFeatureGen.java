package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.MushroomFeatures.MushroomTreeType;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;


public class GLFeatureGen {

	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_GRASS = cf("magical_forest_grass");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_FLOWERS = cf("magical_forest_flowers");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_MUSHROOMS = cf("magical_forest_mushrooms");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_DISK_COARSE_DIRT = cf("magical_forest_disk_coarse_dirt");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_DISK_PODZOL = cf("magical_forest_disk_podzol");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_DISK_MYCELIUM = cf("magical_forest_disk_mycelium");
	public static final ResourceKey<ConfiguredFeature<?, ?>> MAGICAL_FOREST_DISK_MOSS = cf("magical_forest_disk_moss");

	public static final ResourceKey<PlacedFeature> MAGICAL_FOREST_DISK_COARSE_DIRT_PLACED = pf("magical_forest_disk_coarse_dirt");
	public static final ResourceKey<PlacedFeature> MAGICAL_FOREST_DISK_PODZOL_PLACED = pf("magical_forest_disk_podzol");
	public static final ResourceKey<PlacedFeature> MAGICAL_FOREST_DISK_MYCELIUM_PLACED = pf("magical_forest_disk_mycelium");
	public static final ResourceKey<PlacedFeature> MAGICAL_FOREST_DISK_MOSS_PLACED = pf("magical_forest_disk_moss");

	public static void init(DataProviderInitializer init) {
		init.add(Registries.CONFIGURED_FEATURE, ctx -> {
			for (var type : MushroomTreeType.values()) {
				var set = type.set.get();
				ctx.register(type.cfKey, new ConfiguredFeature<>(
						GLWorldGen.MUSHROOM_TREES.get(type).get(),
						new HugeMushroomFeatureConfiguration(
								BlockStateProvider.simple(set.block.get().defaultBlockState()
										.setValue(HugeMushroomBlock.DOWN, false)),
								BlockStateProvider.simple(set.stem.get().defaultBlockState()
										.setValue(HugeMushroomBlock.UP, false)
										.setValue(HugeMushroomBlock.DOWN, false)),
								type.radius
						)
				));
			}
			for (var type : TreeType.values()) {
				ctx.register(type.cfKey, type.createConfiguredFeature());
			}
		FeatureUtils.register(ctx, MAGICAL_FOREST_GRASS, Feature.RANDOM_PATCH,
				FeatureUtils.simpleRandomPatchConfiguration(24,
						PlacementUtils.filtered(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
										new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
												.add(GLNaturalBlocks.BROOM_GRASS.get().defaultBlockState(), 1)
												.add(GLNaturalBlocks.BRACKEN.get().defaultBlockState(), 2)
												.build())),
									BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
											BlockPredicate.matchesTag(Direction.DOWN.getNormal(), BlockTags.DIRT)))));
			FeatureUtils.register(ctx, MAGICAL_FOREST_FLOWERS, Feature.RANDOM_PATCH,
					FeatureUtils.simpleRandomPatchConfiguration(32,
							PlacementUtils.filtered(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
											new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
													.add(GLNaturalBlocks.STAR_FLOWER.get().defaultBlockState(), 3)
													.add(GLNaturalBlocks.EUGUNE_RED.get().defaultBlockState(), 2)
													.add(GLNaturalBlocks.EUGUNE_BROWN.get().defaultBlockState(), 2)
													.add(GLNaturalBlocks.EUGUNE_GHOST_FIRE.get().defaultBlockState(), 1)
													.build())),
									BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
											BlockPredicate.matchesTag(Direction.DOWN.getNormal(), BlockTags.DIRT)))));
			FeatureUtils.register(ctx, MAGICAL_FOREST_MUSHROOMS, Feature.RANDOM_PATCH,
					FeatureUtils.simpleRandomPatchConfiguration(32,
							PlacementUtils.filtered(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
											new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
													.add(GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET.cap.get().defaultBlockState(), 2)
													.add(GLNaturalBlocks.DREAM_MUSHROOM_SET.cap.get().defaultBlockState(), 2)
													.add(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap.get().defaultBlockState(), 1)
													.build())),
									BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
											BlockPredicate.matchesTag(Direction.DOWN.getNormal(), BlockTags.DIRT)))));
			var soil = BlockPredicate.matchesBlocks(List.of(Blocks.GRASS_BLOCK, Blocks.DIRT));
			var grassOnly = BlockPredicate.matchesBlocks(Blocks.GRASS_BLOCK);
			FeatureUtils.register(ctx, MAGICAL_FOREST_DISK_COARSE_DIRT, Feature.DISK,
					new DiskConfiguration(RuleBasedBlockStateProvider.simple(Blocks.COARSE_DIRT),
							soil, UniformInt.of(2, 4), 2));
			FeatureUtils.register(ctx, MAGICAL_FOREST_DISK_PODZOL, Feature.DISK,
					new DiskConfiguration(RuleBasedBlockStateProvider.simple(Blocks.PODZOL),
							grassOnly, UniformInt.of(2, 4), 1));
			FeatureUtils.register(ctx, MAGICAL_FOREST_DISK_MYCELIUM, Feature.DISK,
					new DiskConfiguration(RuleBasedBlockStateProvider.simple(Blocks.MYCELIUM),
							grassOnly, UniformInt.of(2, 3), 1));
			FeatureUtils.register(ctx, MAGICAL_FOREST_DISK_MOSS, Feature.DISK,
					new DiskConfiguration(RuleBasedBlockStateProvider.simple(Blocks.MOSS_BLOCK),
							grassOnly, UniformInt.of(2, 4), 1));
			MagicalForestFeatures.configured(ctx);
	});
		init.add(Registries.PLACED_FEATURE, ctx -> {
			var cf = ctx.lookup(Registries.CONFIGURED_FEATURE);
			PlacementUtils.register(ctx, MAGICAL_FOREST_DISK_COARSE_DIRT_PLACED, cf.getOrThrow(MAGICAL_FOREST_DISK_COARSE_DIRT),
					CountPlacement.of(4), InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, BiomeFilter.biome());
			PlacementUtils.register(ctx, MAGICAL_FOREST_DISK_PODZOL_PLACED, cf.getOrThrow(MAGICAL_FOREST_DISK_PODZOL),
					CountPlacement.of(3), InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, BiomeFilter.biome());
			PlacementUtils.register(ctx, MAGICAL_FOREST_DISK_MYCELIUM_PLACED, cf.getOrThrow(MAGICAL_FOREST_DISK_MYCELIUM),
					CountPlacement.of(2), InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, BiomeFilter.biome());
			PlacementUtils.register(ctx, MAGICAL_FOREST_DISK_MOSS_PLACED, cf.getOrThrow(MAGICAL_FOREST_DISK_MOSS),
					CountPlacement.of(3), InSquarePlacement.spread(),
					PlacementUtils.HEIGHTMAP_OCEAN_FLOOR, BiomeFilter.biome());
			MagicalForestFeatures.placed(ctx);
	});
		init.add(Registries.NOISE, MagicalForestFeatures::noise);
	}

	private static ResourceKey<ConfiguredFeature<?, ?>> cf(String id) {
		return ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc(id));
	}

	private static ResourceKey<PlacedFeature> pf(String id) {
		return ResourceKey.create(Registries.PLACED_FEATURE, GensokyoLegacy.loc(id));
	}

}
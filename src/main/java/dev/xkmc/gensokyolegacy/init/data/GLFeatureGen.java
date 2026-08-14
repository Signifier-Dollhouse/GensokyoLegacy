package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.MushroomFeatures.MushroomTreeType;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLDecoBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;

public class GLFeatureGen {
	public static void init(DataProviderInitializer init) {
		init.add(Registries.CONFIGURED_FEATURE, ctx -> {
			for (var type : MushroomTreeType.values()) {
				var set = type.set.get();
				ctx.register(type.cfKey, new ConfiguredFeature<>(
						GLWorldGen.MUSHROOM_TREES.get(type).get(),
						new HugeMushroomFeatureConfiguration(
								BlockStateProvider.simple(set.block.get()),
								BlockStateProvider.simple(set.stem.get()),
								2
						)
				));
			}
			for (var type : TreeType.values()) {
				ctx.register(type.cfKey, type.createConfiguredFeature());
			}
		});
	}
}

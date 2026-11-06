package dev.xkmc.gensokyolegacy.init.data.biome;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.MushroomFeatures.MushroomTreeType;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures.TreeType;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * The configured features that are trees rather than anything a single biome lays down: the mod's own mushrooms and
 * blue firs, which the magical forest's templates place and which a sapling regrows into.
 * <p>
 * Everything else is a biome's business. {@code BiomeGen.build} owns the single {@code CONFIGURED_FEATURE} bootstrap
 * and calls this first, so a biome can put a tree in its own list without this file naming it.
 */
public class VegetationFeatureGen {

	public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
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
	}

}
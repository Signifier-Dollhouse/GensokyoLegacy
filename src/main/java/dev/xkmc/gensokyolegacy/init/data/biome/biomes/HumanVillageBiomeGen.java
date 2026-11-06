package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
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
 * The hot dry cell of the lower ground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the fields around the
 * temple, a little flowered and not tilled.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class HumanVillageBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("human_village");

	private static final ResourceKey<ConfiguredFeature<?, ?>> VILLAGE_FLOWERS = cf("village_flowers");
	private static final ResourceKey<PlacedFeature> VILLAGE_FLOWERS_PF = pf("village_flowers");

	public HumanVillageBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.8f, 0.5f, 0x4f8f7a, 0x8fb7d6, 0x7db356, 0x6f9c4a, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(PLAINS_GROUND, List.of(VILLAGE_FLOWERS_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, VILLAGE_FLOWERS, Feature.RANDOM_PATCH, onDirt(32, new Palette()
				.add(Blocks.CORNFLOWER, 3).add(Blocks.POPPY, 3).add(Blocks.ALLIUM, 2)
				.add(Blocks.DANDELION, 2).add(Blocks.WHITE_TULIP, 2).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.cover(VILLAGE_FLOWERS_PF, reg.feature(VILLAGE_FLOWERS), CountPlacement.of(4));
	}

}
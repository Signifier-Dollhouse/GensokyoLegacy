package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GensokyoBiomeFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.List;

/**
 * The hot humid cell of the mountain band in {@code doc/design/gensokyo_dimension_biome_guide.md}: the untrodden
 * valley, taiga ground and loose stone and nothing else.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class UntroddenValleyBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("untrodden_valley");

	public UntroddenValleyBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.5f, 0.8f, 0x3c6f7d, 0x7d8f9c, 0x6b8560, 0x5d7854, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(TAIGA_GROUND, List.of(VegetationPlacements.PATCH_BERRY_COMMON),
								List.of(GensokyoBiomeFeatures.STONES_PF)))
						.topLayer(FREEZE_TOP_LAYER));
	}

}
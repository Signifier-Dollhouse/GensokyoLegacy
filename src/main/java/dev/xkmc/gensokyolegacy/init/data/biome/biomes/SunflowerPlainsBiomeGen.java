package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.List;

/**
 * The hot humid cell the guide calls the sunflower plains, and the one biome that used to be a verbatim vanilla copy.
 * <p>
 * Its climate, colours and sky are vanilla's own numbers, because a sunflower field is plains and nothing about the
 * flowers changes what the grass looks like. Only its features are written out here. It is also the only biome that
 * spells its sky colour out instead of letting the temperature set it.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class SunflowerPlainsBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("sunflower_plains");

	public SunflowerPlainsBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.8f, 0.4f, 0x3f76e4, 0xc0d8ff, 0x78a7ff, null, null, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(List.of(CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_TALL_GRASS_2,
								VegetationPlacements.PATCH_SUNFLOWER, VegetationPlacements.TREES_PLAINS,
								VegetationPlacements.FLOWER_PLAINS, VegetationPlacements.PATCH_GRASS_PLAIN,
								VegetationPlacements.BROWN_MUSHROOM_NORMAL, VegetationPlacements.RED_MUSHROOM_NORMAL,
								VegetationPlacements.PATCH_SUGAR_CANE, VegetationPlacements.PATCH_PUMPKIN))
						.topLayer(FREEZE_TOP_LAYER));
	}

}
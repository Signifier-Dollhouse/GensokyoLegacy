package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.List;

/**
 * The rock the mountain is hollow, from just under its topsoil down to {@code DEEP_DEPTH}, and nowhere but the
 * mountain. Not one of the fixed locations: a single climate region of its own, keyed on depth and continentalness
 * rather than on the surface.
 * <p>
 * It is overworld rock and not nether rock: stony peaks' ore set, its emerald, its infested stone, its lava lakes and
 * its glow lichen. It was deep dark once, which is where the sculk in the halls came from — nothing here asks for it.
 * No freeze either: the biome is only ever below a surface, so the top-layer step has nothing to act on.
 *
 * @see "doc/design/gensokyo_biomes.md" §5
 * @see "doc/design/gensokyo_dimension.md" §9
 */
public class MountainInteriorBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("mountain_interior");

	public MountainInteriorBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.6f, 0.8f, 0x2c3a50, 0x5a6a72, null, null, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(List.of(CavePlacements.GLOW_LICHEN)));
	}

}
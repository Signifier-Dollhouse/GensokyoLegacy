package dev.xkmc.gensokyolegacy.init.data.biome;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.MagicalForestBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.MyourenTempleBarrensBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.SakuraForestBiomeGen;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.ParameterUtils;
import terrablender.api.Region;
import terrablender.api.RegionType;
import terrablender.api.VanillaParameterOverlayBuilder;

import java.util.function.Consumer;

/**
 * Overworld region for the magical forest.
 *
 * @see "doc/design/biome/overworld_region.md"
 */
public class GLOverworldRegion extends Region {

	private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);
	// Depth reads 0 at the column surface, negative above it and one unit per 128 blocks below, so this
	// covers the air and the shallow subsurface and leaves the caves to vanilla.
	private static final Climate.Parameter DEPTH_RANGE = Climate.Parameter.span(-1.0F, 0.3F);
	private static final float FALLBACK_OFFSET = 0.01F;

	public GLOverworldRegion(int weight) {
		super(new ResourceLocation(GensokyoLegacy.MODID, "magical_forest"), RegionType.OVERWORLD, weight);
	}

	@Override
	public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		var builder = new VanillaParameterOverlayBuilder();
		start(builder, 0, 1, -1, -0.375f, SakuraForestBiomeGen.KEY);
		start(builder, 0, 1, -0.375f, 0.05f, MagicalForestBiomeGen.KEY);
		start(builder, -1, 0, -1, -0.375f, MyourenTempleBarrensBiomeGen.KEY);
		builder.build().forEach(mapper);
		mapper.accept(Pair.of(Climate.parameters(FULL_RANGE, FULL_RANGE, FULL_RANGE, FULL_RANGE,
				FULL_RANGE, FULL_RANGE, FALLBACK_OFFSET), Region.DEFERRED_PLACEHOLDER));
	}

	private static void start(VanillaParameterOverlayBuilder builder, float t0, float t1, float e0, float e1, ResourceKey<Biome> biome) {
		new ParameterUtils.ParameterPointListBuilder()
				.humidity(ParameterUtils.Humidity.FULL_RANGE)
				.continentalness(ParameterUtils.Continentalness.INLAND, ParameterUtils.Continentalness.FAR_INLAND)
				.depth(DEPTH_RANGE)
				.weirdness(ParameterUtils.Weirdness.FULL_RANGE)
				.temperature(Climate.Parameter.span(t0, t1))
				.erosion(Climate.Parameter.span(e0, e1))
				.build()
				.forEach(point -> builder.add(point, biome));
	}

}

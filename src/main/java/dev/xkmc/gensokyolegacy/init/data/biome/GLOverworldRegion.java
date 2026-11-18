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
		super(ResourceLocation.fromNamespaceAndPath(GensokyoLegacy.MODID, "magical_forest"), RegionType.OVERWORLD, weight);
	}

	@Override
	public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		var builder = new VanillaParameterOverlayBuilder();
		// Flat areas (low erosion) become sakura forest, leaving even ground for structures
		new ParameterUtils.ParameterPointListBuilder()
				.temperature(ParameterUtils.Temperature.NEUTRAL, ParameterUtils.Temperature.COOL,
						ParameterUtils.Temperature.WARM)
				.humidity(ParameterUtils.Humidity.NEUTRAL, ParameterUtils.Humidity.WET,
						ParameterUtils.Humidity.HUMID)
				.continentalness(ParameterUtils.Continentalness.INLAND, ParameterUtils.Continentalness.FAR_INLAND)
				.erosion(ParameterUtils.Erosion.EROSION_0, ParameterUtils.Erosion.EROSION_1)
				.depth(DEPTH_RANGE)
				.weirdness(ParameterUtils.Weirdness.MID_SLICE_NORMAL_ASCENDING, ParameterUtils.Weirdness.MID_SLICE_NORMAL_DESCENDING,
						ParameterUtils.Weirdness.HIGH_SLICE_NORMAL_ASCENDING, ParameterUtils.Weirdness.HIGH_SLICE_NORMAL_DESCENDING)
				.build()
				.forEach(point -> builder.add(point, SakuraForestBiomeGen.KEY));
		new ParameterUtils.ParameterPointListBuilder()
				.temperature(ParameterUtils.Temperature.NEUTRAL, ParameterUtils.Temperature.COOL,
						ParameterUtils.Temperature.WARM)
				.humidity(ParameterUtils.Humidity.NEUTRAL, ParameterUtils.Humidity.WET,
						ParameterUtils.Humidity.HUMID)
				.continentalness(ParameterUtils.Continentalness.INLAND, ParameterUtils.Continentalness.FAR_INLAND)
				.erosion(ParameterUtils.Erosion.EROSION_2, ParameterUtils.Erosion.EROSION_3)
				.depth(DEPTH_RANGE)
				.weirdness(ParameterUtils.Weirdness.MID_SLICE_NORMAL_ASCENDING, ParameterUtils.Weirdness.MID_SLICE_NORMAL_DESCENDING,
						ParameterUtils.Weirdness.HIGH_SLICE_NORMAL_ASCENDING, ParameterUtils.Weirdness.HIGH_SLICE_NORMAL_DESCENDING)
				.build()
				.forEach(point -> builder.add(point, MagicalForestBiomeGen.KEY));
		new ParameterUtils.ParameterPointListBuilder()
				.temperature(ParameterUtils.Temperature.ICY, ParameterUtils.Temperature.COOL)
				.humidity(ParameterUtils.Humidity.NEUTRAL, ParameterUtils.Humidity.DRY, ParameterUtils.Humidity.ARID)
				.continentalness(ParameterUtils.Continentalness.INLAND, ParameterUtils.Continentalness.FAR_INLAND)
				.erosion(ParameterUtils.Erosion.EROSION_0, ParameterUtils.Erosion.EROSION_1)
				.depth(DEPTH_RANGE)
				.weirdness(ParameterUtils.Weirdness.MID_SLICE_NORMAL_ASCENDING, ParameterUtils.Weirdness.MID_SLICE_NORMAL_DESCENDING,
						ParameterUtils.Weirdness.HIGH_SLICE_NORMAL_ASCENDING, ParameterUtils.Weirdness.HIGH_SLICE_NORMAL_DESCENDING)
				.build()
				.forEach(point -> builder.add(point, MyourenTempleBarrensBiomeGen.KEY));
		builder.build().forEach(mapper);
		// Fitness adds offset^2 and the target's offset is always 0, so this leaf wins wherever the nearest
		// declared point is farther than FALLBACK_OFFSET, and falls back to vanilla there.
		mapper.accept(Pair.of(Climate.parameters(FULL_RANGE, FULL_RANGE, FULL_RANGE, FULL_RANGE,
				FULL_RANGE, FULL_RANGE, FALLBACK_OFFSET), Region.DEFERRED_PLACEHOLDER));
	}

}

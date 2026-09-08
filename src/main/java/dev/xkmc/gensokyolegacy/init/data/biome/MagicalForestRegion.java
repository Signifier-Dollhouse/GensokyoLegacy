package dev.xkmc.gensokyolegacy.init.data.biome;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
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

public class MagicalForestRegion extends Region {

	public MagicalForestRegion() {
		super(ResourceLocation.fromNamespaceAndPath(GensokyoLegacy.MODID, "magical_forest"), RegionType.OVERWORLD, 3);
	}

	@Override
	public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
		var builder = new VanillaParameterOverlayBuilder();
		new ParameterUtils.ParameterPointListBuilder()
				.temperature(ParameterUtils.Temperature.NEUTRAL, ParameterUtils.Temperature.COOL)
				.humidity(ParameterUtils.Humidity.NEUTRAL, ParameterUtils.Humidity.WET)
				.continentalness(ParameterUtils.Continentalness.INLAND, ParameterUtils.Continentalness.FAR_INLAND)
				.erosion(ParameterUtils.Erosion.EROSION_0, ParameterUtils.Erosion.EROSION_2)
				.depth(ParameterUtils.Depth.SURFACE, ParameterUtils.Depth.FLOOR)
				.weirdness(ParameterUtils.Weirdness.MID_SLICE_NORMAL_ASCENDING, ParameterUtils.Weirdness.HIGH_SLICE_NORMAL_ASCENDING)
				.build()
				.forEach(point -> builder.add(point, GLBiomes.MAGICAL_FOREST));
		builder.build().forEach(mapper);
	}

}

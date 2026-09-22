package dev.xkmc.gensokyolegacy.content.worldgen.feature.template;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

import java.util.List;
import java.util.stream.Stream;

/**
 * @param templates       weighted structure templates. Convention (enforced by tools/template_export):
 *                        the anchor (trunk / stem base) is the centre of the bottom face, layer 0 is
 *                        the first layer above ground, no air blocks, reach from the anchor &lt;= 15
 * @param processors      extra processors, run after the built-in {@link TemplateBlendProcessor}
 * @param ground          blocks the anchor may stand on
 * @param footprintRadius distance of the 8 terrain samples around the anchor
 * @param maxSlope        max terrain height difference among the samples, all must be dry
 * @param yOffset         vertical shift of layer 0 relative to the first block above ground
 * @param rootDepth       how far trunk blocks of layer 0 are extended downwards to reach the ground
 * @param postFeatures    placed at the trunk base after the template (ground patches, companions)
 */
public record TemplateFeatureConfig(
		SimpleWeightedRandomList<ResourceLocation> templates,
		Holder<StructureProcessorList> processors,
		TagKey<Block> ground,
		int footprintRadius,
		int maxSlope,
		int yOffset,
		int rootDepth,
		List<Holder<PlacedFeature>> postFeatures
) implements FeatureConfiguration {

	public static final Holder<StructureProcessorList> NO_PROCESSORS = Holder.direct(new StructureProcessorList(List.of()));

	public static final Codec<TemplateFeatureConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
			SimpleWeightedRandomList.wrappedCodec(ResourceLocation.CODEC).fieldOf("templates").forGetter(TemplateFeatureConfig::templates),
			StructureProcessorType.LIST_CODEC.optionalFieldOf("processors", NO_PROCESSORS).forGetter(TemplateFeatureConfig::processors),
			TagKey.hashedCodec(Registries.BLOCK).fieldOf("ground").forGetter(TemplateFeatureConfig::ground),
			Codec.intRange(0, 8).fieldOf("footprint_radius").forGetter(TemplateFeatureConfig::footprintRadius),
			Codec.intRange(0, 16).fieldOf("max_slope").forGetter(TemplateFeatureConfig::maxSlope),
			Codec.intRange(-8, 8).optionalFieldOf("y_offset", 0).forGetter(TemplateFeatureConfig::yOffset),
			Codec.intRange(0, 16).fieldOf("root_depth").forGetter(TemplateFeatureConfig::rootDepth),
			PlacedFeature.CODEC.listOf().optionalFieldOf("post_features", List.of()).forGetter(TemplateFeatureConfig::postFeatures)
	).apply(i, TemplateFeatureConfig::new));

	@Override
	public Stream<ConfiguredFeature<?, ?>> getFeatures() {
		return postFeatures.stream().flatMap(e -> e.value().getFeatures());
	}

}

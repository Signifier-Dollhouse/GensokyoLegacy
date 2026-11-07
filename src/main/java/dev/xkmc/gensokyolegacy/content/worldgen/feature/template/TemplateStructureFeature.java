package dev.xkmc.gensokyolegacy.content.worldgen.feature.template;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

/**
 * {@link TemplateFeature} for built structures: the same terrain checks and random rotation, but every block is placed
 * as authored instead of being blended into vegetation.
 */
public class TemplateStructureFeature extends TemplateFeature {

	public TemplateStructureFeature(Codec<TemplateFeatureConfig> codec) {
		super(codec);
	}

	@Override
	protected StructureProcessor createProcessor(TemplateFeatureConfig config) {
		return new TemplateStructureProcessor();
	}

}

package dev.xkmc.gensokyolegacy.content.worldgen.feature.template;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import java.util.Map;

/**
 * Replaces template blocks while retaining shared state properties.
 */
public class TemplateReplaceProcessor extends StructureProcessor {

	public static final Codec<TemplateReplaceProcessor> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(BuiltInRegistries.BLOCK.byNameCodec(), BuiltInRegistries.BLOCK.byNameCodec())
					.fieldOf("replacements").forGetter(e -> e.replacements)
	).apply(i, TemplateReplaceProcessor::new));

	private final Map<Block, Block> replacements;

	public TemplateReplaceProcessor(Map<Block, Block> replacements) {
		this.replacements = replacements;
	}

	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
			StructureBlockInfo original, StructureBlockInfo current, StructurePlaceSettings settings) {
		Block replacement = replacements.get(current.state().getBlock());
		if (replacement == null) return current;
		return new StructureBlockInfo(current.pos(), replacement.withPropertiesOf(current.state()), current.nbt());
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return GLWorldGen.TEMPLATE_REPLACE.get();
	}

}

package dev.xkmc.gensokyolegacy.content.worldgen.feature.template;

import com.mojang.serialization.MapCodec;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import javax.annotation.Nullable;

/**
 * Places a structure template over air and vegetation, with none of the support rules a vegetation template needs.
 */
public class TemplateStructureProcessor extends StructureProcessor {

	public static final MapCodec<TemplateStructureProcessor> CODEC = MapCodec.unit(new TemplateStructureProcessor());

	@Nullable
	@Override
	public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
			StructureBlockInfo blockInfo, StructureBlockInfo rel, StructurePlaceSettings settings) {
		BlockState world = level.getBlockState(rel.pos());
		if (world.is(BlockTags.FEATURES_CANNOT_REPLACE)) return null;
		return TemplateBlendProcessor.isSoft(world) ? rel : null;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return GLWorldGen.TEMPLATE_STRUCTURE_PROCESSOR.get();
	}

}

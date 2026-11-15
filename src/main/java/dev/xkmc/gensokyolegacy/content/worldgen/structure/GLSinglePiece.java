package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.Optional;

/**
 * Datagen-only single element with fixed liquid settings. It serialises as {@code minecraft:single_pool_element}, so
 * a decoded pool replaces it with a plain {@link SinglePoolElement}.
 */
public class GLSinglePiece extends SinglePoolElement {

	public GLSinglePiece(ResourceLocation template, Holder<StructureProcessorList> list, StructureTemplatePool.Projection proj) {
		super(Either.left(template), list, proj, Optional.of(LiquidSettings.IGNORE_WATERLOGGING));
	}

}

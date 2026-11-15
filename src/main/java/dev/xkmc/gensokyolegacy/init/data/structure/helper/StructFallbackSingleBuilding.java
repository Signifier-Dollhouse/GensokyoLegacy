package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.FallbackStructure;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.GLSinglePiece;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain.TerrainPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;
import java.util.Map;

/**
 * {@link FallbackStructure} with a single template, mirroring {@link StructFlatBuilding}. The template id is separate
 * from the structure id so a biome template can stay under {@code biome/...}.
 *
 * @see "doc/design/structure/fallback.md"
 */
public record StructFallbackSingleBuilding(
		ResourceLocation template,
		List<StructureProcessor> processors,
		Map<MobCategory, StructureSpawnOverride> spawns,
		TerrainPredicate terrain,
		int maxDistanceFromCenter
) implements StructBuilding {

	@Override
	public Role role() {
		return Role.TINY;
	}

	@Override
	public void registerTemplatePools(BootstrapContext<StructureTemplatePool> ctx, ResourceLocation id) {
		var empty = ctx.lookup(Registries.TEMPLATE_POOL)
				.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace("empty")));
		var list = ctx.lookup(Registries.PROCESSOR_LIST)
				.getOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, id));
		ctx.register(ResourceKey.create(Registries.TEMPLATE_POOL, id), new StructureTemplatePool(empty, List.of(
				Pair.of(new GLSinglePiece(template, list, StructureTemplatePool.Projection.RIGID), 1)
		)));
	}

	@Override
	public void registerProcessors(BootstrapContext<StructureProcessorList> ctx, ResourceLocation id) {
		ctx.register(ResourceKey.create(Registries.PROCESSOR_LIST, id),
				new StructureProcessorList(processors()));
	}

	@Override
	public void registerStructure(BootstrapContext<Structure> ctx, ResourceLocation id, HolderSet.Named<Biome> biome, SetContext set) {
		var pool = ctx.lookup(Registries.TEMPLATE_POOL)
				.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, id));
		ctx.register(ResourceKey.create(Registries.STRUCTURE, id), new FallbackStructure(
				new Structure.StructureSettings(biome, spawns(), GenerationStep.Decoration.UNDERGROUND_DECORATION,
						terrain().needsBeardifier() ? TerrainAdjustment.NONE : TerrainAdjustment.BEARD_THIN),
				pool, 1, false, maxDistanceFromCenter(), terrain(),
				StructBuilding.setRef(set)
		));
	}

}

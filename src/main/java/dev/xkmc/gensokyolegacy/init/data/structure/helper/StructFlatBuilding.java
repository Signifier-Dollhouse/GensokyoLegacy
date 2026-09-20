package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.FlatCheckStructure;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.GLSinglePiece;
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
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;
import java.util.Map;

public record StructFlatBuilding(
		List<StructureProcessor> processors,
		Map<MobCategory, StructureSpawnOverride> spawns,
		int heightTolerance,
		int maxDistanceFromCenter,
		int attempts,
		int spacing,
		int safetyRadius
) implements StructBuilding {

	@Override
	public void registerTemplatePools(BootstrapContext<StructureTemplatePool> ctx, ResourceLocation id) {
		var empty = ctx.lookup(Registries.TEMPLATE_POOL)
				.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace("empty")));
		var list = ctx.lookup(Registries.PROCESSOR_LIST)
				.getOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, id));
		ctx.register(ResourceKey.create(Registries.TEMPLATE_POOL, id), new StructureTemplatePool(empty, List.of(
				Pair.of(new GLSinglePiece(id, list, StructureTemplatePool.Projection.RIGID), 1)
		)));
	}

	@Override
	public void registerProcessors(BootstrapContext<StructureProcessorList> ctx, ResourceLocation id) {
		ctx.register(ResourceKey.create(Registries.PROCESSOR_LIST, id),
				new StructureProcessorList(processors()));
	}

	@Override
	public void registerStructure(BootstrapContext<Structure> ctx, ResourceLocation id, HolderSet.Named<Biome> biome) {
		var pool = ctx.lookup(Registries.TEMPLATE_POOL)
				.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, id));
		ctx.register(ResourceKey.create(Registries.STRUCTURE, id), new FlatCheckStructure(
				new Structure.StructureSettings(biome, spawns(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN),
				pool, 1, false, maxDistanceFromCenter(), heightTolerance(),
				attempts(), spacing(), RandomSpreadType.LINEAR, StructStructure.saltFor(id), safetyRadius()
		));
	}

}

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

/**
 * Flat-check structure with multiple jigsaw pools.
 * Registers one template pool per part at {@code <id>/<part>} and starts
 * generation from the first part (root). Rigid parts use RIGID projection,
 * flexible parts (hakurei paths) use TERRAIN_MATCHING.
 */
public record StructFlatJigsawBuilding(
		int maxDepth,
		List<Part> parts,
		Map<MobCategory, StructureSpawnOverride> spawns,
		int heightTolerance,
		int maxDistanceFromCenter,
		int attempts,
		int spacing,
		int safetyRadius
) implements StructBuilding {

	public record Part(
			String id, boolean rigid,
			List<StructureProcessor> processors
	) {

	}

	@Override
	public void registerTemplatePools(BootstrapContext<StructureTemplatePool> ctx, ResourceLocation id) {
		for (var e : parts) {
			var pid = id.withSuffix("/" + e.id);
			var empty = ctx.lookup(Registries.TEMPLATE_POOL)
					.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace("empty")));
			var list = ctx.lookup(Registries.PROCESSOR_LIST)
					.getOrThrow(ResourceKey.create(Registries.PROCESSOR_LIST, pid));
			var proj = e.rigid() ? StructureTemplatePool.Projection.RIGID : StructureTemplatePool.Projection.TERRAIN_MATCHING;
			ctx.register(ResourceKey.create(Registries.TEMPLATE_POOL, pid), new StructureTemplatePool(empty, List.of(
					Pair.of(new GLSinglePiece(pid, list, proj), 1)
			)));
		}
	}

	@Override
	public void registerProcessors(BootstrapContext<StructureProcessorList> ctx, ResourceLocation id) {
		for (var e : parts) {
			var pid = id.withSuffix("/" + e.id);
			ctx.register(ResourceKey.create(Registries.PROCESSOR_LIST, pid),
					new StructureProcessorList(e.processors()));
		}
	}

	@Override
	public void registerStructure(BootstrapContext<Structure> ctx, ResourceLocation id, HolderSet.Named<Biome> biome) {
		var pool = ctx.lookup(Registries.TEMPLATE_POOL)
				.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, id.withSuffix("/" + parts().getFirst().id())));
		ctx.register(ResourceKey.create(Registries.STRUCTURE, id), new FlatCheckStructure(
				new Structure.StructureSettings(biome, spawns(), GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.BEARD_THIN),
				pool, maxDepth(), false, maxDistanceFromCenter(), heightTolerance(),
				attempts(), spacing(), RandomSpreadType.LINEAR, StructStructure.saltFor(id), safetyRadius()
		));
	}

}

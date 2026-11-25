package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.FallbackStructure;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.GLSinglePiece;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
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
 * {@link FallbackStructure} with multiple jigsaw pools, mirroring {@link StructFlatJigsawBuilding}: one template pool
 * per part at {@code <id>/<part>}, generation starting from the first part. A fallback takes no set index, so it is
 * skipped by the region pick.
 */
public record StructFallbackBuilding(
		int maxDepth,
		List<StructFlatJigsawBuilding.Part> parts,
		Map<MobCategory, StructureSpawnOverride> spawns,
		int heightTolerance,
		int maxDistanceFromCenter
) implements StructBuilding {

	@Override
	public Role role() {
		return Role.TINY;
	}

	@Override
	public void registerTemplatePools(BootstapContext<StructureTemplatePool> ctx, ResourceLocation id) {
		for (var e : parts) {
			var pid = id.withSuffix("/" + e.id());
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
	public void registerProcessors(BootstapContext<StructureProcessorList> ctx, ResourceLocation id) {
		for (var e : parts) {
			var pid = id.withSuffix("/" + e.id());
			ctx.register(ResourceKey.create(Registries.PROCESSOR_LIST, pid),
					new StructureProcessorList(e.processors()));
		}
	}

	@Override
	public void registerStructure(BootstapContext<Structure> ctx, ResourceLocation id, HolderSet.Named<Biome> biome, SetContext set) {
		var pool = ctx.lookup(Registries.TEMPLATE_POOL)
				.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, id.withSuffix("/" + parts().get(0).id())));
		ctx.register(ResourceKey.create(Registries.STRUCTURE, id), new FallbackStructure(
				new Structure.StructureSettings(biome, spawns(), GenerationStep.Decoration.UNDERGROUND_DECORATION, TerrainAdjustment.BEARD_THIN),
				pool, maxDepth(), false, maxDistanceFromCenter(), heightTolerance(),
				StructBuilding.setRef(set)
		));
	}

}

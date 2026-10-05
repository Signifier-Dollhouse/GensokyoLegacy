package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

import java.util.LinkedHashMap;
import java.util.List;

public record StructStructure(
		ResourceLocation id, TagKey<Biome> biomes, int spacing, int attempts,
		StructureConfig.Builder config,
		List<StructBed> beds,
		// Characters who do not live here but may turn up for a few minutes
		// during the day. Authored here rather than per bed, because the pool
		// belongs to the structure, not to its residents.
		LinkedHashMap<EntityType<?>, StructureConfig.CharacterVisit> visits,
		StructBuilding building,
		// Structures sharing one set id are placed by a single StructureSet:
		// every region randomly picks exactly one member (FlatCheckStructure
		// setIndex/setCount gate), so members of a group can never spawn too
		// close to each other. All members of a group must use the same
		// spacing/attempts and equal weights, and share the set salt. setCount
		// may exceed the registered member count: unassigned indices generate
		// nothing and reserve room for future members without shifting picks.
		ResourceLocation set) {

	public StructStructure(
			ResourceLocation id, TagKey<Biome> biomes, int spacing, int attempts,
			StructureConfig.Builder config,
			List<StructBed> beds,
			StructBuilding building) {
		this(id, biomes, spacing, attempts, config, beds, new LinkedHashMap<>(), building, id);
	}

	public StructStructure(
			ResourceLocation id, TagKey<Biome> biomes, int spacing, int attempts,
			StructureConfig.Builder config,
			List<StructBed> beds,
			StructBuilding building,
			ResourceLocation set) {
		this(id, biomes, spacing, attempts, config, beds, new LinkedHashMap<>(), building, set);
	}

	public static int saltFor(ResourceLocation id) {
		return id.hashCode() & 0x7fffffff;
	}

	public int salt() {
		return saltFor(set());
	}

}

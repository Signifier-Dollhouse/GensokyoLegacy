package dev.xkmc.gensokyolegacy.init.data.structure;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public record StructStructure(
		ResourceLocation id, TagKey<Biome> biomes, int spacing, int attempts,
		StructureConfig.Builder config,
		List<StructBed> beds,
		StructBuilding building) {

	public static int saltFor(ResourceLocation id) {
		return id.hashCode() & 0x7fffffff;
	}

	public int salt() {
		return saltFor(id());
	}

}

package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import dev.xkmc.l2core.init.reg.ench.DataGenHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public interface StructBuilding {

	void registerTemplatePools(BootstrapContext<StructureTemplatePool> ctx, ResourceLocation id);

	void registerProcessors(BootstrapContext<StructureProcessorList> ctx, ResourceLocation id);

	void registerStructure(BootstrapContext<Structure> ctx, ResourceLocation id, HolderSet.Named<Biome> biome, SetContext set);

	/**
	 * Reference to the set placing the structure. Datagen has no structure-set registry to bind against and never reads
	 * the value, so this hands out a {@link DataGenHolder} carrying the id alone; worldgen reads the real one from the
	 * registry.
	 */
	static Holder<StructureSet> setRef(SetContext set) {
		return new DataGenHolder<>(ResourceKey.create(Registries.STRUCTURE_SET, set.set()), null);
	}

}

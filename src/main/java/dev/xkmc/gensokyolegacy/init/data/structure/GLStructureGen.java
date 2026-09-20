package dev.xkmc.gensokyolegacy.init.data.structure;

import com.tterrag.registrate.providers.DataProviderInitializer;
import com.tterrag.registrate.providers.RegistrateDataMapProvider;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.BedData;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.CharacterConfig;
import dev.xkmc.gensokyolegacy.init.data.structure.helper.StructBed;
import dev.xkmc.gensokyolegacy.init.data.structure.helper.StructFlatBuilding;
import dev.xkmc.gensokyolegacy.init.data.structure.helper.StructFlatJigsawBuilding;
import dev.xkmc.gensokyolegacy.init.data.structure.helper.StructStructure;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.MultiSpreadPlacement;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.neoforged.neoforge.common.util.Lazy;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class GLStructureGen {

	private static List<StructStructure> initStructures() {
		return List.of(
				// Marisa's house in the modded magical forest (template 30x13x25, bed at local (4,6,16)-(5,6,16))
				new StructStructure(
						GensokyoLegacy.loc("marisa_house"), GLStructureTagGen.MARISA_HOUSE, 32, 24,
						StructureConfigBuilder.marisa(),
						List.of(new StructBed(
								GLEntities.MARISA,
								CharacterConfig.forStructure(6000, 12000, 12, 30),
								GLBlocks.Beds.MARISA.holder()
						)),
						new StructFlatBuilding(List.of(), Map.of(), 5, 60, 24, 32, 8)
				),
			// Hakurei shrine jigsaw in the modded sakura forest (root 19x14x19, bed at local (5,2,12)-(5,2,13))
			new StructStructure(
					GensokyoLegacy.loc("hakurei_shrine"), GLStructureTagGen.HAKUREI_SHRINE, 32, 24,
					StructureConfigBuilder.hakurei(),
					List.of(new StructBed(
							GLEntities.REIMU,
							CharacterConfig.forStructure(6000, 12000, 16, 30),
							GLBlocks.Beds.REIMU.holder()
					)),
					new StructFlatJigsawBuilding(6, List.of(
							new StructFlatJigsawBuilding.Part("root", true, List.of()),
							new StructFlatJigsawBuilding.Part("road", true, List.of()),
							new StructFlatJigsawBuilding.Part("gate", true, List.of()),
							new StructFlatJigsawBuilding.Part("warehouse", true, List.of()),
							new StructFlatJigsawBuilding.Part("path0", false, List.of()),
							new StructFlatJigsawBuilding.Part("path1", false, List.of()),
							new StructFlatJigsawBuilding.Part("path2", false, List.of()),
							new StructFlatJigsawBuilding.Part("path3", false, List.of()),
							new StructFlatJigsawBuilding.Part("path4", false, List.of()),
							new StructFlatJigsawBuilding.Part("path5", false, List.of()),
							new StructFlatJigsawBuilding.Part("stone", true, List.of()),
							new StructFlatJigsawBuilding.Part("tree0", true, List.of()),
							new StructFlatJigsawBuilding.Part("tree1", true, List.of()),
							new StructFlatJigsawBuilding.Part("tree2", true, List.of()),
							new StructFlatJigsawBuilding.Part("tree2_top", true, List.of()),
							new StructFlatJigsawBuilding.Part("tree3", true, List.of())
					), Map.of(), 5, 80, 24, 32, 8)
			),
				// Kourindou (Morichika's shop) on vanilla plains (template 33x18x33, bed at local (27,8,16)-(27,8,17))
				new StructStructure(
						GensokyoLegacy.loc("morichika_shop"), GLStructureTagGen.MORICHIKA_SHOP, 32, 24,
						StructureConfigBuilder.morichika(),
						List.of(new StructBed(
								GLEntities.MORICHIKA,
								CharacterConfig.forStructure(6000, 12000, 12, 30),
								GLBlocks.Beds.MORICHIKA.holder()
						)),
						new StructFlatBuilding(List.of(), Map.of(), 5, 64, 24, 32, 8)
				)
		);
	}

	private static final Supplier<List<StructStructure>> STRUCTURES = Lazy.of(GLStructureGen::initStructures);

	public static void dataMap(RegistrateDataMapProvider pvd) {
		var bedReg = pvd.builder(GLMeta.BED_DATA.reg());
		var entityReg = pvd.builder(GLMeta.ENTITY_DATA.reg());
		var structureReg = pvd.builder(GLMeta.STRUCTURE_DATA.reg());
		for (var e : STRUCTURES.get()) {
			var config = e.config();
			for (var bedData : e.beds()) {
				for (var bed : bedData.bed())
					bedReg.add(bed, new BedData(bedData.entity().value()), false);
				entityReg.add(bedData.entity(), bedData.data().withId(e.id()), false);
				config.addEntity(bedData.entity().value());
			}
			structureReg.add(e.id(), config.build(), false);
		}

		bedReg.add(GLBlocks.BEDS[GLBlocks.Beds.CIRNO.ordinal()], new BedData(GLEntities.CIRNO.get()), false);
		bedReg.add(GLBlocks.BEDS[GLBlocks.Beds.RUMIA.ordinal()], new BedData(GLEntities.RUMIA.get()), false);
	}

	public static void init(DataProviderInitializer init) {
		init.add(Registries.PROCESSOR_LIST, ctx -> {
			for (var e : STRUCTURES.get()) {
				e.building().registerProcessors(ctx, e.id());
			}
		});
		init.add(Registries.TEMPLATE_POOL, ctx -> {
			for (var e : STRUCTURES.get()) {
				e.building().registerTemplatePools(ctx, e.id());
			}
		});
		init.add(Registries.STRUCTURE, ctx -> {
			for (var e : STRUCTURES.get()) {
				var biome = ctx.lookup(Registries.BIOME).getOrThrow(e.biomes());
				e.building().registerStructure(ctx, e.id(), biome);
			}
		});
		init.add(Registries.STRUCTURE_SET, ctx -> {
			for (var e : STRUCTURES.get()) {
				var str = ctx.lookup(Registries.STRUCTURE).getOrThrow(ResourceKey.create(Registries.STRUCTURE, e.id()));
				ctx.register(ResourceKey.create(Registries.STRUCTURE_SET, e.id()), new StructureSet(
						str, new MultiSpreadPlacement(e.spacing(), RandomSpreadType.LINEAR, e.salt(), e.attempts())));
			}
		});
	}

}

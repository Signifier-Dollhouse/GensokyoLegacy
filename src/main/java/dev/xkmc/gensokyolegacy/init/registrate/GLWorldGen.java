package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.content.dimension.EmptyChunkGenerator;
import dev.xkmc.gensokyolegacy.content.dimension.GensokyoChunkGenerator;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.DoubleBlockFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.MudWaterloggedVegetationPatchFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.MushroomFeatures;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.dripleaf.DripleafScatterConfig;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.dripleaf.DripleafScatterFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.lake.LakeConfig;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.lake.LakeFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.ravine.RavineCaveConfig;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.ravine.RavineCaveFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.stair.StairPoolConfig;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.stair.StairPoolFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.template.TemplateBlendProcessor;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.template.TemplateFeature;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.template.TemplateFeatureConfig;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.AvoidStructuresFilter;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.CanopyFilter;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.GridExclusionFilter;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.JitteredGridPlacement;
import dev.xkmc.gensokyolegacy.content.worldgen.placement.NoiseBandFilter;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.FlatCheckStructure;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.MultiSpreadPlacement;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.SetDataProcessor;
import dev.xkmc.l2core.init.reg.simple.CdcReg;
import dev.xkmc.l2core.init.reg.simple.CdcVal;
import dev.xkmc.l2core.init.reg.simple.SR;
import dev.xkmc.l2core.init.reg.simple.Val;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class GLWorldGen {

	private static final SR<StructureProcessorType<?>> PROCESSORS = SR.of(GensokyoLegacy.REG, Registries.STRUCTURE_PROCESSOR);
	public static final Val<StructureProcessorType<SetDataProcessor>> SET_DATA = PROCESSORS.reg("set_data", () -> () -> SetDataProcessor.CODEC);
	public static final Val<StructureProcessorType<TemplateBlendProcessor>> TEMPLATE_BLEND = PROCESSORS.reg("template_blend", () -> () -> TemplateBlendProcessor.CODEC);

	private static final SR<StructureType<?>> STRUCTURES = SR.of(GensokyoLegacy.REG, Registries.STRUCTURE_TYPE);
	public static final Val<StructureType<FlatCheckStructure>> FLAT = STRUCTURES.reg("flat_check", () -> () -> FlatCheckStructure.CODEC);

	private static final SR<StructurePlacementType<?>> PLACEMENTS = SR.of(GensokyoLegacy.REG, Registries.STRUCTURE_PLACEMENT);
	public static final Val<StructurePlacementType<MultiSpreadPlacement>> MULTI_SPREAD = PLACEMENTS.reg("multi_spread", () -> () -> MultiSpreadPlacement.CODEC);

	private static final CdcReg<ChunkGenerator> CG = CdcReg.of(GensokyoLegacy.REG, BuiltInRegistries.CHUNK_GENERATOR);
	public static final CdcVal<EmptyChunkGenerator> CG_GAP = CG.reg("gap", EmptyChunkGenerator.CODEC);
	public static final CdcVal<GensokyoChunkGenerator> CG_GENSOKYO = CG.reg("gensokyo", GensokyoChunkGenerator.CODEC);


	private static final SR<Feature<?>> FR = SR.of(GensokyoLegacy.REG, BuiltInRegistries.FEATURE);
	public static final Map<MushroomFeatures.MushroomTreeType, Val<AbstractHugeMushroomFeature>> MUSHROOM_TREES;
	public static final Val<TemplateFeature> TEMPLATE = FR.reg("template", () -> new TemplateFeature(TemplateFeatureConfig.CODEC));
	public static final Val<LakeFeature> LAKE = FR.reg("lake", () -> new LakeFeature(LakeConfig.CODEC));
	public static final Val<DoubleBlockFeature> DOUBLE_BLOCK =
			FR.reg("double_block", () -> new DoubleBlockFeature(SimpleBlockConfiguration.CODEC));
	public static final Val<MudWaterloggedVegetationPatchFeature> MUD_WATERLOGGED_VEGETATION_PATCH =
			FR.reg("mud_waterlogged_vegetation_patch", () -> new MudWaterloggedVegetationPatchFeature(VegetationPatchConfiguration.CODEC));
	public static final Val<StairPoolFeature> STAIR_POOL =
			FR.reg("stair_pool", () -> new StairPoolFeature(StairPoolConfig.CODEC));
	public static final Val<DripleafScatterFeature> DRIPLEAF_SCATTER =
			FR.reg("dripleaf_scatter", () -> new DripleafScatterFeature(DripleafScatterConfig.CODEC));
	public static final Val<RavineCaveFeature> RAVINE_CAVE =
			FR.reg("ravine_cave", () -> new RavineCaveFeature(RavineCaveConfig.CODEC));

	private static final SR<PlacementModifierType<?>> PM = SR.of(GensokyoLegacy.REG, Registries.PLACEMENT_MODIFIER_TYPE);
	public static final Val<PlacementModifierType<JitteredGridPlacement>> JITTERED_GRID = PM.reg("jittered_grid", () -> () -> JitteredGridPlacement.CODEC);
	public static final Val<PlacementModifierType<GridExclusionFilter>> GRID_EXCLUSION = PM.reg("grid_exclusion", () -> () -> GridExclusionFilter.CODEC);
	public static final Val<PlacementModifierType<NoiseBandFilter>> NOISE_BAND = PM.reg("noise_band", () -> () -> NoiseBandFilter.CODEC);
	public static final Val<PlacementModifierType<AvoidStructuresFilter>> AVOID_STRUCTURES = PM.reg("avoid_structures", () -> () -> AvoidStructuresFilter.CODEC);
	public static final Val<PlacementModifierType<CanopyFilter>> CANOPY = PM.reg("canopy", () -> () -> CanopyFilter.CODEC);

	static {
		EnumMap<MushroomFeatures.MushroomTreeType, Val<AbstractHugeMushroomFeature>> map = new EnumMap<>(MushroomFeatures.MushroomTreeType.class);
		for (var type : MushroomFeatures.MushroomTreeType.values()) {
			map.put(type, FR.reg(type.id, () -> type.factory.apply(HugeMushroomFeatureConfiguration.CODEC)));
		}
		MUSHROOM_TREES = Collections.unmodifiableMap(map);
	}

	public static void register() {

	}

}

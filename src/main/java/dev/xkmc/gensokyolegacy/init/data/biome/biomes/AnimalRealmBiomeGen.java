package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import dev.xkmc.gensokyolegacy.content.worldgen.feature.lake.LakeConfig;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The cold dry cell of the underground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the animal realm, a
 * cavern warren with a lava pool in the lowest room of each cave system.
 * <p>
 * Warped forest, the one nether biome that is mostly growth: the densest thing the nether has, which is what a realm
 * full of animals wants to be buried in.
 *
 * @see "doc/design/gensokyo_biomes.md" §2.1
 */
public class AnimalRealmBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("animal_realm");

	private static final ResourceKey<ConfiguredFeature<?, ?>> DEN_RUBBLE = cf("den_rubble");
	private static final ResourceKey<PlacedFeature> DEN_RUBBLE_PF = pf("den_rubble");
	private static final ResourceKey<ConfiguredFeature<?, ?>> FUNGUS_PATCH = cf("fungus_patch");
	private static final ResourceKey<PlacedFeature> FUNGUS_PATCH_PF = pf("fungus_patch");
	private static final ResourceKey<ConfiguredFeature<?, ?>> ANIMAL_REALM_LAKE = cf("animal_realm_lake");
	private static final ResourceKey<PlacedFeature> ANIMAL_REALM_LAKE_PF = pf("animal_realm_lake");

	/**
	 * The animal realm's lava, for this biome alone, and the other half of the ruins' pool: the two cold hells are
	 * cut by the same caves, and a water pool in one of them and a lava pool in the other is what makes them read as
	 * two places rather than one.
	 */
	private static final List<ResourceKey<PlacedFeature>> ANIMAL_REALM_LAKE_STEP = List.of(ANIMAL_REALM_LAKE_PF);

	public AnimalRealmBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.7f, 0.8f, 0x2f4a3a, 0x2c3a30, 0x5a6b45, 0x4e5c3c, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(ANIMAL_REALM_LAKE_STEP)
						.ores(OVERWORLD_ORES)
						.underground(join(NETHER_COMMON,
								List.of(NetherPlacements.WARPED_FOREST_VEGETATION, TreePlacements.WARPED_FUNGI,
										NetherPlacements.NETHER_SPROUTS, NetherPlacements.TWISTING_VINES),
								List.of(DEN_RUBBLE_PF, FUNGUS_PATCH_PF)))
						.springs(SPRINGS));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, DEN_RUBBLE, Feature.RANDOM_PATCH, onStone(16, new Palette()
				.add(Blocks.BONE_BLOCK, 3).add(Blocks.COBWEB, 2)
				.add(Blocks.COBBLESTONE, 2).add(Blocks.SPRUCE_FENCE, 1).build()));
		FeatureUtils.register(ctx, FUNGUS_PATCH, Feature.RANDOM_PATCH, onStone(24, new Palette()
				.add(GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET.cap, 2).add(GLNaturalBlocks.EUGUNE_GHOST_FIRE, 2)
				.add(GLNaturalBlocks.EUGUNE_BROWN, 2).add(Blocks.CRIMSON_NYLIUM, 2).add(Blocks.GLOW_LICHEN, 2).build()));
		// The realm's lava, the same pool in the same caverns as the ruins' water and for the same reason: this is
		// the other cold half of the underground, and a lava pool in a cavern is what says so. Netherrack for the
		// basin, because that is the rock a nether cavern is made of. It is laid after the ruins' pool because that
		// is the order the two hell rows are written in.
		FeatureUtils.register(ctx, ANIMAL_REALM_LAKE, GLWorldGen.LAKE.get(), new LakeConfig(
				LakeConfig.Mode.UNDERGROUND, 5,
				new LakeConfig.Shape(4, 20, 11, 3, 4, 6, 1),
				new LakeConfig.Material(Blocks.LAVA.defaultBlockState(), Blocks.AIR.defaultBlockState(),
						Blocks.NETHERRACK.defaultBlockState(), Blocks.AIR.defaultBlockState()), List.of()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.deposit(DEN_RUBBLE_PF, reg.feature(DEN_RUBBLE), RarityFilter.onAverageOnceEvery(5));
		reg.deposit(FUNGUS_PATCH_PF, reg.feature(FUNGUS_PATCH), CountPlacement.of(4));
		// The lava pool's placed feature is the water one with nothing changed: the same one attempt in six chunks,
		// over the same depth band, because the two cold hells sit in the same rows and are cut by the same caves.
		reg.placed(ANIMAL_REALM_LAKE_PF, reg.feature(ANIMAL_REALM_LAKE),
				RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)),
				BiomeFilter.biome());
	}

}
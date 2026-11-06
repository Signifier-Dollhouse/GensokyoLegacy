package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The hot humid cell of the underground in {@code doc/design/gensokyo_dimension_biome_guide.md}: the hell of blood
 * pools.
 * <p>
 * Soul sand valley: a basin of souls is the only thing in the nether that reads as a pool, and the basalt pillars are
 * what stands a blood pool up out of the floor.
 *
 * @see "doc/design/gensokyo_biomes.md" §2
 */
public class HellOfBloodPoolsBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("hell_of_blood_pools");

	/** A blood pool's pillar: soul sand valley's one decoration that is a piece of floor rather than a plant. */
	private static final List<ResourceKey<PlacedFeature>> NETHER_BLOOD_PILLARS =
			List.of(NetherPlacements.BASALT_PILLAR);

	private static final ResourceKey<ConfiguredFeature<?, ?>> BLOOD_DEPOSIT = cf("blood_deposit");
	private static final ResourceKey<PlacedFeature> BLOOD_DEPOSIT_PF = pf("blood_deposit");
	private static final ResourceKey<ConfiguredFeature<?, ?>> BONE_PILE = cf("bone_pile");
	private static final ResourceKey<PlacedFeature> BONE_PILE_PF = pf("bone_pile");

	public HellOfBloodPoolsBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 1.2f, 0.0f, 0x6d1420, 0x5c1414, 0x7a2a2a, 0x6a2424, new MobSpawnSettings.Builder(),
				layers(ctx)
						.localMods(NETHER_BLOOD_PILLARS)
						.ores(join(OVERWORLD_ORES, List.of(MiscOverworldPlacements.DISK_CLAY)))
						.underground(join(NETHER_COMMON, List.of(NetherPlacements.PATCH_CRIMSON_ROOTS),
								List.of(BLOOD_DEPOSIT_PF, BONE_PILE_PF)))
						.springs(SPRINGS));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, BLOOD_DEPOSIT, Feature.RANDOM_PATCH, onStone(20, new Palette()
				.add(Blocks.NETHERRACK, 3).add(Blocks.CRIMSON_NYLIUM, 2).add(Blocks.REDSTONE_ORE, 2).build()));
		FeatureUtils.register(ctx, BONE_PILE, Feature.RANDOM_PATCH, onStone(12, new Palette()
				.add(Blocks.BONE_BLOCK, 3).add(Blocks.SOUL_SAND, 2).add(Blocks.SKELETON_SKULL, 1).build()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.deposit(BLOOD_DEPOSIT_PF, reg.feature(BLOOD_DEPOSIT), CountPlacement.of(3));
		reg.deposit(BONE_PILE_PF, reg.feature(BONE_PILE), RarityFilter.onAverageOnceEvery(6));
	}

}
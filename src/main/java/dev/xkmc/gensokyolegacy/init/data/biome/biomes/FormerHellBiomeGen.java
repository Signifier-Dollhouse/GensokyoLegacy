package dev.xkmc.gensokyolegacy.init.data.biome.biomes;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.lake.LakeConfig;
import dev.xkmc.gensokyolegacy.init.data.biome.AbstractBiomeGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
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
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.List;

/**
 * The cold humid cell of the underground in {@code doc/design/gensokyo_dimension_biome_guide.md}: former hell, a
 * crimson-forest ruin with standing water in the flooded hall.
 * <p>
 * Crimson forest: roots and weeping vines growing back through a ruin is the whole idea of the place.
 *
 * @see "doc/design/gensokyo_biomes.md" §2.3
 */
public class FormerHellBiomeGen extends AbstractBiomeGen {

	public static final ResourceKey<Biome> KEY = biome("former_hell");

	private static final ResourceKey<ConfiguredFeature<?, ?>> OLD_RUBBLE = cf("old_rubble");
	private static final ResourceKey<PlacedFeature> OLD_RUBBLE_PF = pf("old_rubble");
	private static final ResourceKey<ConfiguredFeature<?, ?>> FORMER_HELL_LAKE = cf("former_hell_lake");
	private static final ResourceKey<PlacedFeature> FORMER_HELL_LAKE_PF = pf("former_hell_lake");

	/**
	 * The ruins' standing water, for this biome alone.
	 * <p>
	 * A pool of standing water is not one of the features the four underground biomes share, because the other three
	 * are hot and water in a lava cave would quench it. Former Hell is the cold humid one, and it is the only one of
	 * the four whose rock is a ruin, which is what gives a flooded hall somewhere to be flooded in the first place.
	 */
	private static final List<ResourceKey<PlacedFeature>> FORMER_HELL_LAKE_STEP = List.of(FORMER_HELL_LAKE_PF);

	public FormerHellBiomeGen() {
		super(KEY);
	}

	@Override
	protected Biome build(BootstrapContext<Biome> ctx) {
		return biome(true, 0.8f, 0.4f, 0x5a4a3a, 0x6a5a4a, 0x7a6b52, 0x6b5c46, new MobSpawnSettings.Builder(),
				layers(ctx)
						.lakes(FORMER_HELL_LAKE_STEP)
						.ores(OVERWORLD_ORES)
						.underground(join(NETHER_COMMON,
								List.of(NetherPlacements.CRIMSON_FOREST_VEGETATION, TreePlacements.CRIMSON_FUNGI,
										NetherPlacements.PATCH_CRIMSON_ROOTS, NetherPlacements.WEEPING_VINES),
								List.of(OLD_RUBBLE_PF)))
						.springs(SPRINGS));
	}

	@Override
	protected void configured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
		FeatureUtils.register(ctx, OLD_RUBBLE, Feature.RANDOM_PATCH, onStone(16, new Palette()
				.add(Blocks.CRACKED_STONE_BRICKS, 3).add(Blocks.MOSSY_COBBLESTONE, 2)
				.add(Blocks.BRICKS, 2).add(Blocks.GRAVEL, 1).build()));
		// The ruins' standing water, and the only lake in the biome list that is not on the surface. It has to be a
		// separate configured feature from the magical forest's pond because the mode is in the config: this one is
		// poured into a cavern, which means it finds its own floor instead of taking the ground it lands on, keeps
		// five blocks of headroom above the water, and dams itself with a wall of the ruin's own stone brick rather
		// than a rim of turf. The box is 20 wide and 11 tall, which leaves room for 3 deep of water, the air the
		// maker cuts above it and that headroom. The rim is one block thick: a thicker one is a second course of
		// stone brick standing on the cavern floor, and where the floor is thin or the carver has eaten into it that
		// reads as a ledge floating over nothing.
		FeatureUtils.register(ctx, FORMER_HELL_LAKE, GLWorldGen.LAKE.get(), new LakeConfig(
				LakeConfig.Mode.UNDERGROUND, 5,
				new LakeConfig.Shape(4, 20, 11, 3, 4, 6, 1),
				new LakeConfig.Material(Blocks.WATER.defaultBlockState(), Blocks.AIR.defaultBlockState(),
						Blocks.STONE_BRICKS.defaultBlockState(), Blocks.AIR.defaultBlockState()), List.of()));
	}

	@Override
	protected void placed(Registrar reg) {
		reg.deposit(OLD_RUBBLE_PF, reg.feature(OLD_RUBBLE), RarityFilter.onAverageOnceEvery(4));
		// The pool's own placed feature. No heightmap: a cavern has no surface to place on, and putting it on the
		// world's is exactly the surface lake this is not. One attempt per 6 chunks: a cave system is a lot of
		// columns and the lowest room of each is a site, so an attempt every chunk fills one system with pools. The
		// range is the depth band former hell occupies on both of its rows — the ruins under the mountain and the
		// same ruins in the low ground — so the biome filter spends its attempts inside the biome instead of on the
		// mountain interior above it. The feature walks the column from there.
		reg.placed(FORMER_HELL_LAKE_PF, reg.feature(FORMER_HELL_LAKE),
				RarityFilter.onAverageOnceEvery(6), InSquarePlacement.spread(),
				HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(64)),
				BiomeFilter.biome());
	}

}
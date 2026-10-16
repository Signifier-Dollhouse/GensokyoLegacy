package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureTagGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.NetherPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.data.worldgen.placement.TreePlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * Every biome the Gensokyo dimension may select, and how each one is put together.
 * <p>
 * A biome's features are <em>written out</em>, step by step, in the call that registers it. Nothing is copied from a
 * vanilla biome any more: that used to mean a sculk vein and a monster room arrived wholesale in a Gensokyo biome
 * without anyone asking for them, and the only way to read or change a biome's list was to know which vanilla biome it
 * happened to be standing on. A vanilla name now appears in a comment as the reason for a choice, not as the source of
 * the list.
 * <p>
 * Shared runs live in the constants below and are named by what they are, so editing a biome is editing the lines
 * that name it and nothing else. Never interleave a shared run with a custom feature: that is what a feature order
 * cycle looks like, and it only fails at runtime.
 * <p>
 * No biome spawns a mob. Natural spawning in Gensokyo belongs to the mod's own entities, added by biome modifier, not
 * to a vanilla spawn table copied in beside them.
 *
 * @see "doc/design/gensokyo_biomes.md"
 */
public class GLBiomes {

	public static final ResourceKey<Biome> MAGICAL_FOREST = biome("magical_forest");
	public static final ResourceKey<Biome> SAKURA_FOREST = biome("sakura_forest");
	// lower ground
	public static final ResourceKey<Biome> MUENZUKA = biome("muenzuka");
	public static final ResourceKey<Biome> HUMAN_VILLAGE = biome("human_village");
	public static final ResourceKey<Biome> MYOUREN_TEMPLE_BARRENS = biome("myouren_temple_barrens");
	// upper ground
	public static final ResourceKey<Biome> BAMBOO_FOREST_OF_THE_LOST = biome("bamboo_forest_of_the_lost");
	public static final ResourceKey<Biome> HILL_OF_THE_NAMELESS = biome("hill_of_the_nameless");
	// mountain
	public static final ResourceKey<Biome> UNTRODDEN_VALLEY = biome("untrodden_valley");
	public static final ResourceKey<Biome> GENBU_RAVINE = biome("genbu_ravine");
	public static final ResourceKey<Biome> YOUKAI_MOUNTAIN_FOOTHILLS = biome("youkai_mountain_foothills");
	public static final ResourceKey<Biome> SANCTUARY = biome("sanctuary");
	// underground
	public static final ResourceKey<Biome> HELL_OF_BLOOD_POOLS = biome("hell_of_blood_pools");
	public static final ResourceKey<Biome> ANIMAL_REALM = biome("animal_realm");
	public static final ResourceKey<Biome> HELL_OF_BLAZING_FIRES = biome("hell_of_blazing_fires");
	public static final ResourceKey<Biome> FORMER_HELL = biome("former_hell");
	// the fixed locations of `gensokyolegacy_fixed_biomes_guide.md`, one per water-level or high-altitude band
	public static final ResourceKey<Biome> MISTY_LAKE = biome("misty_lake");
	public static final ResourceKey<Biome> MISTY_LAKE_SHORE = biome("misty_lake_shore");
	public static final ResourceKey<Biome> TENGU_SETTLEMENT = biome("tengu_settlement");
	public static final ResourceKey<Biome> YOUKAI_MOUNTAIN_SUMMIT = biome("youkai_mountain_summit");
	// the dry half of the settlement's band, which the guide does not name
	public static final ResourceKey<Biome> SECRET_HEAVEN_CLIFF = biome("secret_heaven_cliff");

	/**
	 * The rock the mountain is hollow, from just under its topsoil down to {@code DEEP_DEPTH}, and nowhere but the
	 * mountain. Not one of the fixed locations: a single climate region of its own, keyed on depth and
	 * continentalness rather than on the surface.
	 *
	 * @see "doc/design/gensokyo_biomes.md" §5
	 * @see "doc/design/gensokyo_dimension.md" §9
	 */
	public static final ResourceKey<Biome> MOUNTAIN_INTERIOR = biome("mountain_interior");

	public static final ResourceKey<Biome> SUNFLOWER_PLAINS = biome("sunflower_plains");

	/**
	 * The two carvers every Gensokyo biome carves with, which are the overworld's own, unchanged. {@code minecraft:canyon}
	 * is in none of them: a canyon is a surface landform, and Gensokyo's ravine and valley are already its own thing.
	 */
	private static final List<ResourceKey<ConfiguredWorldCarver<?>>> CARVERS =
			List.of(Carvers.CAVE, Carvers.CAVE_EXTRA_UNDERGROUND);

	/**
	 * The overworld ore set, in vanilla's order: the dirt and gravel, the three stone mixes and tuff, then everything
	 * worth digging for. Every Gensokyo biome carries it, underground ones included, because a dimension whose only
	 * ore is what the nether places is not a dimension anyone can mine.
	 */
	private static final List<ResourceKey<PlacedFeature>> OVERWORLD_ORES = List.of(
			OrePlacements.ORE_DIRT, OrePlacements.ORE_GRAVEL,
			OrePlacements.ORE_GRANITE_UPPER, OrePlacements.ORE_GRANITE_LOWER,
			OrePlacements.ORE_DIORITE_UPPER, OrePlacements.ORE_DIORITE_LOWER,
			OrePlacements.ORE_ANDESITE_UPPER, OrePlacements.ORE_ANDESITE_LOWER,
			OrePlacements.ORE_TUFF, OrePlacements.ORE_COAL_UPPER, OrePlacements.ORE_COAL_LOWER,
			OrePlacements.ORE_IRON_UPPER, OrePlacements.ORE_IRON_MIDDLE, OrePlacements.ORE_IRON_SMALL,
			OrePlacements.ORE_GOLD, OrePlacements.ORE_GOLD_LOWER,
			OrePlacements.ORE_REDSTONE, OrePlacements.ORE_REDSTONE_LOWER,
			OrePlacements.ORE_DIAMOND, OrePlacements.ORE_DIAMOND_MEDIUM, OrePlacements.ORE_DIAMOND_LARGE,
			OrePlacements.ORE_DIAMOND_BURIED, OrePlacements.ORE_LAPIS, OrePlacements.ORE_LAPIS_BURIED,
			OrePlacements.ORE_COPPER, CavePlacements.UNDERWATER_MAGMA);

	/** The three soil disks the surface biomes lay on their floor, which is why Gensokyo has sand, clay and gravel. */
	private static final List<ResourceKey<PlacedFeature>> OVERWORLD_DISKS = List.of(
			MiscOverworldPlacements.DISK_SAND, MiscOverworldPlacements.DISK_CLAY, MiscOverworldPlacements.DISK_GRAVEL);

	/** Lava lakes. The one piece of overworld surface furniture the mountain's own rock keeps. */
	private static final List<ResourceKey<PlacedFeature>> LAVA_LAKES = List.of(
			MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND, MiscOverworldPlacements.LAKE_LAVA_SURFACE);

	/** Water and lava in the rock, which is how every Gensokyo cave gets a pool to walk down to. */
	private static final List<ResourceKey<PlacedFeature>> SPRINGS = List.of(
			MiscOverworldPlacements.SPRING_WATER, MiscOverworldPlacements.SPRING_LAVA);

	/**
	 * The ravine's springs: vanilla's own {@code spring_water} at three times the count, and no lava at all.
 * <p>
 * It **replaces** {@link #SPRINGS} rather than joining it — the feature is in the same step, so carrying vanilla's
 * as well would place every spring twice and make the count a lie. Replacing rather than adding is also what takes
 * the lava with it, and that is the point: a ravine is a wet place and the shared `spring_lava` was the only lava in
 * it.
 *
 * @see "doc/design/gensokyo_biomes.md" §4
 */
	private static final List<ResourceKey<PlacedFeature>> RAVINE_SPRINGS = List.of(
			GensokyoBiomeFeatures.RAVINE_SPRING_WATER_PF);

	/**
	 * The ruins' standing water, for {@code former_hell} alone.
	 * <p>
	 * A pool of standing water is not one of the features the four underground biomes share, because the other three
	 * are hot and water in a lava cave would quench it. Former Hell is the cold humid one, and it is the only one of
	 * the four whose rock is a ruin, which is what gives a flooded hall somewhere to be flooded in the first place.
	 *
	 * @see "doc/design/gensokyo_biomes.md" §2
	 */
	private static final List<ResourceKey<PlacedFeature>> FORMER_HELL_LAKE =
			List.of(GensokyoBiomeFeatures.FORMER_HELL_LAKE_PF);

	/**
	 * The animal realm's lava, for {@code animal_realm} alone, and the other half of the pair above: the two cold
	 * hells are cut by the same caves, and a water pool in one of them and a lava pool in the other is what makes
	 * them read as two places rather than one.
	 *
	 * @see "doc/design/gensokyo_biomes.md" §2.1
	 */
	private static final List<ResourceKey<PlacedFeature>> ANIMAL_REALM_LAKE =
			List.of(GensokyoBiomeFeatures.ANIMAL_REALM_LAKE_PF);

	/**
	 * The deltas' lava, for {@code hell_of_blazing_fires} alone. Every other nether spring is left out — see
	 * {@link #NETHER_COMMON} — but this one is the lava rather than a hazard: it is what makes a biome of basalt
	 * a lava biome, and without it the blazing fires are hot-looking rock.
	 */
	private static final List<ResourceKey<PlacedFeature>> DELTA_LAVA = List.of(NetherPlacements.SPRING_DELTA);

	/** The blazing fires' own lava spring, over the nether spring's block list. See {@code GensokyoBiomeFeatures}. */
	private static final List<ResourceKey<PlacedFeature>> BLAZING_HELL_LAVA =
			List.of(GensokyoBiomeFeatures.BLAZING_HELL_LAVA_PF);

	/** Emerald only spawns in a mountain, so only the mountain's biomes get it. */
	private static final List<ResourceKey<PlacedFeature>> EMERALD = List.of(OrePlacements.ORE_EMERALD);

	/** Infested stone, the other half of what makes the mountain's rock read as a mountain and not as a cave. */
	private static final List<ResourceKey<PlacedFeature>> INFESTED = List.of(OrePlacements.ORE_INFESTED);

	/** The single lichen run every Gensokyo biome carries, and the whole of the mountain interior's vegetation. */
	private static final List<ResourceKey<PlacedFeature>> LICHEN = List.of(CavePlacements.GLOW_LICHEN);

	/** The snow line is nowhere near any of these, but the step runs anyway and vanilla's rule is the rule. */
	private static final List<ResourceKey<PlacedFeature>> FREEZE_TOP_LAYER =
			List.of(MiscOverworldPlacements.FREEZE_TOP_LAYER);

	/** Dinosaur bones. Gensokyo's ravine is the one place below ground where they are still bones and not a theme. */
	private static final List<ResourceKey<PlacedFeature>> FOSSILS = List.of(
			CavePlacements.FOSSIL_UPPER, CavePlacements.FOSSIL_LOWER);

	/** A blood pool's pillar: soul sand valley's one decoration that is a piece of floor rather than a plant. */
	private static final List<ResourceKey<PlacedFeature>> NETHER_BLOOD_PILLARS =
			List.of(NetherPlacements.BASALT_PILLAR);

	/**
	 * The three surface features of the deltas, in vanilla's order: the lava pools, then the two basalt column
	 * runs, which is what makes a lava hall a delta and not a cave. {@code minecraft:delta} is the one that fills
	 * them — forty attempts on every layer, each dropping a pool of lava with a magma rim into the one-block air
	 * pockets in the floor. Without it the biome gets a lava spring every sixteenth try and nothing else, which is
	 * a single block of lava rather than a delta.
	 */
	private static final List<ResourceKey<PlacedFeature>> BASALT_COLUMNS = List.of(
			NetherPlacements.DELTA, NetherPlacements.SMALL_BASALT_COLUMNS, NetherPlacements.LARGE_BASALT_COLUMNS);

	/**
	 * The six features every nether biome shares, and the whole of what the four underground biomes agree on: fire,
	 * soul fire, two glowstone runs and the two nether mushrooms.
	 * <p>
	 * Deliberately absent from all of them are the nether <em>ores</em> — magma, blackstone, quartz, ancient debris.
	 * Their placement is written against the nether's own 0…128 column, so copied verbatim they land at y 5…41 and
	 * miss the underground biomes entirely, which is not where the rock is. The overworld ore set above replaces them.
	 * Also absent are the nether springs, which flood a cave system with lava.
	 */
	private static final List<ResourceKey<PlacedFeature>> NETHER_COMMON = List.of(
			NetherPlacements.PATCH_FIRE, NetherPlacements.PATCH_SOUL_FIRE,
			NetherPlacements.GLOWSTONE_EXTRA, NetherPlacements.GLOWSTONE,
			VegetationPlacements.BROWN_MUSHROOM_NETHER, VegetationPlacements.RED_MUSHROOM_NETHER);

	public static void init(DataProviderInitializer init) {
		init.add(Registries.BIOME, (ctx) -> {
			var pf = ctx.lookup(Registries.PLACED_FEATURE);
			var carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
			ctx.register(GLDimensionGen.BIOME_GAP, biome(6840176,
					new MobSpawnSettings.Builder(),
					new BiomeGenerationSettings.PlainBuilder(),
					Musics.createGameMusic(SoundEvents.MUSIC_END)));
			var magicalForest = addDefaultOres(new BiomeGenerationSettings.Builder(pf, carvers))
					.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GLFeatureGen.MAGICAL_FOREST_DISK_COARSE_DIRT_PLACED)
					.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GLFeatureGen.MAGICAL_FOREST_DISK_PODZOL_PLACED)
					.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, GLFeatureGen.MAGICAL_FOREST_DISK_MYCELIUM_PLACED);
			MagicalForestFeatures.addLake(magicalForest);
			MagicalForestFeatures.addVegetation(magicalForest);
			ctx.register(MAGICAL_FOREST, biome(new MobSpawnSettings.Builder(), magicalForest));
			ctx.register(SAKURA_FOREST, biome(true, 0.5f, 0.8f, 0xc0d8ff, 11983713, 11983713,
					new MobSpawnSettings.Builder(),
					addDefaultOres(new BiomeGenerationSettings.Builder(pf, carvers))
							.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_PLAIN)
							.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_CHERRY)
							.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_CHERRY),
					null
			));
			registerTouhouBiomes(ctx, pf, carvers);
			registerFixedBiomes(ctx, pf, carvers);
		});

		init.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ctx -> {
			ctx.register(biomeMod("plain_fairy"), new BiomeModifiers.AddSpawnsBiomeModifier(
					ctx.lookup(Registries.BIOME).getOrThrow(GLStructureTagGen.PLAIN_FAIRY_SPAWN),
					List.of(new MobSpawnSettings.SpawnerData(GLEntities.PLAIN_FAIRY.get(), 10, 3, 3))
			));
		});
	}

	/**
	 * The twelve biomes of {@code doc/design/gensokyo_dimension_biome_guide.md}. Each writes out its own features and
	 * carries Gensokyo's own climate, colours and feature tail.
	 * <p>
	 * The calls are in the guide's order — hot humid, cold humid, hot dry, cold dry — so the declared temperature and
	 * downfall agree with the climate cell the grid picks the biome in. Every one of them stays well above the 0.15
	 * snow line even at the top of its band: a cold <em>cell</em> is a place on the climate map, and nothing here is
	 * meant to actually freeze. The hot cells of each band are lifted a little to keep the split legible in the
	 * numbers.
	 *
	 * @see #coldEnoughToSnow
	 */
	private static void registerTouhouBiomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf,
											HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		var muenzuka = GensokyoBiomeFeatures.MUENZUKA;
		ctx.register(MUENZUKA, biome(true, 0.3f, 0.9f, 0x38605c, 0x3f4a46, 0x6a7058, 0x5a604e, new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, muenzuka.soil()))
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN, VegetationPlacements.FOREST_FLOWERS,
								VegetationPlacements.TREES_BIRCH_AND_OAK, VegetationPlacements.FLOWER_DEFAULT,
								VegetationPlacements.PATCH_GRASS_FOREST, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
								VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
								VegetationPlacements.PATCH_PUMPKIN), muenzuka.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var village = GensokyoBiomeFeatures.HUMAN_VILLAGE;
		ctx.register(HUMAN_VILLAGE, biome(true, 0.8f, 0.5f, 0x4f8f7a, 0x8fb7d6, 0x7db356, 0x6f9c4a, new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(PLAINS_GROUND, village.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var barrens = GensokyoBiomeFeatures.BARRENS;
		ctx.register(MYOUREN_TEMPLE_BARRENS, biome(false, 0.2f, 0.2f, 0x6b5a35, 0xc0a878, 0x9a8f5c, 0x8a7f50,
				new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, List.of(OrePlacements.ORE_GOLD_EXTRA), OVERWORLD_DISKS, barrens.soil()))
						.springs(SPRINGS)
						.vegetation(join(BADLANDS_GROUND, barrens.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var bamboo = GensokyoBiomeFeatures.BAMBOO_FOREST;
		ctx.register(BAMBOO_FOREST_OF_THE_LOST, biome(true, 0.3f, 0.9f, 0x3f6f6a, 0x7d94a0, 0x6f8f52, 0x5f7f4a,
				new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, bamboo.soil()))
						.springs(SPRINGS)
						.vegetation(join(TAIGA_GROUND, List.of(VegetationPlacements.PATCH_BERRY_RARE), bamboo.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var hill = GensokyoBiomeFeatures.HILL;
		ctx.register(HILL_OF_THE_NAMELESS, biome(true, 0.3f, 0.4f, 0x4a7a86, 0x8aa0b0, 0x87a06a, 0x7a9260,
				new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(MEADOW_GROUND, hill.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var valley = GensokyoBiomeFeatures.VALLEY;
		ctx.register(UNTRODDEN_VALLEY, biome(true, 0.5f, 0.8f, 0x3c6f7d, 0x7d8f9c, 0x6b8560, 0x5d7854, new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(TAIGA_GROUND, List.of(VegetationPlacements.PATCH_BERRY_COMMON), valley.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var ravine = GensokyoBiomeFeatures.RAVINE;
		ctx.register(GENBU_RAVINE, biome(true, 0.35f, 0.9f, 0x39604f, 0x5f7a6a, 0x5a7a5c, 0x4e6e52, new MobSpawnSettings.Builder(),
				// swamp, which is what makes it a wet ravine on the surface, plus lush caves below it: the biome's own
				// moss and mud stay, because lush caves brings growth and vines rather than a ground cover to replace
				// them with. `ore_clay` is lush caves' one special ore, and it goes where vanilla puts it — after the
				// default set, before the soft disks. The springs are its own — water at three times vanilla's count and no
				// lava, which is what replacing the shared pair does
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.structures(FOSSILS)
						.ores(join(OVERWORLD_ORES, LUSH_CAVES_ORES, List.of(MiscOverworldPlacements.DISK_CLAY), ravine.soil()))
						.springs(RAVINE_SPRINGS)
						.vegetation(join(SWAMP_GROUND, LUSH_CAVES, ravine.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var foothills = GensokyoBiomeFeatures.FOOTHILLS;
		ctx.register(YOUKAI_MOUNTAIN_FOOTHILLS, biome(true, 0.6f, 0.5f, 0x46707d, 0x8496a8, 0x6f8a58, 0x617c4e,
				new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(CLIFF_GROUND, foothills.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var sanctuary = GensokyoBiomeFeatures.SANCTUARY;
		ctx.register(SANCTUARY, biome(true, 0.35f, 0.4f, 0x57a0a6, 0xa8c4d8, 0x8fc06a, 0x82b45e, new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(MEADOW_GROUND, sanctuary.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		// The four underground biomes, nether to look at. They agree on NETHER_COMMON and the overworld ore set,
		// and each then takes the one nether biome that is about the same thing it is about, minus that biome's ores
		// and springs. See NETHER_COMMON for why those two go.
		var blood = GensokyoBiomeFeatures.BLOOD_POOLS;
		ctx.register(HELL_OF_BLOOD_POOLS, biome(true, 1.2f, 0.0f, 0x6d1420, 0x5c1414, 0x7a2a2a, 0x6a2424,
				new MobSpawnSettings.Builder(),
				// soul sand valley: a basin of souls is the only thing in the nether that reads as a pool, and the
				// basalt pillars are what stands a blood pool up out of the floor
				layers(pf, carvers)
						.localMods(NETHER_BLOOD_PILLARS)
						.ores(join(OVERWORLD_ORES, List.of(MiscOverworldPlacements.DISK_CLAY)))
						.underground(join(NETHER_COMMON, List.of(NetherPlacements.PATCH_CRIMSON_ROOTS), blood.cave()))
						.springs(SPRINGS)));

		var animalRealm = GensokyoBiomeFeatures.ANIMAL_REALM;
		ctx.register(ANIMAL_REALM, biome(true, 0.7f, 0.8f, 0x2f4a3a, 0x2c3a30, 0x5a6b45, 0x4e5c3c, new MobSpawnSettings.Builder(),
				// warped forest, the one nether biome that is mostly growth: the densest thing the nether has, which
				// is what a realm full of animals wants to be buried in
				layers(pf, carvers)
						.lakes(ANIMAL_REALM_LAKE)
						.ores(OVERWORLD_ORES)
						.underground(join(NETHER_COMMON, List.of(NetherPlacements.WARPED_FOREST_VEGETATION,
								TreePlacements.WARPED_FUNGI, NetherPlacements.NETHER_SPROUTS,
								NetherPlacements.TWISTING_VINES), animalRealm.cave()))
						.springs(SPRINGS)));

		var fire = GensokyoBiomeFeatures.BLAZING_FIRES;
		ctx.register(HELL_OF_BLAZING_FIRES, biome(true, 1.5f, 0.0f, 0xc2410a, 0x8c2f0e, 0x6b4030, 0x5a3528,
				new MobSpawnSettings.Builder(),
				// basalt deltas. The column runs are the find here: vanilla lays them on the nether's open floor,
				// and in a cave they stand up off the floor instead, which is a hall of basalt rather than a delta.
				// `minecraft:delta` is the other half and was missing: it is what pours the lava into the pockets
				// the columns stand on, and the biome had the columns with nothing to fill them. spring_delta comes
				// with them and is the one nether spring that does: the delta's lava is what the biome is named for,
				// and the surface rule only gives it magma block, which glows but does not flow. BLAZING_HELL_LAVA is
				// a second one, because vanilla's spring_lava is the overworld spring and all of its valid blocks
				// are stone — it cannot place a single block in a biome standing on blackstone
				layers(pf, carvers)
						.surfaceStructures(BASALT_COLUMNS)
						.ores(OVERWORLD_ORES)
						.underground(join(NETHER_COMMON, List.of(NetherPlacements.BASALT_BLOBS,
								NetherPlacements.BLACKSTONE_BLOBS), fire.cave()))
						.springs(join(SPRINGS, DELTA_LAVA, BLAZING_HELL_LAVA))));

		var formerHell = GensokyoBiomeFeatures.FORMER_HELL;
		ctx.register(FORMER_HELL, biome(true, 0.8f, 0.4f, 0x5a4a3a, 0x6a5a4a, 0x7a6b52, 0x6b5c46, new MobSpawnSettings.Builder(),
				// crimson forest: roots and weeping vines growing back through a ruin is the whole idea of the place
				layers(pf, carvers)
						.lakes(FORMER_HELL_LAKE)
						.ores(OVERWORLD_ORES)
						.underground(join(NETHER_COMMON, List.of(NetherPlacements.CRIMSON_FOREST_VEGETATION,
								TreePlacements.CRIMSON_FUNGI, NetherPlacements.PATCH_CRIMSON_ROOTS,
								NetherPlacements.WEEPING_VINES), formerHell.cave()))
						.springs(SPRINGS)));

		// the mountain's own rock, which is overworld rock and not nether rock: stony peaks' ore set, its emerald,
		// its infested stone, its lava lakes and its glow lichen. It was deep dark once, which is where the sculk in
		// the halls came from — nothing here asks for it. No freeze either: the biome is only ever below a surface,
		// so the top-layer step has nothing to act on.
		ctx.register(MOUNTAIN_INTERIOR, biome(true, 0.6f, 0.8f, 0x2c3a50, 0x5a6a72, null, null, new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(LICHEN)));
	}

	/** The ground cover the overworld plains and forests put on top of Gensokyo's grass, which the tail then adds to. */
	private static final List<ResourceKey<PlacedFeature>> PLAINS_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_TALL_GRASS_2, VegetationPlacements.TREES_PLAINS,
			VegetationPlacements.FLOWER_PLAINS, VegetationPlacements.PATCH_GRASS_PLAIN,
			VegetationPlacements.BROWN_MUSHROOM_NORMAL, VegetationPlacements.RED_MUSHROOM_NORMAL,
			VegetationPlacements.PATCH_SUGAR_CANE, VegetationPlacements.PATCH_PUMPKIN);

	private static final List<ResourceKey<PlacedFeature>> FOREST_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.FOREST_FLOWERS,
			VegetationPlacements.TREES_BIRCH_AND_OAK, VegetationPlacements.FLOWER_DEFAULT,
			VegetationPlacements.PATCH_GRASS_FOREST, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
			VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
			VegetationPlacements.PATCH_PUMPKIN);

	private static final List<ResourceKey<PlacedFeature>> TAIGA_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_LARGE_FERN, VegetationPlacements.TREES_TAIGA,
			VegetationPlacements.FLOWER_DEFAULT, VegetationPlacements.PATCH_GRASS_TAIGA_2,
			VegetationPlacements.BROWN_MUSHROOM_TAIGA, VegetationPlacements.RED_MUSHROOM_TAIGA,
			VegetationPlacements.PATCH_SUGAR_CANE, VegetationPlacements.PATCH_PUMPKIN);

	private static final List<ResourceKey<PlacedFeature>> MEADOW_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_TALL_GRASS_2,
			VegetationPlacements.PATCH_GRASS_PLAIN, VegetationPlacements.FLOWER_MEADOW,
			VegetationPlacements.TREES_MEADOW);

	private static final List<ResourceKey<PlacedFeature>> BADLANDS_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_GRASS_BADLANDS,
			VegetationPlacements.PATCH_DEAD_BUSH_BADLANDS, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
			VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE_BADLANDS,
			VegetationPlacements.PATCH_PUMPKIN, VegetationPlacements.PATCH_CACTUS_DECORATED);

	private static final List<ResourceKey<PlacedFeature>> SWAMP_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.TREES_SWAMP, VegetationPlacements.FLOWER_SWAMP,
			VegetationPlacements.PATCH_GRASS_NORMAL, VegetationPlacements.PATCH_DEAD_BUSH,
			VegetationPlacements.PATCH_WATERLILY, VegetationPlacements.BROWN_MUSHROOM_SWAMP,
			VegetationPlacements.RED_MUSHROOM_SWAMP, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
			VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE_SWAMP,
			VegetationPlacements.PATCH_PUMPKIN, AquaticPlacements.SEAGRASS_SWAMP);

	/**
	 * Lush caves' seven, in {@code BiomeDefaultFeatures.addLushCavesVegetationFeatures}'s order, which is the order the
	 * feature sorter reads. {@code genbu_ravine} is the one surface biome that takes them, and it takes them whole —
	 * the floor growth, the two vine sets, the ceiling growth and the rooted azalea all hang off the same cave
	 * surfaces, so taking some of the seven would be taking a fragment of one decoration rather than a style.
	 *
	 * @see "doc/design/gensokyo_biomes.md" §4
	 */
	private static final List<ResourceKey<PlacedFeature>> LUSH_CAVES = List.of(
			CavePlacements.LUSH_CAVES_CEILING_VEGETATION, CavePlacements.CAVE_VINES,
			CavePlacements.LUSH_CAVES_CLAY, CavePlacements.LUSH_CAVES_VEGETATION,
			CavePlacements.ROOTED_AZALEA_TREE, CavePlacements.SPORE_BLOSSOM, CavePlacements.CLASSIC_VINES);

	/**
	 * Lush caves' one special ore, {@code BiomeDefaultFeatures.addLushCavesSpecialOres}. It goes in the soft-ore step
	 * between the default ore set and the soft disks, which is where vanilla writes it.
	 */
	private static final List<ResourceKey<PlacedFeature>> LUSH_CAVES_ORES = List.of(OrePlacements.ORE_CLAY);

	/** The windswept pair, which the cliff and the foothills both take; the badlands ground is all either of them has. */
	private static final List<ResourceKey<PlacedFeature>> CLIFF_GROUND = List.of(
			CavePlacements.GLOW_LICHEN, VegetationPlacements.TREES_WINDSWEPT_HILLS,
			VegetationPlacements.FLOWER_DEFAULT, VegetationPlacements.PATCH_GRASS_BADLANDS,
			VegetationPlacements.BROWN_MUSHROOM_NORMAL, VegetationPlacements.RED_MUSHROOM_NORMAL,
			VegetationPlacements.PATCH_SUGAR_CANE, VegetationPlacements.PATCH_PUMPKIN);

	/**
	 * The fixed locations of {@code gensokyolegacy_fixed_biomes_guide.md}, plus the one the guide does not name. They
	 * are not a fifth through eighth band of the climate grid: each takes most or all of the plane of a single height
	 * band, so what picks them is continentalness and very nearly nothing else. Misty Lake is the drowned set, its
	 * shore is the strip above the waterline, and the settlement and the summit are the two bands above the mountain.
	 * <p>
	 * {@link #SECRET_HEAVEN_CLIFF} is the exception, and the only one of them humidity picks at all: it is the dry
	 * half of the settlement's band, so continentalness still fixes its height and humidity alone splits it from the
	 * settlement next to it. See {@code doc/design/gensokyo_biomes.md} §7.
	 */
	private static void registerFixedBiomes(BootstrapContext<Biome> ctx, HolderGetter<PlacedFeature> pf,
											 HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		// a broad open lake: squid and salmon, no drowned mobs, because there is nothing here to drown in
		var lake = GensokyoBiomeFeatures.MISTY_LAKE;
		// minecraft:trees_water is the one feature this biome does not take from its base: mangrove and dark oak water
		// trees are a swamp signal, and the guide asks for a lake that does not read as one
		ctx.register(MISTY_LAKE, biome(true, 0.3f, 0.9f, 0x3a7f8c, 0x86b4c8, 0x6f9c74, 0x5f8a66, new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN, VegetationPlacements.FLOWER_DEFAULT,
								VegetationPlacements.PATCH_GRASS_BADLANDS, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
								VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
								VegetationPlacements.PATCH_PUMPKIN, AquaticPlacements.SEAGRASS_RIVER), lake.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var shore = GensokyoBiomeFeatures.MISTY_LAKE_SHORE;
		ctx.register(MISTY_LAKE_SHORE, biome(true, 0.4f, 0.7f, 0x3a7f8c, 0x9cc0cc, 0x7da863, 0x6c965a,
				new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN, VegetationPlacements.FLOWER_DEFAULT,
								VegetationPlacements.PATCH_GRASS_BADLANDS, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
								VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
								VegetationPlacements.PATCH_PUMPKIN), shore.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		var settlement = GensokyoBiomeFeatures.TENGU_SETTLEMENT;
		ctx.register(TENGU_SETTLEMENT, biome(true, 0.4f, 0.5f, 0x46707d, 0x93a6b8, 0x7a9464, 0x6b8456,
				new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(FOREST_GROUND, settlement.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		// the dry half of the same band, a cliff ledge over the settlement rather than a second town. The stone is
		// carried by the surface rule's steep test in the dimension rather than by a palette here, and preciptation
		// stays on, which is what keeps it off the snow line
		var cliff = GensokyoBiomeFeatures.SECRET_HEAVEN_CLIFF;
		ctx.register(SECRET_HEAVEN_CLIFF, biome(true, 0.35f, 0.2f, 0x3f7482, 0x9aa8b8, 0x8c8b5a, 0x7d7d50,
				new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN,
								VegetationPlacements.TREES_WINDSWEPT_FOREST, VegetationPlacements.FLOWER_DEFAULT,
								VegetationPlacements.PATCH_GRASS_BADLANDS, VegetationPlacements.BROWN_MUSHROOM_NORMAL,
								VegetationPlacements.RED_MUSHROOM_NORMAL, VegetationPlacements.PATCH_SUGAR_CANE,
								VegetationPlacements.PATCH_PUMPKIN), cliff.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		// the summit, the one place the mountain's own rock reaches daylight
		var summit = GensokyoBiomeFeatures.SUMMIT;
		ctx.register(YOUKAI_MOUNTAIN_SUMMIT, biome(true, 0.4f, 0.3f, 0x3f6f7d, 0xa8b4c4, 0x8a9a72, 0x7a8a64,
				new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS, EMERALD))
						.underground(INFESTED)
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN), summit.vegetation()))
						.topLayer(FREEZE_TOP_LAYER)));

		// the guide's own hot humid lower-ground biome, and the one that used to be a verbatim vanilla copy: its
		// climate, colours and sky are vanilla's own numbers, because a sunflower field is plains and nothing about
		// the flowers changes what the grass looks like. Only its features are written out here.
		ctx.register(SUNFLOWER_PLAINS, biome(true, 0.8f, 0.4f, 0x3f76e4, 0xc0d8ff, 0x78a7ff,
				null, null, new MobSpawnSettings.Builder(),
				layers(pf, carvers)
						.lakes(LAVA_LAKES)
						.ores(join(OVERWORLD_ORES, OVERWORLD_DISKS))
						.springs(SPRINGS)
						.vegetation(join(List.of(CavePlacements.GLOW_LICHEN, VegetationPlacements.PATCH_TALL_GRASS_2,
								VegetationPlacements.PATCH_SUNFLOWER, VegetationPlacements.TREES_PLAINS,
								VegetationPlacements.FLOWER_PLAINS, VegetationPlacements.PATCH_GRASS_PLAIN,
								VegetationPlacements.BROWN_MUSHROOM_NORMAL, VegetationPlacements.RED_MUSHROOM_NORMAL,
								VegetationPlacements.PATCH_SUGAR_CANE, VegetationPlacements.PATCH_PUMPKIN)))
						.topLayer(FREEZE_TOP_LAYER)));
	}

	/**
	 * One biome's features, written out step by step.
	 *
	 * Nothing is inherited, so this is the whole of what a biome generates: the two carvers are the only thing set up
	 * for it, and every other step is added by name, in the order the step should generate. A step left alone is a
	 * step that does not happen.
	 */
	private static class Layers extends BiomeGenerationSettings.PlainBuilder {

		private final BiomeGenerationSettings.Builder builder;

		private Layers(HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
			builder = new BiomeGenerationSettings.Builder(pf, carvers);
			for (var carver : CARVERS) builder.addCarver(GenerationStep.Carving.AIR, carver);
		}

		private Layers add(GenerationStep.Decoration step, List<ResourceKey<PlacedFeature>> features) {
			for (var feature : features) builder.addFeature(step, feature);
			return this;
		}

		private Layers localMods(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.LOCAL_MODIFICATIONS, features);
		}

		private Layers lakes(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.LAKES, features);
		}

		private Layers structures(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.UNDERGROUND_STRUCTURES, features);
		}

		private Layers surfaceStructures(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.SURFACE_STRUCTURES, features);
		}

		private Layers ores(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.UNDERGROUND_ORES, features);
		}

		private Layers underground(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.UNDERGROUND_DECORATION, features);
		}

		private Layers springs(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.FLUID_SPRINGS, features);
		}

		private Layers vegetation(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.VEGETAL_DECORATION, features);
		}

		private Layers topLayer(List<ResourceKey<PlacedFeature>> features) {
			return add(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, features);
		}

		@Override
		public BiomeGenerationSettings build() {
			return builder.build();
		}
	}

	private static Layers layers(HolderGetter<PlacedFeature> pf, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
		return new Layers(pf, carvers);
	}

	/** {@code List.of} chains, for the places a biome takes a shared run and then adds its own to it. */
	@SafeVarargs
	private static <T> List<T> join(List<T>... lists) {
		return Arrays.stream(lists).flatMap(List::stream).toList();
	}

	private static ResourceKey<Biome> biome(String id) {
		return ResourceKey.create(Registries.BIOME, GensokyoLegacy.loc(id));
	}

	private static ResourceKey<BiomeModifier> biomeMod(String id) {
		return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GensokyoLegacy.loc(id));
	}

	private static BiomeGenerationSettings.Builder addDefaultOres(BiomeGenerationSettings.Builder builder) {
		BiomeDefaultFeatures.addDefaultOres(builder);
		return builder;
	}

	/**
	 * The one every surface and underground biome uses: water colour, fog colour and the two grass overrides.
	 * The special effects are otherwise ours, which is why temperature alone sets the sky.
	 */
	private static Biome biome(boolean hasPrecipitation, float temperature, float downfall,
							   int waterColor, int fogColor,
							   @Nullable Integer grassCol, @Nullable Integer foliageCol,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, waterColor, 329011, fogColor,
				null, grassCol, foliageCol, spawns, gen, bgm);
	}

	/** The same without the music, which is every biome here: Gensokyo's soundtrack is the dimension's business. */
	private static Biome biome(boolean hasPrecipitation, float temperature, float downfall,
							   int waterColor, int fogColor,
							   @Nullable Integer grassCol, @Nullable Integer foliageCol,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen
	) {
		return biome(hasPrecipitation, temperature, downfall, waterColor, 329011, fogColor,
				null, grassCol, foliageCol, spawns, gen, null);
	}

	/**
	 * The same with the sky colour spelled out, for {@link #SUNFLOWER_PLAINS} alone. Every other biome lets the
	 * temperature set the sky, which is why this one is the only caller: it is the biome that keeps vanilla's own
	 * numbers rather than being a Gensokyo biome that happens to be grassy.
	 */
	private static Biome biome(boolean hasPrecipitation, float temperature, float downfall,
							   int waterColor, int fogColor, int skyColor,
							   @Nullable Integer grassCol, @Nullable Integer foliageCol,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen
	) {
		return biome(hasPrecipitation, temperature, downfall, waterColor, 329011, fogColor,
				skyColor, grassCol, foliageCol, spawns, gen, null);
	}

	/** The end dimension, which is the one Gensokyo biome that is not Gensokyo at all. */
	private static Biome biome(int fogColor,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(false, 2, 0, fogColor, spawns, gen, bgm);
	}

	private static Biome biome(boolean hasPrecipitation, float temperature, float downfall, int fogColor,
							   MobSpawnSettings.Builder spawns,
							   BiomeGenerationSettings.PlainBuilder gen,
							   @Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, 4159204, 329011, fogColor,
				null, null, null, spawns, gen, bgm);
	}

	private static Biome biome(
			MobSpawnSettings.Builder spawns,
			BiomeGenerationSettings.PlainBuilder gen
	) {
		return biome(true, 0.7f, 0.8f, 0x4b6fad, 329011, 0x8facf3, 0x4770c1, 0x418e63, 0x4e9465, spawns, gen, null);
	}

	private static Biome biome(
			boolean hasPrecipitation, float temperature, float downfall,
			int fogColor, @Nullable Integer grassCol, @Nullable Integer foliageCol,
			MobSpawnSettings.Builder spawns,
			BiomeGenerationSettings.PlainBuilder gen,
			@Nullable Music bgm
	) {
		return biome(hasPrecipitation, temperature, downfall, 4159204, 329011, fogColor,
				null, grassCol, foliageCol, spawns, gen, bgm);
	}

	private static Biome biome(
			boolean hasPrecipitation, float temperature, float downfall,
			int waterColor, int waterFogColor, int fogColor,
			@Nullable Integer skyCol,
			@Nullable Integer grassCol, @Nullable Integer foliageCol,
			MobSpawnSettings.Builder spawns,
			BiomeGenerationSettings.PlainBuilder gen,
			@Nullable Music bgm
	) {
		BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
				.waterColor(waterColor)
				.waterFogColor(waterFogColor)
				.fogColor(fogColor);
		if (skyCol != null) {
			effects.skyColor(skyCol);
		} else {
			effects.skyColor(calculateSkyColor(temperature));
		}
		effects.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
				.backgroundMusic(bgm);
		if (grassCol != null) {
			effects.grassColorOverride(grassCol);
		}
		if (foliageCol != null) {
			effects.foliageColorOverride(foliageCol);
		}
		return new Biome.BiomeBuilder()
				.hasPrecipitation(hasPrecipitation)
				.temperature(temperature)
				.downfall(downfall)
				.specialEffects(effects.build())
				.mobSpawnSettings(spawns.build())
				.generationSettings(gen.build())
				.build();
	}

	/**
	 * The snow line, which nothing here is allowed to reach: {@link Biome#shouldSnow} and {@link Biome#shouldFreeze}
	 * both ask for {@code getTemperature(pos) >= 0.15}, and {@code getTemperature} subtracts up to 0.19 above y 80 for
	 * altitude. Every biome this class registers clears 0.15 after that.
	 */
	static final float coldEnoughToSnow = 0.15f;

	protected static int calculateSkyColor(float temperature) {
		float f = Mth.clamp(temperature / 3.0F, -1.0F, 1.0F);
		return Mth.hsvToRgb(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F);
	}

}
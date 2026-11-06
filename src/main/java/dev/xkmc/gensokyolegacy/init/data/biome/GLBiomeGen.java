package dev.xkmc.gensokyolegacy.init.data.biome;

import com.tterrag.registrate.providers.DataProviderInitializer;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.biomes.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.Tags;

/**
 * The one entry point for everything about the Gensokyo biomes: the list of the biomes themselves, and the tags on the
 * biome registry.
 * <p>
 * The tags used to sit beside the structure registry's own tags in {@code GLStructureTagGen}, which made one file
 * list every biome and every structure — so a branch adding a biome and a branch adding a structure both opened it.
 * The biome half is here now, and {@code GLStructureGen} is the only thing that reads both: it needs a biome tag to
 * place a structure and a structure key to put in it.
 * <p>
 * A biome still does not list itself in either half. {@code *BiomeGen.KEY} is where a biome says what it is, this
 * file says which categories it belongs to, and {@link AbstractBiomeGen#build} is what writes the biome out.
 *
 * @see "doc/design/gensokyo_biomes.md" §8
 */
public class GLBiomeGen {

	public static void init(DataProviderInitializer init) {
		AbstractBiomeGen.build(init,
				new GapBiomeGen(),
				new MagicalForestBiomeGen(), new SakuraForestBiomeGen(),
				new MuenzukaBiomeGen(), new HumanVillageBiomeGen(), new MyourenTempleBarrensBiomeGen(),
				new BambooForestOfTheLostBiomeGen(), new HillOfTheNamelessBiomeGen(),
				new UntroddenValleyBiomeGen(), new GenbuRavineBiomeGen(),
				new YoukaiMountainFoothillsBiomeGen(), new SanctuaryBiomeGen(),
				new HellOfBloodPoolsBiomeGen(), new AnimalRealmBiomeGen(),
				new HellOfBlazingFiresBiomeGen(), new FormerHellBiomeGen(),
				new MountainInteriorBiomeGen(),
				new MistyLakeBiomeGen(), new MistyLakeShoreBiomeGen(),
				new TenguSettlementBiomeGen(), new SecretHeavenCliffBiomeGen(), new YoukaiMountainSummitBiomeGen(),
				new SunflowerPlainsBiomeGen());
	}

	public static final ProviderType<RegistrateTagsProvider.Impl<Biome>> BIOME_TAG =
			ProviderType.registerDynamicTag("biome", "biome", Registries.BIOME);

	public static final TagKey<Biome> CIRNO_NEST = biomeTag("has_structure/cirno_nest");
	public static final TagKey<Biome> HAKUREI_SHRINE = biomeTag("has_structure/hakurei_shrine");
	public static final TagKey<Biome> MARISA_HOUSE = biomeTag("has_structure/marisa_house");
	public static final TagKey<Biome> MORICHIKA_SHOP = biomeTag("has_structure/morichika_shop");
	public static final TagKey<Biome> ALICE_HOUSE = biomeTag("has_structure/alice_house");
	public static final TagKey<Biome> PLAIN_FAIRY_SPAWN = biomeTag("has_spawn/plain_fairy");

	/** Our own copies of the overworld's biome categories, same names in our own namespace.
	 *
	 *  <p>Gensokyo biomes deliberately stay out of {@code minecraft:is_forest} and the rest. Those are shared
	 *  vocabulary: every mod that ships a forest biome writes to the same tag, so what is in them is a claim about
	 *  the biome that other mods read to decide their own behaviour — a mod testing {@code c:is_forest} for "put
	 *  mushrooms down" would start finding them on a cliff it cannot see. Ours are the same classification without
	 *  the side effect, and reading both pairs is fine, which is what {@link #PLAIN_FAIRY_SPAWN} does. */
	public static final TagKey<Biome> IS_MAGICAL = biomeTag("is_magical");
	public static final TagKey<Biome> IS_FOREST = biomeTag("is_forest");
	public static final TagKey<Biome> IS_PLAINS = biomeTag("is_plains");
	public static final TagKey<Biome> IS_MOUNTAIN = biomeTag("is_mountain");
	public static final TagKey<Biome> IS_MOUNTAIN_PEAK = biomeTag("is_mountain/peak");
	public static final TagKey<Biome> IS_UNDERGROUND = biomeTag("is_underground");
	public static final TagKey<Biome> IS_SPOOKY = biomeTag("is_spooky");
	public static final TagKey<Biome> IS_FLORAL = biomeTag("is_floral");
	public static final TagKey<Biome> IS_WET = biomeTag("is_wet");
	public static final TagKey<Biome> IS_BEACH = biomeTag("is_beach");
	public static final TagKey<Biome> IS_AQUATIC = biomeTag("is_aquatic");
	public static final TagKey<Biome> IS_DRY = biomeTag("is_dry");
	public static final TagKey<Biome> IS_TAIGA = biomeTag("is_taiga");

	/** Youkai's Feasts' own cherry tag, written by our datagen run: a tag provider writes to the namespace of
	 *  the tag it is handed, so this is the only way to reach a foreign tag without a hand-written file.
	 *  Spelled out rather than imported because that mod is compile-only. */
	public static final TagKey<Biome> YSF_CHERRY_BIOMES = TagKey.create(Registries.BIOME,
			ResourceLocation.fromNamespaceAndPath("youkaisfeasts", "cherry_biomes"));

	public static TagKey<Biome> biomeTag(String name) {
		return TagKey.create(Registries.BIOME, GensokyoLegacy.loc(name));
	}

	public static void genBiomeTag(RegistrateTagsProvider.Impl<Biome> pvd) {
		pvd.addTag(CIRNO_NEST)
				.add(Biomes.SNOWY_PLAINS)
				.add(Biomes.ICE_SPIKES)
				.add(Biomes.FROZEN_OCEAN)
				.add(Biomes.DEEP_FROZEN_OCEAN)
				.add(Biomes.GROVE)
				.add(Biomes.FROZEN_RIVER)
				.add(Biomes.SNOWY_TAIGA)
				.add(Biomes.SNOWY_BEACH);
		pvd.addTag(HAKUREI_SHRINE)
				.add(Biomes.CHERRY_GROVE)
				.add(SakuraForestBiomeGen.KEY)
				// Youkai's Feasts' own sakura forest, a shrine grove as much as ours. Optional, so that mod stays
				// optional, and only that one biome: its sakura taiga and birch forest are another stand of trees.
				.addOptional(ResourceLocation.parse("youkaisfeasts:sakura_forest"));
		// Our sakura forest is a cherry grove to Youkai's Feasts. "replace" stays false, which is the point: their
		// file lists three sakura biomes and the loader clears all if any pack replaces. Magical forest is not one.
		pvd.addTag(YSF_CHERRY_BIOMES).add(SakuraForestBiomeGen.KEY);
		pvd.addTag(MARISA_HOUSE)
				.add(MagicalForestBiomeGen.KEY);
		pvd.addTag(MORICHIKA_SHOP)
				.add(MagicalForestBiomeGen.KEY);
		pvd.addTag(ALICE_HOUSE)
				.add(MagicalForestBiomeGen.KEY);
		// the one place that reads both pairs. The modifier is not scoped to a dimension, so it has to reach the
		// vanilla overworld's forests and plains as well as Gensokyo's — nesting the shared tags costs nothing here,
		// because reading a tag is not a claim about it, and it is the only way the overworld keeps its fairies.
		pvd.addTag(PLAIN_FAIRY_SPAWN)
				.addTag(Tags.Biomes.IS_FOREST)
				.addTag(Tags.Biomes.IS_PLAINS)
				.addTag(IS_FOREST)
				.addTag(IS_PLAINS);
		pvd.addTag(IS_MAGICAL).add(MagicalForestBiomeGen.KEY).add(SakuraForestBiomeGen.KEY);
		pvd.addTag(IS_FOREST)
				.add(MagicalForestBiomeGen.KEY).add(SakuraForestBiomeGen.KEY)
				.add(MuenzukaBiomeGen.KEY).add(BambooForestOfTheLostBiomeGen.KEY)
				.add(UntroddenValleyBiomeGen.KEY).add(YoukaiMountainFoothillsBiomeGen.KEY)
				.add(TenguSettlementBiomeGen.KEY);
		pvd.addTag(IS_PLAINS)
				.add(HumanVillageBiomeGen.KEY).add(SunflowerPlainsBiomeGen.KEY)
				.add(HillOfTheNamelessBiomeGen.KEY);
		// the mountain band and the two fixed locations above it
		pvd.addTag(IS_MOUNTAIN)
				.add(UntroddenValleyBiomeGen.KEY).add(GenbuRavineBiomeGen.KEY)
				.add(YoukaiMountainFoothillsBiomeGen.KEY).add(SanctuaryBiomeGen.KEY)
				.add(TenguSettlementBiomeGen.KEY).add(SecretHeavenCliffBiomeGen.KEY)
				.add(YoukaiMountainSummitBiomeGen.KEY);
		pvd.addTag(IS_MOUNTAIN_PEAK).add(YoukaiMountainSummitBiomeGen.KEY);
		pvd.addTag(IS_UNDERGROUND)
				.add(HellOfBloodPoolsBiomeGen.KEY).add(AnimalRealmBiomeGen.KEY)
				.add(FormerHellBiomeGen.KEY).add(HellOfBlazingFiresBiomeGen.KEY)
				.add(MountainInteriorBiomeGen.KEY);
		pvd.addTag(IS_SPOOKY).add(MuenzukaBiomeGen.KEY).add(HillOfTheNamelessBiomeGen.KEY);
		pvd.addTag(IS_FLORAL).add(HumanVillageBiomeGen.KEY).add(SanctuaryBiomeGen.KEY)
				.add(HillOfTheNamelessBiomeGen.KEY);
		pvd.addTag(IS_WET).add(GenbuRavineBiomeGen.KEY)
				.add(MistyLakeBiomeGen.KEY).add(MistyLakeShoreBiomeGen.KEY);
		// the four fixed locations. Misty Lake is not tagged as an ocean: it is a freshwater lake, and anything that
		// branches on is_ocean would put oceans' own overrides on it
		pvd.addTag(IS_BEACH).add(MistyLakeShoreBiomeGen.KEY);
		pvd.addTag(IS_AQUATIC).add(MistyLakeBiomeGen.KEY);
		pvd.addTag(IS_DRY).add(MyourenTempleBarrensBiomeGen.KEY);
		pvd.addTag(IS_TAIGA).add(BambooForestOfTheLostBiomeGen.KEY)
				.add(UntroddenValleyBiomeGen.KEY);
	}

}
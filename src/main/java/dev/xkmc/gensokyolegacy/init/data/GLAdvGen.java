package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.GLBiomes;
import dev.xkmc.gensokyolegacy.init.registrate.GLCriteriaTriggers;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2core.serial.advancements.AdvancementGenerator;
import dev.xkmc.l2core.serial.advancements.CriterionBuilder;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

public class GLAdvGen {
	public static final ResourceLocation KOISHI_FIRST = GensokyoLegacy.loc("koishi_first");
	public static final ResourceLocation KOISHI_HAT = GensokyoLegacy.loc("koishi_hat");

	public static final ResourceLocation WELCOME = GensokyoLegacy.loc("main/welcome");
	public static final ResourceLocation ENTER_HAKUREI_SHRINE = GensokyoLegacy.loc("main/enter_hakurei_shrine");
	public static final ResourceLocation ENTER_MARISA_HOUSE = GensokyoLegacy.loc("main/enter_marisa_house");
	public static final ResourceLocation ENTER_MORICHIKA_SHOP = GensokyoLegacy.loc("main/enter_morichika_shop");

	public static final ResourceLocation ALCHEMY = GensokyoLegacy.loc("main/alchemy");
	public static final ResourceLocation OBTAIN_DOLL_GLOVE = GensokyoLegacy.loc("main/obtain_doll_glove");
	public static final ResourceLocation OBTAIN_TALISMAN = GensokyoLegacy.loc("main/obtain_talisman");
	public static final ResourceLocation OBTAIN_MINI_HAKKERO = GensokyoLegacy.loc("main/obtain_mini_hakkero");
	public static final ResourceLocation OBTAIN_CAT_BELL = GensokyoLegacy.loc("main/obtain_cat_bell");
	public static final ResourceLocation OBTAIN_CENTIPICKAXE = GensokyoLegacy.loc("main/obtain_centipickaxe");
	public static final ResourceLocation OBTAIN_DOWSER = GensokyoLegacy.loc("main/obtain_dowser");
	public static final ResourceLocation OBTAIN_MERMAID_PEARL = GensokyoLegacy.loc("main/obtain_mermaid_pearl");
	public static final ResourceLocation OBTAIN_SEALING_POT = GensokyoLegacy.loc("main/obtain_sealing_pot");
	public static final ResourceLocation OBTAIN_GAP_PORTAL = GensokyoLegacy.loc("main/obtain_gap_portal");
	public static final ResourceLocation OBTAIN_BORDER_UMBRELLA = GensokyoLegacy.loc("main/obtain_border_umbrella");

	public static void genAdv(RegistrateAdvancementProvider pvd) {
		pvd.accept(Advancement.Builder.advancement().addCriterion("koishi_first",
				GLCriteriaTriggers.KOISHI_FIRST.get().createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()))
		).build(KOISHI_FIRST));
		pvd.accept(Advancement.Builder.advancement().addCriterion("koishi_hat",
				GLCriteriaTriggers.KOISHI_HAT.get().createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()))
		).build(KOISHI_HAT));

		var gen = new AdvancementGenerator(pvd, GensokyoLegacy.MODID);
		var tab = gen.new TabBuilder("main");
		var root = tab.root("welcome", GLItems.BORDER_UMBRELLA.get(),
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.located(
						LocationPredicate.Builder.location().setBiomes(HolderSet.direct(
								resolveBiome(pvd, GLBiomes.MAGICAL_FOREST),
								resolveBiome(pvd, GLBiomes.SAKURA_FOREST),
								resolveBiome(pvd, GLDimensionGen.BIOME_GAP))))),
				"Welcome to Gensokyo", "Find your way in this new world");
		root.add((id, builder, conditions) -> builder.rewards(
				AdvancementRewards.Builder.loot(ResourceKey.create(Registries.LOOT_TABLE,
						GensokyoLegacy.loc("tools_guide"))).build()));
		var hakurei = root.create("enter_hakurei_shrine", Items.CHERRY_SAPLING,
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.located(
						LocationPredicate.Builder.inStructure(resolve(pvd, "hakurei_shrine")))),
				"Hakurei Shrine", "Enter the Hakurei Shrine");
		var marisaHouse = root.create("enter_marisa_house", Items.RED_MUSHROOM_BLOCK,
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.located(
						LocationPredicate.Builder.inStructure(resolve(pvd, "marisa_house")))),
				"Kirisame House", "Enter Marisa's house in the Magical Forest");
		var morichika = root.create("enter_morichika_shop", Items.EMERALD,
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.located(
						LocationPredicate.Builder.inStructure(resolve(pvd, "morichika_shop")))),
				"Kourindou", "Enter Morichika's shop in the Magical Forest");
		marisaHouse.create("alchemy", GLBlocks.ALCHEMY_POT.asItem(),
						CriterionBuilder.item(GLBlocks.ALCHEMY_POT.asItem()),
						"Alchemy Pot", "Obtain an Alchemy Pot")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_doll_glove", GLItems.DOLL_GLOVE.get(),
						CriterionBuilder.item(GLItems.DOLL_GLOVE.get()),
						"Doll Glove", "Obtain the Seven-Colored Doll Glove")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_sealing_pot", GLBlocks.SEALING_POT.asItem(),
						CriterionBuilder.item(GLBlocks.SEALING_POT.asItem()),
						"Sealing Pot", "Obtain a Sealing Pot")
				.type(AdvancementType.TASK, false, false, false);
		hakurei.create("obtain_talisman", GLTalismans.HEAL_TALISMAN.get(),
						CriterionBuilder.item(GLTagGen.TALISMAN),
						"Paper Talismans", "Obtain a paper talisman")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_gap_portal", GLBlocks.GAP_PORTAL.asItem(),
						CriterionBuilder.item(GLBlocks.GAP_PORTAL.asItem()),
						"Gap Portal", "Obtain a Gap Portal")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_border_umbrella", GLItems.BORDER_UMBRELLA.get(),
						CriterionBuilder.item(GLItems.BORDER_UMBRELLA.get()),
						"Border Umbrella", "Obtain a Border Umbrella")
				.type(AdvancementType.TASK, false, false, false);
		morichika.create("obtain_mini_hakkero", GLItems.MINI_FURNACE_1.get(),
						CriterionBuilder.item(GLItems.MINI_FURNACE_1.get()),
						"Mini Hakkero", "Obtain a Mini Hakkero Prototype")
				.type(AdvancementType.TASK, false, false, false);
		morichika.create("obtain_cat_bell", GLItems.CAT_BELL.get(),
						CriterionBuilder.item(GLItems.CAT_BELL.get()),
						"Cat Bell", "Obtain a Cat Bell")
				.type(AdvancementType.TASK, false, false, false);
		morichika.create("obtain_centipickaxe", GLItems.CENTIPICKAXE.get(),
						CriterionBuilder.item(GLItems.CENTIPICKAXE.get()),
						"Centipeck", "Obtain a Centipeck")
				.type(AdvancementType.TASK, false, false, false);
		morichika.create("obtain_dowser", GLItems.DOWSER.get(),
						CriterionBuilder.item(GLItems.DOWSER.get()),
						"Dowser", "Obtain Nazrin's Dowser")
				.type(AdvancementType.TASK, false, false, false);
		morichika.create("obtain_mermaid_pearl", GLItems.MERMAID_PEARL.get(),
						CriterionBuilder.item(GLItems.MERMAID_PEARL.get()),
						"Mermaid's Pearl", "Obtain a Mermaid's Pearl")
				.type(AdvancementType.TASK, false, false, false);
		root.finish();
	}

	private static Holder<Biome> resolveBiome(RegistrateAdvancementProvider pvd, ResourceKey<Biome> key) {
		return pvd.resolve(key);
	}

	private static Holder<Structure> resolve(RegistrateAdvancementProvider pvd, String id) {
		return pvd.resolve(ResourceKey.create(Registries.STRUCTURE, GensokyoLegacy.loc(id)));
	}

}

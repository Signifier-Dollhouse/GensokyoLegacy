package dev.xkmc.gensokyolegacy.init.data;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.content.item.talisman.core.GLTalismans;
import dev.xkmc.gensokyolegacy.content.trigger.FeedCharacterTrigger;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.biome.GLBiomes;
import dev.xkmc.gensokyolegacy.init.registrate.GLCriteriaTriggers;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2core.serial.advancements.AdvancementGenerator;
import dev.xkmc.l2core.serial.advancements.CriterionBuilder;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
	public static final ResourceLocation ENTER_ALICE_HOUSE = GensokyoLegacy.loc("main/enter_alice_house");

	public static final ResourceLocation ALCHEMY = GensokyoLegacy.loc("main/alchemy");
	public static final ResourceLocation OBTAIN_DOLL_GLOVE = GensokyoLegacy.loc("main/obtain_doll_glove");
	public static final ResourceLocation FEED_FAIRY = GensokyoLegacy.loc("main/feed_fairy");
	public static final ResourceLocation OBTAIN_TALISMAN = GensokyoLegacy.loc("main/obtain_talisman");
	public static final ResourceLocation OBTAIN_MINI_HAKKERO = GensokyoLegacy.loc("main/obtain_mini_hakkero");
	public static final ResourceLocation OBTAIN_MINI_HAKKERO_REPAIRED = GensokyoLegacy.loc("main/obtain_mini_hakkero_repaired");
	public static final ResourceLocation OBTAIN_STRANGE_GLASSES = GensokyoLegacy.loc("main/obtain_strange_glasses");
	public static final ResourceLocation OBTAIN_CAT_BELL = GensokyoLegacy.loc("main/obtain_cat_bell");
	public static final ResourceLocation OBTAIN_CENTIPICKAXE = GensokyoLegacy.loc("main/obtain_centipickaxe");
	public static final ResourceLocation OBTAIN_DOWSER = GensokyoLegacy.loc("main/obtain_dowser");
	public static final ResourceLocation OBTAIN_MERMAID_PEARL = GensokyoLegacy.loc("main/obtain_mermaid_pearl");
	public static final ResourceLocation OBTAIN_SEALING_POT = GensokyoLegacy.loc("main/obtain_sealing_pot");
	public static final ResourceLocation OBTAIN_GAP_PORTAL = GensokyoLegacy.loc("main/obtain_gap_portal");
	public static final ResourceLocation OBTAIN_BORDER_UMBRELLA = GensokyoLegacy.loc("main/obtain_border_umbrella");
	public static final ResourceLocation OBTAIN_BROOM = GensokyoLegacy.loc("main/obtain_broom");
	public static final ResourceLocation OBTAIN_IRON_DAGGER = GensokyoLegacy.loc("main/obtain_iron_dagger");
	public static final ResourceLocation OBTAIN_DAGGER_GLOVE = GensokyoLegacy.loc("main/obtain_dagger_glove");
	public static final ResourceLocation OBTAIN_FAIRY_ICE_CRYSTAL = GensokyoLegacy.loc("main/obtain_fairy_ice_crystal");
	public static final ResourceLocation OBTAIN_FROZEN_FROG = GensokyoLegacy.loc("main/obtain_frozen_frog");
	public static final ResourceLocation OBTAIN_WINGS = GensokyoLegacy.loc("main/obtain_wings");

	/**
	 * The criterion both feed advancements share, built off the entity type rather than the food: the dish is always
	 * whatever {@code FairyFeedModule} already accepted. <p> The entity half has to name every fairy that can be fed,
	 * since the trigger fires for whoever carries that module and the criterion would otherwise throw her out; Cirno is
	 * fed on the same two tiers, so both count here. A direct holder set rather than a tag, because the list is closed
	 * - one more entity for whoever next teaches a fairy to eat.
	 */
	private static Criterion<FeedCharacterTrigger.TriggerInstance> fedAFairy() {
		var fairies = HolderSet.direct(
				GLEntities.PLAIN_FAIRY.get().builtInRegistryHolder(),
				GLEntities.CIRNO.get().builtInRegistryHolder());
		return FeedCharacterTrigger.TriggerInstance.usedItem(GLCriteriaTriggers.FEED_FAIRY, null,
				EntityPredicate.Builder.entity().entityType(new EntityTypePredicate(fairies)).build(), null);
	}

	/**
	 * One of the items counts, which {@link CriterionBuilder#items} cannot express - it asks for all of them at once.
	 * Only for items that are variants of one thing, where which variant turned up is not the advancement's business.
	 */
	private static CriterionBuilder anyItem(Item... items) {
		var builder = CriterionBuilder.or();
		for (var item : items) {
			builder.add(InventoryChangeTrigger.TriggerInstance.hasItems(item));
		}
		return builder;
	}

	public static void genAdv(RegistrateAdvancementProvider pvd) {
		pvd.accept(Advancement.Builder.advancement().addCriterion("koishi_first",
				GLCriteriaTriggers.KOISHI_FIRST.get().createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()))
		).build(KOISHI_FIRST));
		pvd.accept(Advancement.Builder.advancement().addCriterion("koishi_hat",
				GLCriteriaTriggers.KOISHI_HAT.get().createCriterion(new PlayerTrigger.TriggerInstance(Optional.empty()))
		).build(KOISHI_HAT));

		var gen = new AdvancementGenerator(pvd, GensokyoLegacy.MODID);
		var tab = gen.new TabBuilder("main");
		// The biomes are the usual way in, but fairies also spawn outside them, so feeding
		// one already counts; OR rather than a second root, to keep the same root node.
		var root = tab.root("welcome", GLItems.BORDER_UMBRELLA.get(),
				CriterionBuilder.or()
						.add(PlayerTrigger.TriggerInstance.located(
								LocationPredicate.Builder.location().setBiomes(HolderSet.direct(
										resolveBiome(pvd, GLBiomes.MAGICAL_FOREST),
										resolveBiome(pvd, GLBiomes.SAKURA_FOREST),
										resolveBiome(pvd, GLDimensionGen.BIOME_GAP)))))
						.add(fedAFairy()),
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
		// Purple is the last of Alice's seven dyes, and the glove she weaves
		// them into hangs off this branch rather than off Marisa's alchemy pot.
		var aliceHouse = root.create("enter_alice_house", Items.PURPLE_DYE,
				CriterionBuilder.one(PlayerTrigger.TriggerInstance.located(
						LocationPredicate.Builder.inStructure(resolve(pvd, "alice_house")))),
				"Margatroid Cottage", "Enter Alice's house in the Magical Forest");
		// Entry.create() parents to the builder it is called on, so both hang off alchemy
		// itself - chaining them off one another would bury the broom under the sealing pot.
		var alchemy = marisaHouse.create("alchemy", GLBlocks.ALCHEMY_POT.asItem(),
						CriterionBuilder.item(GLBlocks.ALCHEMY_POT.asItem()),
						"Alchemy Pot", "Obtain an Alchemy Pot")
				.type(AdvancementType.TASK, false, false, false);
		alchemy.create("obtain_sealing_pot", GLBlocks.SEALING_POT.asItem(),
						CriterionBuilder.item(GLBlocks.SEALING_POT.asItem()),
						"Sealing Pot", "Obtain a Sealing Pot")
				.type(AdvancementType.TASK, false, false, false);
		alchemy.create("obtain_broom", GLItems.BROOM.get(),
						CriterionBuilder.item(GLItems.BROOM.get()),
						"Flying Broom", "Obtain a Flying Broom")
				.type(AdvancementType.TASK, false, false, false);
		aliceHouse.create("obtain_doll_glove", GLItems.DOLL_GLOVE.get(),
						CriterionBuilder.item(GLItems.DOLL_GLOVE.get()),
						"Doll Glove", "Obtain the Seven-Colored Doll Glove")
				.type(AdvancementType.TASK, false, false, false);
		// Hangs off the root rather than off one of the four houses: a fairy is found
		// in the woods, not behind a door, so there is no structure to hang it under.
		var feedFairy = root.create("feed_fairy", GLItems.FAIRY_CAKE.get(),
						CriterionBuilder.one(fedAFairy()),
						"Fairy Feeding", "Feed a fairy")
				.type(AdvancementType.TASK, true, true, false);
		// Feeding a Cirno a cake buys the crystal, and the crystal is what freezes a frog - so the frog hangs
		// under the crystal rather than beside it, and both of them under feeding, the step that needs a fairy.
		feedFairy.create("obtain_fairy_ice_crystal", GLItems.FAIRY_ICE_CRYSTAL.get(),
						CriterionBuilder.item(GLItems.FAIRY_ICE_CRYSTAL.get()),
						"Fairy Ice Crystal", "Obtain a Fairy Ice Crystal")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_frozen_frog", GLItems.FROZEN_FROG_TEMPERATE.get(),
						anyItem(GLItems.FROZEN_FROG_COLD.asItem(), GLItems.FROZEN_FROG_WARM.asItem(),
								GLItems.FROZEN_FROG_TEMPERATE.asItem()),
						"Frozen Frog", "Obtain a Frozen Frog")
				.type(AdvancementType.TASK, false, false, false);
		// Ice wings are cut from crystals and fairy wings from dust, so either one answers the same question
		// and there is nothing to hang the other under. The icon is the ice pair: it is the better one.
		feedFairy.create("obtain_wings", GLItems.ICE_WINGS.get(),
						anyItem(GLItems.ICE_WINGS.asItem(), GLItems.FAIRY_WINGS.asItem()),
						"Wings", "Obtain a pair of wings")
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
		// The repaired copy hangs off the prototype rather than continuing the
		// Morichika chain: it comes from Marisa's bench, not from his shelf.
		var miniHakkero = morichika.create("obtain_mini_hakkero", GLItems.MINI_HAKKERO_PROTOTYPE.get(),
						CriterionBuilder.item(GLItems.MINI_HAKKERO_PROTOTYPE.get()),
						"Mini Hakkero", "Obtain a Mini Hakkero Prototype")
				.type(AdvancementType.TASK, false, false, false);
		miniHakkero.create("obtain_mini_hakkero_repaired", GLItems.MINI_HAKKERO.get(),
						CriterionBuilder.item(GLItems.MINI_HAKKERO.get()),
						"Mini Hakkero [Repaired]", "Have Marisa repair your Mini Hakkero")
				.type(AdvancementType.TASK, false, false, false);
		// The bell hangs off the glasses rather than off the hakkero: it is
		// Morichika's stock too, and the glasses are what you wear to find it.
		morichika.create("obtain_strange_glasses", GLItems.STRANGE_GLASSES.get(),
						CriterionBuilder.item(GLItems.STRANGE_GLASSES.get()),
						"Strange Glasses", "Obtain the Strange Glasses")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_cat_bell", GLItems.CAT_BELL.get(),
						CriterionBuilder.item(GLItems.CAT_BELL.get()),
						"Cat Bell", "Obtain a Cat Bell")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_centipickaxe", GLItems.CENTIPICKAXE.get(),
						CriterionBuilder.item(GLItems.CENTIPICKAXE.get()),
						"Centipeck", "Obtain a Centipeck")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_dowser", GLItems.DOWSER.get(),
						CriterionBuilder.item(GLItems.DOWSER.get()),
						"Dowser", "Obtain Nazrin's Dowser")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_mermaid_pearl", GLItems.MERMAID_PEARL.get(),
						CriterionBuilder.item(GLItems.MERMAID_PEARL.get()),
						"Mermaid's Pearl", "Obtain a Mermaid's Pearl")
				.type(AdvancementType.TASK, false, false, false);
		morichika.create("obtain_iron_dagger", GLItems.IRON_DAGGER.get(),
						CriterionBuilder.item(GLItems.IRON_DAGGER.get()),
						"Iron Dagger", "Obtain an Iron Dagger")
				.type(AdvancementType.TASK, false, false, false)
				.create("obtain_dagger_glove", GLItems.DAGGER_GLOVE.get(),
						CriterionBuilder.item(GLItems.DAGGER_GLOVE.get()),
						"Dagger Glove", "Obtain a Dagger Glove")
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

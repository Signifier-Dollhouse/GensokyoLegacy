package dev.xkmc.gensokyolegacy.init;

import com.tterrag.registrate.providers.ProviderType;
import dev.xkmc.gensokyolegacy.compat.datagen.DataProviderInitializer;
import dev.xkmc.gensokyolegacy.compat.datagen.GLDatapackProvider;
import dev.xkmc.gensokyolegacy.compat.l2lib.GLRegistrate;
import dev.xkmc.gensokyolegacy.compat.l2lib.Reg;
import dev.xkmc.gensokyolegacy.content.attachment.party.food.BlockFoods;
import dev.xkmc.gensokyolegacy.content.cooldown.CooldownRates;
import dev.xkmc.gensokyolegacy.content.cooldown.LivelySource;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.YoukaiNodeEvaluatorRegistry;
import dev.xkmc.gensokyolegacy.content.entity.misc.FairyIce;
import dev.xkmc.gensokyolegacy.content.entity.misc.FrozenFrog;
import dev.xkmc.gensokyolegacy.content.entity.misc.HexBrewBottleEntity;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.ghost.YuyukoFanSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.spell.card.TouhouSpellCards;
import dev.xkmc.gensokyolegacy.event.GLAttackListener;
import dev.xkmc.gensokyolegacy.event.GLClickHandler;
import dev.xkmc.gensokyolegacy.init.data.*;
import dev.xkmc.gensokyolegacy.init.data.biome.GLBiomeGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GLBiomeModifierGen;
import dev.xkmc.gensokyolegacy.init.data.biome.GLOverworldRegion;
import dev.xkmc.gensokyolegacy.init.data.loot.GLGLMProvider;
import dev.xkmc.gensokyolegacy.init.data.loot.GiftLootGen;
import dev.xkmc.gensokyolegacy.init.data.rpg.*;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureLootGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureTagGen;
import dev.xkmc.gensokyolegacy.init.data.structure.ReportBlocksInStructure;
import dev.xkmc.gensokyolegacy.init.registrate.*;
import dev.xkmc.gensokyolegacy.mixin.ItemAccessor;
import dev.xkmc.gensokyolegacy.util.ProjectileDispenser;
import dev.xkmc.l2damagetracker.contents.attack.AttackEventHandler;
import dev.xkmc.l2library.capability.entity.GeneralCapabilityHolder;
import dev.xkmc.l2library.compat.patchouli.PatchouliHelper;
import dev.xkmc.l2library.serial.config.PacketHandlerWithConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrablender.api.Regions;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.Set;

@Mod(GensokyoLegacy.MODID)
@EventBusSubscriber(modid = GensokyoLegacy.MODID, bus = EventBusSubscriber.Bus.MOD)
public class GensokyoLegacy {

	public static final Logger LOGGER = LogManager.getLogger();

	public static final String MODID = "gensokyolegacy";
	public static final PacketHandlerWithConfig HANDLER = GLPackets.create(4);
	public static final Reg REG = new Reg(MODID, HANDLER);
	public static final GLRegistrate REGISTRATE = new GLRegistrate(MODID);

	public GensokyoLegacy() {

		GLItems.register();
		GLEntities.register();
		CodecRegistry.register();

		GLRecipes.register();
		GLMeta.register();
		GLMisc.register();
		GLWorldGen.register();
		GLBrains.register();
		YoukaiNodeEvaluatorRegistry.init();
		GLSounds.register();
		GLCriteriaTriggers.register();
		GLModConfig.init();
		TouhouSpellCards.registerSpells();
		if (ModList.get().isLoaded(PatchouliAPI.MOD_ID)) {
			new PatchouliHelper(REGISTRATE, "tools_guide")
					.buildModel("guide_book").buildShapelessRecipe(e -> e
									.requires(Items.BOOK).requires(GLItems.MAGIC_BOOK.get()),
							() -> Items.BOOK)
					.buildBook("Gensokyo Tools Guide",
							"This volume documents the working side of Gensokyo Legacy: brewing in the Alchemy Pot, paper talismans, combat dolls and their glove, special and everyday tools, and the decoration blocks you can sit on, cover and slide.",
							1, GLItems.TAB.key());
		}

		new GLClickHandler(loc("main"));
		AttackEventHandler.register(1765, new GLAttackListener());

		GLBiomeGen.register();
		GLStructureTagGen.register();
	}

	/**
	 * L2Library only attaches entity capabilities on its own; chunks and levels need the same
	 * pass so {@code AttVal.type().getOrCreate(...)} finds a provider.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static void attach(AttachCapabilitiesEvent<?> event) {
		if (!(event.getObject() instanceof ICapabilityProvider obj)) return;
		for (GeneralCapabilityHolder holder : GeneralCapabilityHolder.INTERNAL_MAP.values()) {
			if (holder.entity_class.isInstance(obj)) {
				((AttachCapabilitiesEvent) event).addCapability(holder.id, holder.generateSerializer(obj));
			}
		}
	}

	@SubscribeEvent
	public static void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			BlockFoods.register();
			BorderUmbrellaSelectionListener.register();
			DollGloveSelectionListener.register();
			DaggerGloveSelectionListener.register();
			YuyukoFanSelectionListener.register();
			AnchorSelectionListener.register();
			CooldownRates.register(new LivelySource());

			ProjectileDispenser.register(GLItems.FROZEN_FROG_COLD.get(), ProjectileDispenser.thrown(FrozenFrog::new));
			ProjectileDispenser.register(GLItems.FROZEN_FROG_WARM.get(), ProjectileDispenser.thrown(FrozenFrog::new));
			ProjectileDispenser.register(GLItems.FROZEN_FROG_TEMPERATE.get(), ProjectileDispenser.thrown(FrozenFrog::new));
			ProjectileDispenser.register(GLItems.FAIRY_ICE_CRYSTAL.get(), ProjectileDispenser.thrown(FairyIce::new));
			ProjectileDispenser.register(HexBrew.EXPLOSIVE_HEXBREW.bottle.get(),
					ProjectileDispenser.thrown(HexBrewBottleEntity::new));
			ProjectileDispenser.register(HexBrew.MIASMA_HEXBREW.bottle.get(),
					ProjectileDispenser.thrown(HexBrewBottleEntity::new));
			ProjectileDispenser.register(HexBrew.REFINED_EXPLOSIVE_HEXBREW.bottle.get(),
					ProjectileDispenser.thrown(HexBrewBottleEntity::new));
			ProjectileDispenser.register(HexBrew.REFINED_MIASMA_HEXBREW.bottle.get(),
					ProjectileDispenser.thrown(HexBrewBottleEntity::new));

			((ItemAccessor) Items.POTION).setCraftingRemainingItem(Items.GLASS_BOTTLE);
			((ItemAccessor) Items.DRAGON_BREATH).setCraftingRemainingItem(Items.GLASS_BOTTLE);

			Regions.register(new GLOverworldRegion(GLModConfig.COMMON.regionWeight.get()));
		});
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void gatherData(GatherDataEvent event) {
		REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, GLTagGen::onBlockTagGen);
		REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, GLTagGen::onItemTagGen);
		REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, GLTagGen::onEntityTagGen);
		REGISTRATE.addDataGenerator(GLBiomeGen.BIOME_TAG, GLBiomeGen::genBiomeTag);
		REGISTRATE.addDataGenerator(GLStructureTagGen.STRUCTURE_TAG, GLStructureTagGen::genStructureTag);
		REGISTRATE.addDataGenerator(ProviderType.RECIPE, GLRecipeGen::genRecipe);
		REGISTRATE.addDataGenerator(ProviderType.LOOT, GLStructureLootGen::genLoot);
		REGISTRATE.addDataGenerator(ProviderType.LOOT, GiftLootGen::genLoot);
		REGISTRATE.addDataGenerator(ProviderType.ADVANCEMENT, GLAdvGen::genAdv);
		REGISTRATE.addDataGenerator(ProviderType.LANG, GLLang::genLang);
		var gen = event.getGenerator();
		var out = gen.getPackOutput();
		var helper = event.getExistingFileHelper();
		var init = new DataProviderInitializer();
		GLDimensionGen.init(init);
		GLBiomeGen.init(init);
		GLBiomeModifierGen.init(init);
		GLStructureGen.init(init);
		new GLDamageTypes(out, event.getLookupProvider(), helper, MODID)
				.generate(event.includeServer(), gen);

		var reimu = new ReimuQDGen();
		var marisa = new MarisaQDGen();
		var morichika = new MorichikaQDGen();
		var alice = new AliceQDGen();
		var sakuya = new SakuyaQDGen();
		var suika = new SuikaQDGen();
		var cirno = new CirnoQDGen();
		var yuyuko = new YuyukoQDGen();
		var minamitsu = new MinamitsuQDGen();
		var gift = new GiftQDGen();
		QuestDialogData.build(init,
				reimu, marisa, morichika, alice, sakuya, suika, cirno, yuyuko, minamitsu, gift);
		gen.addProvider(event.includeServer(), new GLConfigGen(gen,
				reimu, marisa, morichika, alice, sakuya, suika, cirno, yuyuko, minamitsu, gift));

		gen.addProvider(event.includeServer(), new GLGLMProvider(out));
		gen.addProvider(event.includeServer(), new GLSlotGen(out, helper, event.getLookupProvider()));

		var dynamic = new GLDatapackProvider("Gensokyo Legacy Registries", out, event.getLookupProvider(),
				init.builder(), Set.of(MODID, "minecraft"));
		// the dynamic-tag providers are created by Registrate itself (one per registered ProviderType); the
		// addDataGenerator callbacks above run inside their addTags. Validation reads the vanilla lookup, so
		// mod-namespace references resolve through --existing against src/generated/resources instead.
		gen.addProvider(event.includeServer(), dynamic);

		ReportBlocksInStructure.report();
	}

	public static ResourceLocation loc(String id) {
		return new ResourceLocation(MODID, id);
	}
}

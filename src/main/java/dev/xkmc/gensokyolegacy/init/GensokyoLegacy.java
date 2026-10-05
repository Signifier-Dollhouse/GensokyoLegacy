package dev.xkmc.gensokyolegacy.init;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.tterrag.registrate.providers.ProviderType;
import dev.xkmc.gensokyolegacy.compat.touhoulittlemaid.TLMCompat;
import dev.xkmc.gensokyolegacy.compat.touhoulittlemaid.TouhouSpellCards;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.YoukaiNodeEvaluatorRegistry;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrewWrapper;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.event.GLAttackListener;
import dev.xkmc.gensokyolegacy.event.GLClickHandler;
import dev.xkmc.gensokyolegacy.init.data.*;
import dev.xkmc.gensokyolegacy.init.data.biome.GLBiomes;
import dev.xkmc.gensokyolegacy.init.data.biome.GLFeatureGen;
import dev.xkmc.gensokyolegacy.init.data.biome.MagicalForestRegion;
import dev.xkmc.gensokyolegacy.init.data.loot.GLGLMProvider;
import dev.xkmc.gensokyolegacy.init.data.rpg.AliceQDGen;
import dev.xkmc.gensokyolegacy.init.data.rpg.MarisaQDGen;
import dev.xkmc.gensokyolegacy.init.data.rpg.MorichikaQDGen;
import dev.xkmc.gensokyolegacy.init.data.rpg.QuestDialogData;
import dev.xkmc.gensokyolegacy.init.data.rpg.ReimuQDGen;
import dev.xkmc.gensokyolegacy.init.data.rpg.SakuyaQDGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureLootGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureTagGen;
import dev.xkmc.gensokyolegacy.init.data.structure.ReportBlocksInStructure;
import dev.xkmc.gensokyolegacy.init.registrate.*;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.gensokyolegacy.mixin.ItemAccessor;
import dev.xkmc.l2core.compat.patchouli.PatchouliHelper;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import dev.xkmc.l2core.init.reg.simple.Reg;
import dev.xkmc.l2damagetracker.contents.attack.AttackEventHandler;
import dev.xkmc.l2serial.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import terrablender.api.Regions;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.Arrays;

@Mod(GensokyoLegacy.MODID)
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GensokyoLegacy {

	public static final Logger LOGGER = LogManager.getLogger();

	public static final String MODID = "gensokyolegacy";
	public static final Reg REG = new Reg(MODID);
	public static final L2Registrate REGISTRATE = new L2Registrate(MODID);
	public static final PacketHandler HANDLER = GLPackets.create(3);

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
							"This volume documents the working side of Gensokyo Legacy: brewing in the Alchemy Pot, paper talismans, combat dolls and their glove, special and everyday tools, and the decoration blocks you can sit on, cover and slide.$(br2)Open it from any Patchouli guide book. An entry shows $(bold)what it does$(), $(bold)how to use it$() and $(bold)where it comes from$().",
							1, GLItems.TAB.key());
		}

		new GLClickHandler(loc("main"));
		AttackEventHandler.register(1765, new GLAttackListener());
		if (ModList.get().isLoaded(TouhouLittleMaid.MOD_ID)) {
			NeoForge.EVENT_BUS.register(TLMCompat.class);
		}
		BorderUmbrellaSelectionListener.register();
		DollGloveSelectionListener.register();
		DaggerGloveSelectionListener.register();
	}

	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.ItemHandler.BLOCK,
				GLBlocks.ALCHEMY_POT_BE.get(),
				(be, dir) -> be.getItemCap(dir));
		event.registerBlockEntity(
				Capabilities.FluidHandler.BLOCK,
				GLBlocks.ALCHEMY_POT_BE.get(),
				(be, dir) -> be.getTankCap(dir));
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new HexBrewWrapper(stack),
				Items.GLASS_BOTTLE);
		var hexBottles = Arrays.stream(HexBrew.values()).map(e -> e.bottle.asItem()).toArray(Item[]::new);
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ctx) -> new HexBrewWrapper(stack),
				hexBottles);
	}

	@SubscribeEvent
	public static void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			DispenserBlock.registerProjectileBehavior(GLItems.FROZEN_FROG_COLD.get());
			DispenserBlock.registerProjectileBehavior(GLItems.FROZEN_FROG_WARM.get());
			DispenserBlock.registerProjectileBehavior(GLItems.FROZEN_FROG_TEMPERATE.get());
			DispenserBlock.registerProjectileBehavior(GLItems.FAIRY_ICE_CRYSTAL.get());
			DispenserBlock.registerProjectileBehavior(HexBrew.EXPLOSIVE_HEXBREW.bottle.get());
			DispenserBlock.registerProjectileBehavior(HexBrew.MIASMA_HEXBREW.bottle.get());

			((ItemAccessor) Items.POTION).setCraftingRemainingItem(Items.GLASS_BOTTLE);
			((ItemAccessor) Items.DRAGON_BREATH).setCraftingRemainingItem(Items.GLASS_BOTTLE);

			Regions.register(new MagicalForestRegion(GLModConfig.COMMON.regionWeight.get()));
		});
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void gatherData(GatherDataEvent event) {
		REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, GLTagGen::onBlockTagGen);
		REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, GLTagGen::onItemTagGen);
		REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, GLTagGen::onEntityTagGen);
		REGISTRATE.addDataGenerator(GLStructureTagGen.BIOME_TAG, GLStructureTagGen::genBiomeTag);
		REGISTRATE.addDataGenerator(ProviderType.DATA_MAP, GLDataMapGen::dataMapGen);
		REGISTRATE.addDataGenerator(ProviderType.RECIPE, GLRecipeGen::genRecipe);
		REGISTRATE.addDataGenerator(ProviderType.LOOT, GLStructureLootGen::genLoot);
		REGISTRATE.addDataGenerator(ProviderType.ADVANCEMENT, GLAdvGen::genAdv);
		REGISTRATE.addDataGenerator(ProviderType.LANG, GLLang::genLang);
		var init = REGISTRATE.getDataGenInitializer();
		init.addDependency(GLStructureTagGen.BIOME_TAG, ProviderType.DYNAMIC);
		init.addDependency(ProviderType.ADVANCEMENT, ProviderType.DYNAMIC);
		GLDimensionGen.init(init);
		GLBiomes.init(init);
		GLStructureGen.init(init);
		GLFeatureGen.init(init);
		new GLDamageTypes(REGISTRATE).generate();

		var gen = event.getGenerator();
		var out = gen.getPackOutput();
		var helper = event.getExistingFileHelper();
		var pvd = event.getLookupProvider();
		gen.addProvider(event.includeServer(), new GLGLMProvider(out, pvd));
		gen.addProvider(event.includeServer(), new GLSlotGen(out, helper, pvd));

		var reimu = new ReimuQDGen();
		var marisa = new MarisaQDGen();
		var morichika = new MorichikaQDGen();
		var alice = new AliceQDGen();
		var sakuya = new SakuyaQDGen();
		QuestDialogData.build(REGISTRATE, reimu, marisa, morichika, alice, sakuya);

		ReportBlocksInStructure.report();
	}

	public static ResourceLocation loc(String id) {
		return ResourceLocation.fromNamespaceAndPath(MODID, id);
	}
}

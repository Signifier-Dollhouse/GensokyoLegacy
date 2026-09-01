package dev.xkmc.gensokyolegacy.init;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.tterrag.registrate.providers.ProviderType;
import dev.xkmc.gensokyolegacy.compat.touhoulittlemaid.TLMCompat;
import dev.xkmc.gensokyolegacy.compat.touhoulittlemaid.TouhouSpellCards;
import dev.xkmc.gensokyolegacy.content.attachment.character.CharDataToClient;
import dev.xkmc.gensokyolegacy.content.attachment.misc.FrogSyncPacket;
import dev.xkmc.gensokyolegacy.content.attachment.misc.KoishiStartPacket;
import dev.xkmc.gensokyolegacy.content.client.debug.BlockInfoToClient;
import dev.xkmc.gensokyolegacy.content.client.debug.BlockRequestToServer;
import dev.xkmc.gensokyolegacy.content.client.debug.CharacterInfoToClient;
import dev.xkmc.gensokyolegacy.content.client.debug.CharacterRequestToServer;
import dev.xkmc.gensokyolegacy.content.client.debug.DoorRequestToServer;
import dev.xkmc.gensokyolegacy.content.client.structure.*;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.PathDataToClient;
import dev.xkmc.gensokyolegacy.content.entity.behavior.move.YoukaiNodeEvaluatorRegistry;
import dev.xkmc.gensokyolegacy.content.entity.foundation.CombatToClient;
import dev.xkmc.gensokyolegacy.content.item.character.TouhouMat;
import dev.xkmc.gensokyolegacy.content.item.tool.CatBell;
import dev.xkmc.gensokyolegacy.content.item.tool.Dowser;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaAnvilHandler;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaSelectionListener;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaDeletePacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaOpenRenamePacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaRenamePacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaReorderPacket;
import dev.xkmc.gensokyolegacy.content.item.umbrella.network.BorderUmbrellaSelectPacket;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectSyncPacket;
import dev.xkmc.gensokyolegacy.content.rpg.core.CodecRegistry;
import dev.xkmc.gensokyolegacy.content.rpg.network.QuestStatusToClient;
import dev.xkmc.gensokyolegacy.content.rpg.network.TradeStatusToClient;
import dev.xkmc.gensokyolegacy.event.GLAttackListener;
import dev.xkmc.gensokyolegacy.event.GLClickHandler;
import dev.xkmc.gensokyolegacy.init.data.*;
import dev.xkmc.gensokyolegacy.init.data.loot.GLGLMProvider;
import dev.xkmc.gensokyolegacy.init.data.rpg.ReimuQDGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureLootGen;
import dev.xkmc.gensokyolegacy.init.data.structure.GLStructureTagGen;
import dev.xkmc.gensokyolegacy.init.data.structure.ReportBlocksInStructure;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrew;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.HexBrewWrapper;
import dev.xkmc.gensokyolegacy.init.registrate.*;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import dev.xkmc.l2core.init.reg.simple.Reg;
import dev.xkmc.l2damagetracker.contents.attack.AttackEventHandler;
import dev.xkmc.l2serial.network.PacketHandler;
import dev.xkmc.l2serial.serialization.custom_handler.CodecHandler;
import dev.xkmc.l2serial.serialization.custom_handler.Handlers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluid;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GensokyoLegacy.MODID)
@EventBusSubscriber(modid = GensokyoLegacy.MODID)
public class GensokyoLegacy {

	public static final Logger LOGGER = LogManager.getLogger();

	public static final String MODID = "gensokyolegacy";
	public static final Reg REG = new Reg(MODID);
	public static final L2Registrate REGISTRATE = new L2Registrate(MODID);
	public static final PacketHandler HANDLER = new PacketHandler(MODID, 1,
			e -> e.create(CharDataToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(PathDataToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(BlockRequestToServer.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(BlockInfoToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(CharacterRequestToServer.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(DoorRequestToServer.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(CharacterInfoToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(StructureBoundUpdateToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(CustomStructureBoundUpdateToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(StructureInfoRequestToServer.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(StructureInfoUpdateToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(StructureRepairToServer.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(StructureEditToServer.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(FrogSyncPacket.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(KoishiStartPacket.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(CombatToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(AreaEffectSyncPacket.class, PacketHandler.NetDir.PLAY_TO_CLIENT),

			e -> e.create(Dowser.DowserToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(CatBell.MountToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(QuestStatusToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(TradeStatusToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT),

			e -> e.create(BorderUmbrellaSelectPacket.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(BorderUmbrellaRenamePacket.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(BorderUmbrellaDeletePacket.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(BorderUmbrellaReorderPacket.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(BorderUmbrellaOpenRenamePacket.class, PacketHandler.NetDir.PLAY_TO_CLIENT)
	);

	public GensokyoLegacy() {
		Handlers.enableVanilla(Fluid.class, BuiltInRegistries.FLUID);
		new CodecHandler<>(FluidIngredient.class, FluidIngredient.CODEC, FluidIngredient.STREAM_CODEC);

		GLDecoBlocks.register();
		GLItems.register();
		GLEffects.register();
		GLFluids.register();
		GLEntities.register();
		CodecRegistry.register();

		GLRecipes.register();
		TouhouMat.register();
		GLMeta.register();
		GLMisc.register();
		GLWorldGen.register();
		GLBrains.register();
		YoukaiNodeEvaluatorRegistry.init();
		GLSounds.register();
		GLCriteriaTriggers.register();
		GLModConfig.init();
		TouhouSpellCards.registerSpells();

		new GLClickHandler(loc("main"));
		AttackEventHandler.register(1765, new GLAttackListener());
		if (ModList.get().isLoaded(TouhouLittleMaid.MOD_ID)) {
			NeoForge.EVENT_BUS.register(TLMCompat.class);
		}
		BorderUmbrellaSelectionListener.register();
		NeoForge.EVENT_BUS.register(BorderUmbrellaAnvilHandler.class);
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
		});
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void gatherData(GatherDataEvent event) {
		REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, GLTagGen::onBlockTagGen);
		REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, GLTagGen::onItemTagGen);
		REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, GLTagGen::onEntityTagGen);
		REGISTRATE.addDataGenerator(GLStructureTagGen.BIOME_TAG, GLStructureTagGen::genBiomeTag);
		REGISTRATE.addDataGenerator(ProviderType.DATA_MAP, GLDataMapGen::dataMapGen);
		REGISTRATE.addDataGenerator(ProviderType.LANG, GLLang::genLang);
		REGISTRATE.addDataGenerator(ProviderType.RECIPE, GLRecipeGen::genRecipe);
		REGISTRATE.addDataGenerator(ProviderType.LOOT, GLStructureLootGen::genLoot);
		REGISTRATE.addDataGenerator(ProviderType.ADVANCEMENT, GLAdvGen::genAdv);
		var init = REGISTRATE.getDataGenInitializer();
		GLDimensionGen.init(init);
		GLStructureGen.init(init);
		GLFeatureGen.init(init);
		new GLDamageTypes(REGISTRATE).generate();

		var gen = event.getGenerator();
		gen.addProvider(event.includeServer(), new GLGLMProvider(gen.getPackOutput(), event.getLookupProvider()));

		new ReimuQDGen();

		ReportBlocksInStructure.report();
	}

	public static ResourceLocation loc(String id) {
		return ResourceLocation.fromNamespaceAndPath(MODID, id);
	}
}

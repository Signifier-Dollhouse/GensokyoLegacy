package dev.xkmc.gensokyolegacy.init;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderHelper;
import dev.xkmc.gensokyolegacy.compat.touhoulittlemaid.TLMRenderHandler;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.overlay.AlchemyHintOverlay;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.overlay.TileClientTooltip;
import dev.xkmc.gensokyolegacy.content.block.functional.alchemypot.overlay.TileTooltip;
import dev.xkmc.gensokyolegacy.content.client.debug.DebugOverlay;
import dev.xkmc.gensokyolegacy.content.client.model.*;
import dev.xkmc.gensokyolegacy.content.entity.characters.rumia.BlackBallModel;
import dev.xkmc.gensokyolegacy.content.entity.characters.rumia.RumiaModel;
import dev.xkmc.gensokyolegacy.content.entity.dolls.client.HeadPossession;
import dev.xkmc.gensokyolegacy.content.item.common.ClientInvTooltip;
import dev.xkmc.gensokyolegacy.content.item.common.InvTooltip;
import dev.xkmc.gensokyolegacy.content.item.common.client.GloveTargetCache;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.content.item.dagger.DaggerGloveItem;
import dev.xkmc.gensokyolegacy.content.item.ghost.YuyukoFanItem;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.client.DollAttackStatusOverlay;
import dev.xkmc.gensokyolegacy.content.item.glove.client.DollClientLoadoutTooltip;
import dev.xkmc.gensokyolegacy.content.item.glove.client.DollGloveOverlay;
import dev.xkmc.gensokyolegacy.content.item.glove.client.DollLoadoutTooltip;
import dev.xkmc.gensokyolegacy.content.item.hakkero.ClientHakkeroTooltip;
import dev.xkmc.gensokyolegacy.content.item.hakkero.HakkeroTooltip;
import dev.xkmc.gensokyolegacy.content.item.umbrella.BorderUmbrellaItem;
import dev.xkmc.gensokyolegacy.content.ui.quest.QuestOverlay;
import dev.xkmc.gensokyolegacy.content.ui.quest.QuestTab;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2tabs.tabs.core.TabRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.FrogRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@EventBusSubscriber(value = Dist.CLIENT, modid = GensokyoLegacy.MODID, bus = EventBusSubscriber.Bus.MOD)
public class GLClient {

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
		if (ModList.get().isLoaded(TouhouLittleMaid.MOD_ID)) {
			MinecraftForge.EVENT_BUS.register(TLMRenderHandler.class);
		}

		MinecraftForge.EVENT_BUS.register(GLClient.class);
		event.enqueueWork(() -> {
			QuestTab.TOKEN = TabRegistry.registerTab(3750, QuestTab::new, () -> Items.BOOK, GLLang.Quest.TAB.get());
			ItemProperties.register(GLItems.BORDER_UMBRELLA.get(), GensokyoLegacy.loc("umbrella_open"), BorderUmbrellaItem::isOpen);
			ItemProperties.register(GLItems.BORDER_UMBRELLA.get(), GensokyoLegacy.loc("umbrella_display"), BorderUmbrellaItem::displayPredicate);
			ItemProperties.register(GLItems.DOLL_GLOVE.get(), GensokyoLegacy.loc("glove_display"), DollGloveItem::displayPredicate);
			ItemProperties.register(GLItems.DAGGER_GLOVE.get(), GensokyoLegacy.loc("dagger_glove_display"), DaggerGloveItem::displayPredicate);
			ItemProperties.register(GLItems.MINAMITSU_ANCHOR.get(), GensokyoLegacy.loc("anchor_display"), AnchorItem::displayPredicate);
			ItemProperties.register(GLItems.YUYUKO_FAN.get(), GensokyoLegacy.loc("yuyuko_fan_mode"), YuyukoFanItem::modePredicate);
			ItemProperties.register(GLItems.YUYUKO_FAN.get(), GensokyoLegacy.loc("yuyuko_fan_display"), YuyukoFanItem::displayPredicate);

			CuriosRendererRegistry.register(GLItems.STRAW_HAT.get(), TouhouHatRenderer::new);
			CuriosRendererRegistry.register(GLItems.SUWAKO_HAT.get(), TouhouHatRenderer::new);
			CuriosRendererRegistry.register(GLItems.KOISHI_HAT.get(), TouhouHatRenderer::new);
			CuriosRendererRegistry.register(GLItems.STRANGE_GLASSES.get(), StrangeGlassesRenderer::new);

			GLItems.STAR.get().getTypeForRender();
			GLItems.PINK_BUTTERFLY.get().getTypeForRender();
			GLItems.BLUE_BUTTERFLY.get().getTypeForRender();
			GLItems.IRON_DAGGER.get().getTypeForRender();
			GLItems.NUE_TRIDENT.get().getTypeForRender();
			GLItems.MINAMITSU_ANCHOR.get().getTypeForRender();
			GLItems.ANCHOR_WET.get().getTypeForRender();
			GLItems.DROPLET.get().getTypeForRender();
			GLItems.COIN.get().getTypeForRender();
			ProjectileRenderHelper.setup();
		});
	}

	@SubscribeEvent
	public static void clientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		// the shared glove target cache (glove.md §2) traces for whichever gloves are held,
		// so it is driven from one place rather than from every glove's inventoryTick
		GloveTargetCache.tick();
		// after the level tick, so the head under a possessed camera reports the frame it
		// just flew rather than the keys it has not pressed yet
		HeadPossession.tick();
	}

	@SubscribeEvent
	public static void clientTickPre(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		// before the level tick, so the head flies on this frame's keys rather than last frame's
		HeadPossession.readInput();
	}

	@SubscribeEvent
	public static void addGuiLayer(RegisterGuiOverlaysEvent event) {
		event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "debug", new DebugOverlay());
		event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "quest", new QuestOverlay());
		event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "alchemy_hint", new AlchemyHintOverlay());
		event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "doll_glove", new DollGloveOverlay());
		event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "doll_attack_status", new DollAttackStatusOverlay());
	}

	@SubscribeEvent
	public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
		event.register(TileTooltip.class, TileClientTooltip::new);
		event.register(InvTooltip.class, ClientInvTooltip::new);
		event.register(DollLoadoutTooltip.class, DollClientLoadoutTooltip::new);
		event.register(HakkeroTooltip.class, ClientHakkeroTooltip::new);
	}

	@SubscribeEvent
	public static void addDeco(RegisterItemDecorationsEvent event) {
		//event.register(GLItems.MINI_HAKKERO_PROTOTYPE.get(), new HakkeroPrototypeDeco());
	}

	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(RumiaModel.LAYER_LOCATION, RumiaModel::createBodyLayer);
		event.registerLayerDefinition(BlackBallModel.LAYER_LOCATION, BlackBallModel::createBodyLayer);
		event.registerLayerDefinition(SuwakoHatModel.SUWAKO, SuwakoHatModel::createSuwakoHat);
		event.registerLayerDefinition(SuwakoHatModel.STRAW, SuwakoHatModel::createStrawHat);
		event.registerLayerDefinition(FrogStrawHatModel.STRAW, FrogStrawHatModel::createHat);
	}

	@SubscribeEvent
	public static void addLayer(EntityRenderersEvent.AddLayers event) {
		if (ModList.get().isLoaded(TouhouLittleMaid.MOD_ID)) {
			TLMRenderHandler.addLayers(event);
		}
		if (event.getRenderer(EntityType.FROG) instanceof FrogRenderer r) {
			r.addLayer(new FrogHatLayer<>(r, event.getEntityModels()));
		}
		for (var skin : event.getSkins()) {
			var renderer = event.getSkin(skin);
			addCharacterLayers(renderer);
		}
		for (var type : ForgeRegistries.ENTITY_TYPES.getValues()) {
			if (type.getCategory() == MobCategory.MISC) continue;
			addCharacterLayers(event.getRenderer((EntityType) type));
		}
	}

	/**
	 * The two things a humanoid wears that vanilla has no slot renderer for. Both are swept over every entity type and
	 * every player skin, so a player picks them up without a hook of its own - the wings in particular have to be an
	 * entity layer rather than a curios one, since the back slot has no renderer to register.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void addCharacterLayers(EntityRenderer<?> renderer) {
		if (renderer instanceof LivingEntityRenderer ler && ler.getModel() instanceof HumanoidModel) {
			ler.addLayer(new TouhouHatLayer<>(ler));
			ler.addLayer(new IceWingsLayer<>(ler));
		}
	}

}

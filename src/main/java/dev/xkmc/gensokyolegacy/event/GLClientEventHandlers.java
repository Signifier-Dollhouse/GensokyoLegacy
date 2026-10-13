package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectRenderer;
import dev.xkmc.gensokyolegacy.content.attachment.area.ClientAreaEffectTracker;
import dev.xkmc.gensokyolegacy.content.client.deco.DowserRenderer;
import dev.xkmc.gensokyolegacy.content.client.party.PartyAreaRenderer;
import dev.xkmc.gensokyolegacy.content.effect.FloatingEffect;
import dev.xkmc.gensokyolegacy.content.item.hakkero.HakkeroPrototypeDeco;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureOutlineRenderer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.util.LavaEffectsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = GensokyoLegacy.MODID)
public class GLClientEventHandlers {

	@SubscribeEvent
	public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientAreaEffectTracker.clear();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel().isClientSide()) {
			ClientAreaEffectTracker.clear();
		}
	}

	@SubscribeEvent
	public static void tooltip(ItemTooltipEvent event) {
		if (true) return;//TODO gift not enabled yet;
		var level = Minecraft.getInstance().level;
		if (level == null) return;
		var data = GLMeta.GIFT_DATA.get(level.registryAccess(), event.getItemStack().getItemHolder());
		if (data == null) return;
		event.getToolTip().add(GLLang.ItemCommon.GIFT_FAVOR.get(data.favor()).withStyle(ChatFormatting.GOLD));
		event.getToolTip().add(GLLang.ItemCommon.GIFT_TYPE.get(data.type().getDisplay()).withStyle(ChatFormatting.GRAY));
	}

	@SubscribeEvent
	public static void renderStageEvent(RenderLevelStageEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
			StructureOutlineRenderer.renderOutline(event.getPoseStack(), event.getCamera().getPosition());
			DowserRenderer.renderOutline(event.getPoseStack(), event.getCamera().getPosition());
			PartyAreaRenderer.renderOutline(event.getPoseStack(), event.getCamera().getPosition());
		}
		// no tick handler: the renderer holds no state of its own, it reads the held
		// wand's component every frame
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			AreaEffectRenderer.onRenderLevel(mc.level, event);
		}
	}

	@SubscribeEvent
	public static void onClientTickPre(ClientTickEvent.Pre event) {
		DowserRenderer.tickClient();
	}

	@SubscribeEvent
	public static void onPlayerTickPost(PlayerTickEvent.Post event) {
		var pl = event.getEntity();
		if (pl.hasEffect(GLEffects.FLOATING)) {
			if (pl instanceof LocalPlayer lp) {
				var vec = pl.getDeltaMovement();
				if (lp.input.jumping && vec.y < 0)
					pl.setDeltaMovement(vec.multiply(1, 0.8, 1));
			}
		}
	}

	public static void onSlotRender(GuiGraphics g, ItemStack stack, Slot slot) {
		if (slot.container instanceof Inventory inv) {
			int index = slot.getSlotIndex();
			HakkeroPrototypeDeco.renderSlot(g, stack, inv, index, slot.x, slot.y);
		}
	}

	public static int onStackDeco(int original) {
		if (original > 9) {
			var player = Minecraft.getInstance().player;
			if (player != null && player.hasEffect(GLEffects.BAKA)) {
				return 9;
			}
		}
		return original;
	}

	public static int onDecoColor(String text, int original) {
		if (text.equals("9")) {
			var player = Minecraft.getInstance().player;
			if (player != null && player.hasEffect(GLEffects.BAKA)) {
				return 0x9AC0ED;
			}
		}
		return original;
	}

	@SubscribeEvent
	public static void renderFireOverlay(RenderBlockScreenEffectEvent event) {
		if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.FIRE) {
			if (LavaEffectsHelper.noFire(event.getPlayer())) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public static void onFogSetup(ViewportEvent.RenderFog event) {
		if (event.getType() == FogType.LAVA) {
			if (event.getCamera().getEntity() instanceof LivingEntity le && LavaEffectsHelper.lavaVision(le)) {
				event.setFarPlaneDistance(24f);
				event.setCanceled(true);
			}
		}
	}

}
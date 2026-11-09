package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectRenderer;
import dev.xkmc.gensokyolegacy.content.attachment.area.ClientAreaEffectTracker;
import dev.xkmc.gensokyolegacy.content.client.deco.DowserRenderer;
import dev.xkmc.gensokyolegacy.content.client.party.PartyAreaRenderer;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureOutlineRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.client.HeadPossession;
import dev.xkmc.gensokyolegacy.content.item.anchor.AnchorItem;
import dev.xkmc.gensokyolegacy.content.item.hakkero.HakkeroPrototypeDeco;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.gensokyolegacy.util.LavaEffectsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = GensokyoLegacy.MODID)
public class GLClientEventHandlers {

	@SubscribeEvent
	public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientAreaEffectTracker.clear();
	}

	/**
	 * Shift and right click is the head's camera toggle, so it has to reach the client at all.
	 */
	@SubscribeEvent
	public static void onInteractKey(InputEvent.InteractionKeyMappingTriggered event) {
		HeadPossession.onInteract(event);
	}

	@SubscribeEvent
	public static void onRenderHand(RenderHandEvent event) {
		HeadPossession.onRenderHand(event);
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel().isClientSide()) {
			ClientAreaEffectTracker.clear();
		}
	}

	@SubscribeEvent
	public static void tooltip(ItemTooltipEvent event) {
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
	public static void onClientTickPre(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;
		DowserRenderer.tickClient();
	}

	@SubscribeEvent
	public static void onPlayerTickPost(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		var pl = event.player;
		if (pl.hasEffect(GLEffects.FLOATING.get())) {
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
			if (player != null && player.hasEffect(GLEffects.BAKA.get())) {
				return 9;
			}
		}
		return original;
	}

	public static int onDecoColor(String text, int original) {
		if (text.equals("9")) {
			var player = Minecraft.getInstance().player;
			if (player != null && player.hasEffect(GLEffects.BAKA.get())) {
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

	/**
	 * The anchor borrows the bow's zoom while it charges: the same squared ramp and 15% at full
	 * draw, scaled by the player's FOV effect setting like the vanilla bow.
	 */
	@SubscribeEvent
	public static void onComputeFov(ComputeFovModifierEvent event) {
		Player player = event.getPlayer();
		if (!(player.getUseItem().getItem() instanceof AnchorItem)) return;
		float t = AnchorItem.chargeProgress(player);
		if (t <= 0) return;
		float fov = event.getFovModifier() * (1F - t * 0.15F);
		event.setNewFovModifier((float) Mth.lerp(Minecraft.getInstance().options.fovEffectScale().get(), 1F, fov));
	}

}
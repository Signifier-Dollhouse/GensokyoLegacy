package dev.xkmc.gensokyolegacy.event;

import dev.xkmc.gensokyolegacy.content.client.deco.DowserRenderer;
import dev.xkmc.gensokyolegacy.content.client.deco.FurnaceItemDeco;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureOutlineRenderer;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = GensokyoLegacy.MODID)
public class GLClientEventHandlers {

	@SubscribeEvent
	public static void tooltip(ItemTooltipEvent event) {
		var level = Minecraft.getInstance().level;
		if (level == null) return;
		var data = GLMeta.GIFT_DATA.get(level.registryAccess(), event.getItemStack().getItemHolder());
		if (data == null) return;
		event.getToolTip().add(GLLang.ITEM$GIFT_FAVOR.get(data.favor()).withStyle(ChatFormatting.GOLD));
		event.getToolTip().add(GLLang.ITEM$GIFT_TYPE.get(data.type().getDisplay()).withStyle(ChatFormatting.GRAY));
	}

	@SubscribeEvent
	public static void renderStageEvent(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
			StructureOutlineRenderer.renderOutline(event.getPoseStack(), event.getCamera().getPosition());
			DowserRenderer.renderOutline(event.getPoseStack(), event.getCamera().getPosition());
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Pre event) {
		DowserRenderer.tickClient();
	}

	public static void onSlotRender(GuiGraphics g, ItemStack stack, Slot slot) {
		if (slot.container instanceof Inventory inv) {
			int index = slot.getSlotIndex();
			FurnaceItemDeco.renderSlot(g, stack, inv, index, slot.x, slot.y);
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

}
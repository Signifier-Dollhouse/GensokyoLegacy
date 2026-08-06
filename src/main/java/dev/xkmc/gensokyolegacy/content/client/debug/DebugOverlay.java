package dev.xkmc.gensokyolegacy.content.client.debug;

import dev.xkmc.gensokyolegacy.content.block.base.IDebugInfoBlockEntity;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureInfoClientManager;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2itemselector.overlay.OverlayUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DebugOverlay implements LayeredDraw.Layer {

	@Override
	public void render(GuiGraphics g, DeltaTracker delta) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;
		var level = player.level();
		var hit = Minecraft.getInstance().hitResult;
		boolean glasses = player.getItemBySlot(EquipmentSlot.HEAD).is(GLItems.DEBUG_GLASSES);
		ItemStack hand = player.getMainHandItem();
		boolean doorDebug = hand.is(GLItems.DOOR_DEBUG_WAND);
		if (!glasses && !doorDebug) return;
		List<Component> lines = new ArrayList<>();
		long time = level.getGameTime();
		if (glasses) {
			if (hit instanceof BlockHitResult block) {
				if (level.getBlockEntity(block.getBlockPos()) instanceof IDebugInfoBlockEntity bed) {
					BedInfoClientManager.tooltip(lines, time, bed);
				} else {
					StructureInfoClientManager.tooltip(lines, time, block.getBlockPos());
				}
			} else if (hit instanceof EntityHitResult ehit) {
				if (ehit.getEntity() instanceof YoukaiEntity youkai) {
					CharacterInfoClientManager.tooltip(lines, time, youkai);
				}
			}
		}
		if (doorDebug) {
			UUID id = hand.get(GLItems.DC_DEBUG_YOUKAI);
			if (id == null) {
				lines.add(GLLang.ITEM$DOOR_DEBUG_UNBOUND.get().withStyle(ChatFormatting.GRAY));
			} else {
				Entity e = level.getEntitiesOfClass(YoukaiEntity.class,
								player.getBoundingBox().inflate(64), e2 -> e2.getUUID().equals(id))
						.stream().findFirst().orElse(null);
				if (e instanceof YoukaiEntity youkai) {
					CharacterInfoClientManager.doorTooltip(lines, time, youkai);
				} else {
					lines.add(GLLang.ITEM$DOOR_DEBUG_MISSING.get().withStyle(ChatFormatting.GRAY));
				}
			}
		}
		if (lines.isEmpty()) return;
		int sw = g.guiWidth();
		int sh = g.guiHeight();
		new OverlayUtil(g, (int) (sw * 0.6), (int) (sh * 0.5), (int) (sw * 0.3))
				.renderLongText(Minecraft.getInstance().font, lines);
	}

}

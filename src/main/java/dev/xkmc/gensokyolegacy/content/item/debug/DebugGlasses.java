package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.content.client.debug.DebugCharacterTooltip;
import dev.xkmc.gensokyolegacy.content.client.debug.IDebugOverlayWand;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureInfoClientManager;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DebugGlasses extends Item implements IDebugOverlayWand {

	public DebugGlasses(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.HEAD;
	}

	@Override
	public void addTooltip(Player player, ItemStack stack, List<Component> lines, long gameTime) {
		if (DebugCharacterTooltip.add(player, lines, gameTime)) return;
		StructureInfoClientManager.tooltip(lines, gameTime);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ITEM$GLASS_PATH.get().withStyle(ChatFormatting.GRAY));

		list.add(GLLang.ITEM$GLASS_CHARACTER.get().withStyle(ChatFormatting.GRAY));
		list.add(GLLang.ITEM$GLASS_BED.get().withStyle(ChatFormatting.GRAY));
	}

}

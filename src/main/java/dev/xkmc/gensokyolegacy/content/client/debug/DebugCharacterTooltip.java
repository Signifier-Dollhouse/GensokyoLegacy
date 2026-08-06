package dev.xkmc.gensokyolegacy.content.client.debug;

import dev.xkmc.gensokyolegacy.content.block.base.IDebugInfoBlockEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;

public class DebugCharacterTooltip {

	/**
	 * @return true if a bed or character overlay was added.
	 */
	public static boolean add(Player player, List<Component> lines, long gameTime) {
		var level = player.level();
		var hit = Minecraft.getInstance().hitResult;
		if (hit instanceof BlockHitResult block) {
			if (level.getBlockEntity(block.getBlockPos()) instanceof IDebugInfoBlockEntity bed) {
				BedInfoClientManager.tooltip(lines, gameTime, bed);
				return true;
			}
		} else if (hit instanceof EntityHitResult ehit) {
			if (ehit.getEntity() instanceof YoukaiEntity youkai) {
				CharacterInfoClientManager.tooltip(lines, gameTime, youkai);
				return true;
			}
		}
		return false;
	}

}

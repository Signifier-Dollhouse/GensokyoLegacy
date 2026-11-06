package dev.xkmc.gensokyolegacy.content.item.debug;

import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPower;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPowerHolder;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostVariantHolder;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * One ghost readout shared by the debug wand click and the debug glasses overlay.
 */
public final class GhostDebugInfo {

	private GhostDebugInfo() {
	}

	public static Component describe(Entity entity, GhostPowerHolder holder) {
		Component variant = ((GhostVariantHolder) holder).getVariant().getDisplayName();
		var line = entity instanceof GhostEntity ? GLLang.ItemDebug.GHOST_TAMED : GLLang.ItemDebug.GHOST_WILD;
		return line.get(variant, holder.getPower(), GhostPower.MAX);
	}

}

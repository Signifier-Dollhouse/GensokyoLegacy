package dev.xkmc.gensokyolegacy.content.item.glove.client;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.action.DollActionType;
import dev.xkmc.gensokyolegacy.content.item.glove.DollGloveItem;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveMode;
import dev.xkmc.gensokyolegacy.content.item.glove.mode.DollGloveModes;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Client side of wheel visibility: super / suicide show while a summoned doll
 * carries a matching weapon, read off the synced roster plus each doll's
 * server-computed validity mask. No capability access.
 */
public final class DollGloveClientModes {

	private DollGloveClientModes() {
	}

	public static List<DollGloveMode> available(Player player, DollGloveMode current) {
		boolean hasSuper = current == DollGloveMode.SUPER || hasValidDoll(player, DollActionType.SUPER_ATTACK);
		boolean hasSuicide = current == DollGloveMode.SUICIDE || hasValidDoll(player, DollActionType.SUICIDE_ATTACK);
		return DollGloveModes.available(current, hasSuper, hasSuicide);
	}

	/**
	 * Display list for the scroll sidebar ({@code getList} has no player
	 * param): resolves the client player internally. Falls back to the static
	 * set when no player is at hand.
	 */
	public static List<DollGloveMode> availableForDisplay(ItemStack stack) {
		var mc = Minecraft.getInstance();
		if (mc.player == null) return DollGloveModes.potentiallyVisible();
		return available(mc.player, DollGloveItem.getMode(stack));
	}

	public static boolean hasValidDoll(Player player, DollActionType type) {
		var mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		for (var entry : GLMeta.DOLL.type().getOrCreate(player).getRoster()) {
			if (!(mc.level.getEntity(entry.id()) instanceof DollEntity doll)) continue;
			if (!doll.getUUID().equals(entry.uuid())) continue;
			if (doll.isValidFor(type)) return true;
		}
		return false;
	}

}

package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Dual ghost: no upkeep and no aura, spent once to cancel the owner's death.
 */
final class DualGhostHandler implements GhostVariantHandler {

	@Override
	public Component getDisplayName() {
		return GLLang.Ghost.DUAL.get();
	}

	@Override
	public boolean wild() {
		return false;
	}

	@Override
	public boolean leavesWhenWeak() {
		return false;
	}

	@Override
	public boolean wardDeath(ServerPlayer player, GhostEntity ghost, DollAttachment att) {
		return att.absorb(player, ghost);
	}

}

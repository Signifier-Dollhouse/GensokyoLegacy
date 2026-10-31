package dev.xkmc.gensokyolegacy.content.item.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Collects every summoned ghost the player owns. At a full fan a ghost is released instead, the
 * same escape hatch a single absorption uses; the haunting ghost refuses release and stays.
 */
final class AbsorbFanHandler implements YuyukoFanModeHandler {

	@Override
	public boolean use(ServerPlayer sp, ItemStack stack, DollAttachment att) {
		int absorbed = 0;
		int count = 0;
		int released = 0;
		for (GhostEntity ghost : att.summonedGhosts(sp)) {
			int room = SpiritualPower.max(stack) - SpiritualPower.get(stack);
			if (room <= 0) {
				if (att.release(sp, ghost)) released++;
				continue;
			}
			int take = Math.min(ghost.getPower(), room);
			if (!att.absorb(sp, ghost)) continue;
			SpiritualPower.add(stack, take);
			absorbed += take;
			count++;
		}
		if (count == 0 && released == 0) {
			boolean full = SpiritualPower.get(stack) >= SpiritualPower.max(stack);
			sp.displayClientMessage(full ? GLLang.Ghost.FAN_FULL.get() : GLLang.Ghost.NO_GHOSTS.get(), true);
			return false;
		}
		if (count > 0) sp.displayClientMessage(GLLang.Ghost.ABSORB_ALL.get(absorbed, count), true);
		if (released > 0) sp.displayClientMessage(GLLang.Ghost.RELEASED_ALL.get(released), true);
		return true;
	}

	@Override
	public ItemStack icon() {
		return new ItemStack(GLItems.YUYUKO_FAN.get());
	}

}

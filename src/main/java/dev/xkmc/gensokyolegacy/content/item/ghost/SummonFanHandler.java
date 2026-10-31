package dev.xkmc.gensokyolegacy.content.item.ghost;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollSpawn;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPower;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostVariant;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Refills every summoned ghost, then summons one of each missing kind while power and the ledger
 * cap allow it.
 */
final class SummonFanHandler implements YuyukoFanModeHandler {

	@Override
	public boolean use(ServerPlayer sp, ItemStack stack, DollAttachment att) {
		if (SpiritualPower.get(stack) <= 0) {
			sp.displayClientMessage(GLLang.Ghost.FAN_EMPTY.get(), true);
			return false;
		}
		int restored = 0;
		int summoned = 0;
		boolean capped = false;
		for (GhostVariant type : GhostVariant.values()) {
			if (!type.handler.summonable()) continue;
			GhostEntity existing = att.findSummonedGhost(sp, type);
			if (existing != null) {
				int need = GhostPower.MAX - existing.getPower();
				int spend = Math.min(need, SpiritualPower.get(stack));
				if (spend > 0) {
					existing.addPower(spend);
					SpiritualPower.add(stack, -spend);
					restored += spend;
				}
				continue;
			}
			if (SpiritualPower.get(stack) < GhostPower.SUMMON_COST) continue;
			if (att.summonCapped()) {
				capped = true;
				break;
			}
			var pos = DollSpawn.ringPos(sp, att.summonedCount(), DollAttachment.MAX_SUMMONED);
			if (!att.summonGhost(sp, type, pos)) continue;
			SpiritualPower.add(stack, -GhostPower.SUMMON_COST);
			summoned++;
		}
		if (restored == 0 && summoned == 0) {
			if (capped) {
				sp.displayClientMessage(GLLang.Doll.TOO_MANY.get(DollAttachment.MAX_SUMMONED), true);
			} else if (hasSummoned(sp, att)) {
				sp.displayClientMessage(GLLang.Ghost.ALL_FULL.get(), true);
			} else {
				sp.displayClientMessage(GLLang.Ghost.FAN_EMPTY.get(), true);
			}
			return false;
		}
		sp.displayClientMessage(GLLang.Ghost.SUMMON_ALL.get(summoned, restored), true);
		return true;
	}

	private static boolean hasSummoned(ServerPlayer sp, DollAttachment att) {
		for (GhostVariant type : GhostVariant.values()) {
			if (!type.handler.summonable()) continue;
			if (att.findSummonedGhost(sp, type) != null) return true;
		}
		return false;
	}

	@Override
	public ItemStack icon() {
		SpawnEggItem egg = SpawnEggItem.byId(GLEntities.STRAY_GHOST.get());
		return egg == null ? new ItemStack(GLItems.YUYUKO_FAN.get()) : new ItemStack(egg);
	}

}

package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollSpawn;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPower;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostVariant;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Sends the ledger's haunting ghost at the marked target. An existing one is refilled and reused
 * instead of spawning a second, so a hunter never leaves a retinue of ghosts behind.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
final class HauntFanHandler implements YuyukoFanModeHandler {

	@Override
	public boolean use(ServerPlayer sp, ItemStack stack, DollAttachment att) {
		LivingEntity target = GLMeta.GLOVE_TARGET.type().getOrCreate(sp).resolve(sp, stack);
		if (target == null || !YuyukoFanItem.isHauntTarget(stack, sp, target)) {
			sp.displayClientMessage(GLLang.Ghost.NO_TARGET.get(), true);
			return false;
		}
		GhostEntity existing = att.findSummonedGhost(sp, GhostVariant.HAUNTING);
		if (existing != null) {
			int cost = Math.min(GhostPower.SUMMON_COST, GhostPower.MAX - existing.getPower());
			if (SpiritualPower.get(stack) < cost) {
				sp.displayClientMessage(GLLang.Ghost.NEED_POWER.get(cost), true);
				return false;
			}
			existing.addPower(cost);
			SpiritualPower.add(stack, -cost);
			existing.hunt(target);
			sp.displayClientMessage(GLLang.Ghost.HAUNT_REUSED.get(), true);
			return true;
		}
		if (SpiritualPower.get(stack) < GhostPower.SUMMON_COST) {
			sp.displayClientMessage(GLLang.Ghost.NEED_POWER.get(GhostPower.SUMMON_COST), true);
			return false;
		}
		if (att.summonCapped()) {
			sp.displayClientMessage(GLLang.Doll.TOO_MANY.get(DollAttachment.MAX_SUMMONED), true);
			return false;
		}
		Vec3 pos = DollSpawn.ringPos(sp, att.summonedCount(), DollAttachment.MAX_SUMMONED);
		if (!att.summonGhost(sp, GhostVariant.HAUNTING, pos)) {
			sp.displayClientMessage(GLLang.Ghost.SUMMON_FAIL.get(), true);
			return false;
		}
		GhostEntity ghost = att.findSummonedGhost(sp, GhostVariant.HAUNTING);
		if (ghost == null) {
			sp.displayClientMessage(GLLang.Ghost.SUMMON_FAIL.get(), true);
			return false;
		}
		ghost.hunt(target);
		SpiritualPower.add(stack, -GhostPower.SUMMON_COST);
		sp.displayClientMessage(GLLang.Ghost.HAUNT_SUMMONED.get(), true);
		return true;
	}

	@Override
	public ItemStack icon() {
		return new ItemStack(GLItems.BONE_FLUTE.get());
	}

}

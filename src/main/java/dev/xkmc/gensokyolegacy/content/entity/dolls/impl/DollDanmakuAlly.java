package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.api.IYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Friendly filter for doll-shot danmaku: the owner and fellow dolls of the same
 * owner never take friendly fire. Ownerless dolls (block-hosted, stray) treat
 * each other as friendly. Player-owned dolls additionally spare non-enemies —
 * anything the player never commanded against, that isn't hostile to the owner
 * side, and that no player hurt before. Fully defaulted — the entity gains
 * danmaku allyship with zero additional code.
 */
public interface DollDanmakuAlly extends DollBaseImpl, IYoukaiEntity {

	@Override
	default boolean shouldHurt(LivingEntity le) {
		if (le == asDoll()) return false;
		if (isOwnerSide(le)) return false;
		UUID owner = asDoll().getOwnerUUID();
		if (le instanceof OwnableEntity other) {
			UUID otherOwner = other.getOwnerUUID();
			if (owner == null && otherOwner == null) return false;
		}
		return owner == null || !isNonEnemy(le);
	}

	/**
	 * The owner side — the owner plus anything they own (dolls, pets) — is
	 * always spared from doll danmaku, regardless of enemy determination below.
	 */
	default boolean isOwnerSide(LivingEntity entity) {
		UUID owner = asDoll().getOwnerUUID();
		if (owner == null || entity == null) return false;
		if (entity.getUUID().equals(owner)) return true;
		return entity instanceof OwnableEntity other && owner.equals(other.getOwnerUUID());
	}

	/**
	 * Non-enemy: not commanded to be attacked by the player, not targeting the
	 * owner, and never hurt by the owner (last-hurt check on the entity is
	 * enough; allies are skipped). Commanded targets stay enemies past ticket
	 * completion (bullets outlive tickets) via the commander's transient attack
	 * record.
	 */
	default boolean isNonEnemy(LivingEntity le) {
		DollEntity doll = asDoll();
		UUID ownerId = doll.getOwnerUUID();
		if (ownerId == null) return false;
		DollHost host = doll.getHost();
		if (host instanceof DollAttachment att && att.commands.isCommandedTarget(le)) return false;
		if (le instanceof Mob mob && mob.getTarget() != null &&
				ownerId.equals(mob.getTarget().getUUID())) return false;
		LivingEntity lastHurt = le.getLastHurtByMob();
		if (lastHurt != null && ownerId.equals(lastHurt.getUUID())) return false;
		return true;
	}

	@Override
	default void onDanmakuHit(LivingEntity target, IDanmakuEntity bullet) {
	}

	@Override
	default void onDanmakuImmune(LivingEntity target, IDanmakuEntity bullet, DamageSource source) {
	}

	@Override
	default boolean isTarget(LivingEntity e) {
		return false;
	}

	@Override
	default AABB getBoundingBoxForDanmaku() {
		return asDoll().getBoundingBox();
	}

}

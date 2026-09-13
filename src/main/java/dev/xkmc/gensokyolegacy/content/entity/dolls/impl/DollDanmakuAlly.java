package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.api.IYoukaiEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Friendly filter for doll-shot danmaku: the owner and fellow dolls of the same
 * owner never take friendly fire. Ownerless dolls (block-hosted, stray) treat
 * each other as friendly. Everything else is a valid target. Fully defaulted —
 * the entity gains danmaku allyship with zero additional code.
 */
public interface DollDanmakuAlly extends DollBaseImpl, IYoukaiEntity {

	@Override
	default boolean shouldHurt(LivingEntity le) {
		if (le == asDoll()) return false;
		UUID owner = asDoll().getOwnerUUID();
		if (owner != null && owner.equals(le.getUUID())) return false;
		if (le instanceof OwnableEntity other) {
			UUID otherOwner = other.getOwnerUUID();
			if (owner != null && owner.equals(otherOwner)) return false;
			if (owner == null && otherOwner == null) return false;
		}
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

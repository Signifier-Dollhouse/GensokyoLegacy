package dev.xkmc.gensokyolegacy.content.entity.youkai.impl;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.api.IYoukaiEntity;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.fastprojectileapi.spellcircle.SpellCircleHolder;
import dev.xkmc.gensokyolegacy.content.entity.behavior.combat.YoukaiFightEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about how a character picks fights and answers being hit: the targeting clock, who
 * counts as a target at all, the danmaku hooks and the spell circle.
 * <p>
 * The strategy behind it all is {@code YoukaiCombatManager}, which a character overrides
 * instead; this is the entity-side face of it. It extends {@link IYoukaiEntity} and
 * {@link SpellCircleHolder} so its defaults are real overrides of those declarations rather than
 * siblings of them. The vanilla gate that calls {@link #invulnerableTo} is the one piece left on
 * the entity class - see {@code YoukaiEntity}.
 *
 * @see "doc/design/youkai/entity.md"
 */
public interface YoukaiCombat extends YoukaiBaseImpl, IYoukaiEntity, SpellCircleHolder {

	default void tickTargeting() {
		var self = asYoukai();
		if (self.getTarget() == null || !self.getTarget().isAlive()) {
			int idle = self.noTargetTime() + 1;
			self.setNoTargetTime(idle);
			boolean doHeal = self.getFeatures().noTargetHealing() && idle >= 20 && self.tickCount % 20 == 0;
			doHeal |= self.getCombatProgress() < self.getMaxHealth();
			if (doHeal && self.getLastHurtByMob() instanceof Player player && player.getAbilities().instabuild) {
				if (self.tickCount - self.getLastHurtByMobTimestamp() < 100) {
					doHeal = false;
				}
			}
			if (doHeal) {
				self.setHealth(self.getMaxHealth());
			}
		} else {
			self.setNoTargetTime(0);
		}
		int idle = self.noTargetTime();
		if (idle == 0 && self.getFeatures().hasBossBar()) {
			self.setBossBarVisible(true);
		}
		if (idle > 40) {
			self.setBossBarVisible(false);
		}
		self.refreshIdle();
	}

	default void tickSpell() {
		var self = asYoukai();
		if (self.spellCard != null) {
			if (self.isAggressive() && self.shouldShowSpellCircle()) {
				self.spellCard.tick(self.cardHolder);
			} else {
				self.spellCard.reset();
			}
		}
	}

	default boolean shouldIgnore(LivingEntity e) {
		var self = asYoukai();
		if (e instanceof Player player && player.getAbilities().instabuild)
			return false;
		if (!self.targets.isValidTarget(e))
			return true;
		var event = new YoukaiFightEvent(self, e);
		return NeoForge.EVENT_BUS.post(event).isCanceled();
	}

	default boolean wouldInitiateAttack(LivingEntity entity) {
		var self = asYoukai();
		if (entity instanceof Player) return false;
		if (!self.targets.isValidTarget(entity)) return false;
		if (shouldIgnore(entity)) return false;
		return self.combatManager.targetKind(entity).initiateAttack();
	}

	default boolean isHostileTo(LivingEntity le) {
		var self = asYoukai();
		return self.targets.contains(le) || wouldInitiateAttack(le);
	}

	@Override
	default boolean shouldHurt(LivingEntity le) {
		var self = asYoukai();
		if (shouldIgnore(le)) return false;
		if (le instanceof Player && self.targets.isValidTarget(le)) return true;
		return isHostileTo(le) || self.combatManager.targetKind(le).isPrey()
				|| self.combatManager.shouldHurtInnocent(le);
	}

	@Override
	default void onDanmakuHit(LivingEntity e, IDanmakuEntity danmaku) {
		asYoukai().combatManager.onDanmakuHit(e, danmaku);
	}

	@Override
	default void onDanmakuImmune(LivingEntity e, IDanmakuEntity danmaku, DamageSource source) {
		asYoukai().combatManager.onDanmakuImmune(e, danmaku, source);
	}

	@Override
	default boolean shouldShowSpellCircle() {
		return asYoukai().combatManager.getSpellCircle() != null;
	}

	@Override
	default @Nullable ResourceLocation getSpellCircle() {
		return asYoukai().combatManager.getSpellCircle();
	}

	@Override
	default float getCircleSize(float pTick) {
		return asYoukai().combatManager.getCircleSize(pTick);
	}

	/**
	 * The invulnerability gate itself. {@link DamageTypeTags#IS_FALL} and the combat manager's own
	 * filter stay on the entity class, where the vanilla super call lives.
	 */
	default boolean invulnerableTo(DamageSource source) {
		var self = asYoukai();
		if (source.getEntity() instanceof LivingEntity le && shouldIgnore(le)) {
			return true;
		}
		return source.is(DamageTypeTags.IS_FALL) || self.combatManager.isInvulnerableTo(source);
	}

	default void shoot(float dmg, int life, Vec3 vec, DyeColor color) {
		var self = asYoukai();
		ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), self, self.level());
		danmaku.setItem(DanmakuItems.Bullet.CIRCLE.get(color).asStack());
		danmaku.setup(dmg, life, true, true, vec);
		self.level().addFreshEntity(danmaku);
	}

}
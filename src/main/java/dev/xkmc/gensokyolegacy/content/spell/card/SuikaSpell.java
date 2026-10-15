package dev.xkmc.gensokyolegacy.content.spell.card;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.spell.spellcard.ActualSpellCard;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.Ticker;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.foundation.DamageClampEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.YoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Suika's spell card: a brawler rather than a caster, picking one of three melee moves by distance, and only
 * laying down danmaku once cornered. The charge leap outruns the speed cap - {@link DamageClampEntity#grantOverspeed}.
 */
@SerialClass
public class SuikaSpell extends ActualSpellCard {

	/**
	 * Health ratio at which the danmaku layer comes on. The flag latches for the rest of the fight once she is
	 * pushed this low; only {@link #reset()} - called on every tick she is not attacking - clears it.
	 */
	private static final float EMPOWERED_HEALTH = 0.7f;

	private static final double FAR = 12, NEAR = 4;
	private static final double PULL_RANGE = 12, KNOCK_RANGE = 6;
	private static final double JUMP_SPEED = 1.5;
	private static final int JUMP_OVER_SPEED = 20;
	private static final double PULL_SPEED = 0.2;
	private static final double KNOCK_SPEED = 1.5;
	private static final float KNOCK_DAMAGE = 10;
	private static final int JUMP_CD = 25, PULL_CD = 40, SWING_CD = 15;

	private static final int RING_COUNT = 24;
	private static final int RING_LIFE = 40;
	private static final int CHARGE_RING_TICKS = 20;
	private static final int GATHER_PER_TICK = 2, GATHER_DURATION = 40;
	private static final double GATHER_DIST = 16, GATHER_ANGULAR_SPEED = 360d / 60;
	private static final int SWING_TICKS = 20, SWING_CRACKS = 20, SWING_LIFE = 20;
	private static final double SWING_SPEED = 0.7, SWING_RAD_RATE = 1;

	/**
	 * Paces the charge and gather moves.
	 */
	@SerialField
	private int cooldown;

	/**
	 * Paces the swing, kept apart from {@link #cooldown} so a swing can cut into a charge or gather.
	 */
	@SerialField
	private int swingCd;

	/**
	 * Bumped every swing. Charge and gather danmaku tag themselves with the value they were born under and expire
	 * once it moves on, which is how a swing wipes those tickers without touching a {@link SwingTicker}.
	 */
	@SerialField
	private int moveId;

	/**
	 * Latched on at {@link #EMPOWERED_HEALTH}; only {@link #reset()} turns it off.
	 */
	@SerialField
	private boolean empowered;

	@Override
	public void tick(CardHolder holder) {
		super.tick(holder);
		if (cooldown > 0) cooldown--;
		if (swingCd > 0) swingCd--;
		var self = holder.self();
		if (!empowered && self.getHealth() < self.getMaxHealth() * EMPOWERED_HEALTH) empowered = true;
		var target = holder.target();
		if (target == null) return;
		double distA = holder.center().subtract(target).multiply(1, 0.5, 1).length();
		if (self instanceof YoukaiEntity y) {
			for (var e : targets(y, FAR)) {
				distA = Math.min(distA, holder.center().subtract(e.position()).length());
			}
		}
		if (distA < NEAR) {
			if (swingCd > 0) return;
			swing(holder, empowered);
			swingCd = SWING_CD;
			cooldown = 0;
			return;
		}
		if (cooldown > 0) return;
		double dist = holder.center().subtract(target).length();
		if (dist > FAR) {
			charge(holder, target, empowered);
			cooldown = JUMP_CD;
		} else {
			gather(holder);
			cooldown = PULL_CD;
		}
	}

	/**
	 * The base class clears the card's tickers whenever Suika is not attacking, the signal that the fight is
	 * over; the latched {@link #empowered} flag is dropped with them.
	 */
	@Override
	public void reset() {
		super.reset();
		empowered = false;
	}

	/**
	 * The leap. Straight at the target's centre, the honest direction for a flying boss. Navigation is stopped
	 * because a stale route would otherwise keep steering her while she flies.
	 */
	private void charge(CardHolder holder, Vec3 target, boolean empowered) {
		var self = holder.self();
		var diff = target.subtract(holder.center());
		if (diff.lengthSqr() < 1e-6) return;
		var dir = diff.normalize();
		self.setDeltaMovement(dir.scale(JUMP_SPEED));
		self.hasImpulse = true;
		if (self instanceof DamageClampEntity clamp) clamp.grantOverspeed(JUMP_OVER_SPEED);
		if (self instanceof YoukaiEntity youkai) youkai.getNavigation().stop();
		if (empowered) addTicker(new ChargeTicker());
	}

	/**
	 * One ring of mentos perpendicular to {@code dir}: {@code asNormal} swaps the frame's forward for its
	 * normal, and every rotation in that frame stays perpendicular to {@code dir}.
	 */
	private void ring(CardHolder holder, Vec3 dir) {
		var ori = DanmakuHelper.getOrientation(dir).asNormal();
		for (int i = 0; i < RING_COUNT; i++) {
			var vec = ori.rotateDegrees(360d / RING_COUNT * i).scale(1);
			holder.shoot(holder.prepareDanmaku(RING_LIFE, vec, DanmakuItems.Bullet.MENTOS, DyeColor.BLUE));
		}
	}

	/**
	 * The leap's danmaku: rather than one ring at launch, a ring is seeded every tick for {@link #CHARGE_RING_TICKS}
	 * ticks, so the dash trails a sleeve of them. The plane is read once, when the first ring is due, and so survives
	 * drag.
	 */
	@SerialClass
	public static class ChargeTicker extends Ticker<SuikaSpell> {

		@SerialField
		private Vec3 dir = Vec3.ZERO;
		@SerialField
		private int owner = -1;

		@Override
		public boolean tick(CardHolder holder, SuikaSpell card) {
			super.tick(holder, card);
			if (owner < 0) owner = card.moveId;
			if (owner != card.moveId) return true;
			if (dir.lengthSqr() < 1e-9) init(holder);
			if (tick <= CHARGE_RING_TICKS && dir.lengthSqr() > 1e-9) card.ring(holder, dir);
			return tick > CHARGE_RING_TICKS;
		}

		private void init(CardHolder holder) {
			var v = holder.self().getDeltaMovement();
			dir = v.lengthSqr() < 1e-9 ? holder.forward() : v.normalize();
		}
	}

	/**
	 * The yank. She plays the gather clip and pulls in one burst rather than holding a beam on them: the clip is
	 * a sweep of the arms, and a per-tick pull would drag everyone through her instead of to her.
	 */
	private void gather(CardHolder holder) {
		var youkai = youkai(holder);
		if (youkai == null) return;
		YoukaiAnim.SKILL_01.play(youkai);
		addTicker(new GatherSpawner());
	}

	private void gatherPull(CardHolder holder) {
		var youkai = youkai(holder);
		if (youkai == null) return;
		var center = youkai.position().add(0, youkai.getBbHeight() / 2, 0);
		for (var e : targets(youkai, PULL_RANGE)) {
			var diff = center.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
			double dist = diff.length();
			if (dist < 1e-3) continue;
			e.setDeltaMovement(e.getDeltaMovement().add(diff.scale(PULL_SPEED)));
			e.hasImpulse = true;
			markMoved(e);
		}
	}

	/**
	 * The swing. A shove away from her with a little lift, so the knock reads as a swat rather than a slide, and a flat
	 * {@link #KNOCK_DAMAGE} of mob-attack damage on each; bumping {@link #moveId} is what lets it interrupt.
	 */
	private void swing(CardHolder holder, boolean empowered) {
		var youkai = youkai(holder);
		if (youkai == null) return;
		moveId++;
		youkai.swing(InteractionHand.MAIN_HAND, true);
		YoukaiAnim.SKILL_03.play(youkai);
		var center = youkai.position().add(0, youkai.getBbHeight() / 2, 0);
		for (var e : targets(youkai, KNOCK_RANGE)) {
			var diff = e.position().add(0, e.getBbHeight() / 2, 0).subtract(center);
			var push = new Vec3(diff.x, 0, diff.z);
			if (push.lengthSqr() < 1e-6) continue;
			e.hurt(youkai.damageSources().mobAttack(youkai), KNOCK_DAMAGE);
			e.setDeltaMovement(push.normalize().scale(KNOCK_SPEED).add(0, 0.4, 0));
			e.hasImpulse = true;
			markMoved(e);
		}
		if (empowered) addTicker(new SwingTicker());
	}

	/**
	 * A vanilla velocity change on a player is ignored client side unless the client is told
	 * to re-read it, so every shove flags the player; a mob has no such gate.
	 */
	private static void markMoved(LivingEntity e) {
		if (e instanceof Player player) player.hurtMarked = true;
	}

	@Nullable
	private static YoukaiEntity youkai(CardHolder holder) {
		return holder.self() instanceof YoukaiEntity youkai ? youkai : null;
	}

	/**
	 * Everything this Suika is willing to hurt inside {@code range}, by the same {@code shouldHurt} test
	 * danmaku damage runs through, so melee moves and bullets aim at the same set of entities.
	 */
	private static List<LivingEntity> targets(YoukaiEntity youkai, double range) {
		return youkai.level().getEntitiesOfClass(LivingEntity.class,
				youkai.getBoundingBox().inflate(range),
				e -> e != youkai && e.isAlive() && youkai.shouldHurt(e));
	}

	/**
	 * Seeds the gather pattern: {@link #GATHER_PER_TICK} fresh {@link GatherTicker}s every tick for {@link
	 * #GATHER_DURATION} ticks, so the pattern builds to a steady shell of arms that keeps turning rather than one ring.
	 */
	@SerialClass
	public static class GatherSpawner extends Ticker<SuikaSpell> {

		@SerialField
		private int owner = -1;

		@Override
		public boolean tick(CardHolder holder, SuikaSpell card) {
			super.tick(holder, card);
			if (owner < 0) owner = card.moveId;
			if (owner != card.moveId) return true;
			if (tick <= GATHER_DURATION) {
				card.gatherPull(holder);
				if (card.empowered) {
					for (int i = 0; i < GATHER_PER_TICK; i++) {
						var arm = new GatherTicker();
						arm.owner = owner;
						card.addTicker(arm);
					}
				}
			}
			return tick > GATHER_DURATION;
		}
	}

	/**
	 * One arm of the gather pattern: a plane picked once from a normalised 3D gaussian, one mento per tick at an angle
	 * sweeping a full turn over {@link #GATHER_DURATION} ticks, its {@link #owner} inherited so a swing strands them
	 * all.
	 */
	@SerialClass
	public static class GatherTicker extends Ticker<SuikaSpell> {

		@SerialField
		private Vec3 axis = Vec3.ZERO;
		@SerialField
		private double angle;
		@SerialField
		private int owner = -1;

		@Override
		public boolean tick(CardHolder holder, SuikaSpell card) {
			super.tick(holder, card);
			if (owner < 0) owner = card.moveId;
			if (owner != card.moveId) return true;
			if (axis.lengthSqr() < 1e-9) init(holder);
			if (tick <= GATHER_DURATION) {
				var ori = DanmakuHelper.getOrientation(axis).asNormal();
				var dir = ori.rotateDegrees(angle + GATHER_ANGULAR_SPEED * tick);
				var pos = holder.center().add(dir.scale(GATHER_DIST));
				var e = holder.prepareDanmaku((int) GATHER_DIST, dir.scale(-1), DanmakuItems.Bullet.CIRCLE, DyeColor.BLUE);
				e.setPos(pos);
				holder.shoot(e);
			}
			return tick > GATHER_DURATION;
		}

		private void init(CardHolder holder) {
			var r = holder.random();
			var v = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian());
			axis = v.lengthSqr() < 1e-9 ? new Vec3(0, 1, 0) : v.normalize();
			angle = r.nextDouble() * 360;
		}
	}

	/**
	 * The swing's danmaku: for twenty ticks a growing shell of mentos thrown outward around her at her own height, each
	 * doubled into a rising and a falling copy, turning the ring into a curtain to step out of rather than duck under.
	 */
	@SerialClass
	public static class SwingTicker extends Ticker<SuikaSpell> {

		@Override
		public boolean tick(CardHolder holder, SuikaSpell card) {
			super.tick(holder, card);
			if (tick <= SWING_TICKS) step(holder, tick);
			return tick > SWING_TICKS;
		}

		private void step(CardHolder holder, int t) {
			var center = holder.center();
			double radius = t * SWING_RAD_RATE;
			double y = holder.self().getY();
			var r = holder.random();
			for (int i = 0; i < SWING_CRACKS; i++) {
				double a = r.nextDouble() * Math.PI * 2;
				var pos = new Vec3(center.x + Math.cos(a) * radius, y, center.z + Math.sin(a) * radius);
				spawn(holder, pos, new Vec3(0, SWING_SPEED, 0));
				spawn(holder, pos, new Vec3(0, -SWING_SPEED, 0));
			}
		}

		private void spawn(CardHolder holder, Vec3 pos, Vec3 vec) {
			var e = holder.prepareDanmaku(SWING_LIFE, vec, DanmakuItems.Bullet.BUBBLE, DyeColor.BLUE);
			e.setPos(pos);
			holder.shoot(e);
		}
	}

}

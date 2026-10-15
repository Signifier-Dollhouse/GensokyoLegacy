package dev.xkmc.gensokyolegacy.content.spell.card;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.spell.spellcard.ActualSpellCard;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.spell.part.SakuyaKnifeStorm;
import dev.xkmc.gensokyolegacy.content.spell.part.SakuyaKnifeTrail;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

/**
 * Time Sign "Infinity Blade" — three moves, all knives but the first: a red bubble fan, an orbiting knife ring,
 * and a storm hung off damage. No phases, so this is an {@link ActualSpellCard} with two ordinary methods.
 */
@SerialClass
public class SakuyaSpell extends ActualSpellCard {

	private static final int FAN_INTERVAL = 40;
	private static final int FAN_COUNT = 40;
	private static final double FAN_SPREAD = 30;
	private static final double FAN_MIN_SPEED = 1, FAN_MAX_SPEED = 1.5;
	private static final double FAN_MIN_RANGE = 80, FAN_MAX_RANGE = 100;
	private static final int ORBIT_RADIUS = 3;
	private static final int ORBIT_PERIOD = 20;
	private static final int ORBIT_PER_CYCLE = 2;
	private static final double KNIFE_SPEED = 2;
	private static final double KNIFE_LIFE_FACTOR = 1.5;
	private static final int KNIFE_TRAIL_LIFE = 40;
	private static final int HURT_COOLDOWN = 20;
	private static final int HURT_PER_TICK = 80;
	private static final int HURT_DURATION = 10;

	@SerialField
	private int hurtCooldown;

	@Override
	public void tick(CardHolder holder) {
		super.tick(holder);
		if (hurtCooldown > 0) hurtCooldown--;
		var target = holder.target();
		if (target == null) return;
		if (tick % FAN_INTERVAL == 0) fan(holder, target);
		orbit(holder, target);
	}

	/**
	 * Forty bubbles in a loose cone at the target. Yaw and pitch are scattered independently and
	 * symmetrically about the target line, so the cone is symmetric rather than lopsided.
	 */
	private void fan(CardHolder holder, Vec3 target) {
		var diff = target.subtract(holder.center());
		var dir = diff.lengthSqr() < 1e-4 ? holder.forward() : diff.normalize();
		var r = holder.random();
		var ori = DanmakuHelper.getOrientation(dir);
		for (int i = 0; i < FAN_COUNT; i++) {
			double speed = FAN_MIN_SPEED + r.nextDouble() * (FAN_MAX_SPEED - FAN_MIN_SPEED);
			double range = FAN_MIN_RANGE + r.nextDouble() * (FAN_MAX_RANGE - FAN_MIN_RANGE);
			var vec = ori.rotateDegrees(r.nextDouble() * FAN_SPREAD * 2 - FAN_SPREAD,
					r.nextDouble() * FAN_SPREAD * 2 - FAN_SPREAD).scale(speed);
			holder.shoot(holder.prepareDanmaku((int) Math.ceil(range / speed), vec,
					DanmakuItems.Bullet.BUBBLE, DyeColor.RED));
		}
	}

	/**
	 * The ring. The circle is horizontal, at the caster's own centre height: knives thrown from above
	 * or below her read as a dome, and a rotating ring is only legible as a ring when its halves share a plane.
	 */
	private void orbit(CardHolder holder, Vec3 target) {
		var center = holder.center();
		double step = 360d / ORBIT_PERIOD * tick;
		for (int i = 0; i < ORBIT_PER_CYCLE; i++) {
			double a = Math.toRadians(step + 360d / ORBIT_PER_CYCLE * i);
			knife(holder, target, center.add(Math.cos(a) * ORBIT_RADIUS, 0, Math.sin(a) * ORBIT_RADIUS));
		}
	}

	/**
	 * One knife, over-timed to {@link #KNIFE_LIFE_FACTOR} times its flight so it arrives with the tail still to run and
	 * {@link SakuyaKnifeTrail} sends a second one back; it is set up by hand, the library only does that for players.
	 */
	private void knife(CardHolder holder, Vec3 target, Vec3 pos) {
		var diff = target.subtract(pos);
		double dist = diff.length();
		if (dist < 1e-4) return;
		int life = (int) Math.ceil(KNIFE_LIFE_FACTOR * dist / KNIFE_SPEED);
		var e = holder.prepareDanmaku(life, diff.scale(KNIFE_SPEED / dist),
				DanmakuItems.Bullet.DAGGER, DyeColor.LIGHT_BLUE);
		e.setPos(pos);
		e.afterExpiry = new SakuyaKnifeTrail(KNIFE_SPEED, KNIFE_TRAIL_LIFE);
		e.afterExpiry.setup(holder);
		holder.shoot(e);
	}

	/**
	 * The cooldown is checked here rather than in the storm, so a flurry of damage inside one window produces
	 * one storm rather than one per hit. {@code super} still runs, so a card wanting {@link #hit} is unaffected.
	 */
	@Override
	public void hurt(CardHolder holder, DamageSource source, float amount) {
		super.hurt(holder, source, amount);
		if (hurtCooldown > 0 || holder.target() == null) return;
		hurtCooldown = HURT_COOLDOWN;
		addTicker(new SakuyaKnifeStorm<SakuyaSpell>(HURT_PER_TICK, HURT_DURATION));
	}

}
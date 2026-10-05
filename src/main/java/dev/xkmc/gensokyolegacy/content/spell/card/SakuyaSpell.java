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
 * Time Sign "Infinity Blade" — three moves, all of them knives but the first.
 * <p>
 * The card has no phases: the red bubble fan and the orbiting knife ring both run for as long as the
 * card is active, and the knife storm hangs off damage instead of off the clock. That is why this is
 * an {@link ActualSpellCard} with two ordinary methods rather than the tick-per-phase script most of
 * the cards here use — there is no single moment at which "the third move" happens.
 * <ul>
 * <li><b>The fan.</b> Forty red bubbles every {@link #FAN_INTERVAL} ticks, scattered within
 * {@link #FAN_SPREAD} degrees of the target line on both axes, at {@link #FAN_MIN_SPEED} to
 * {@link #FAN_MAX_SPEED}. Each one's life is derived from the range it is meant to cover rather than
 * picked outright, so the scatter in speed does not also scatter in reach: a slow bubble and a fast
 * one both die around the same distance out, which is what keeps the fan a fan instead of a gradient
 * that thins out halfway to the target.</li>
 * <li><b>The ring.</b> Two knives every tick, 180 degrees apart on a {@link #ORBIT_RADIUS}-block circle
 * around the caster that turns once per second. Each is over-timed to {@link #KNIFE_LIFE_FACTOR}
 * times its own flight and hands off to a {@link SakuyaKnifeTrail} on expiry.</li>
 * <li><b>The storm.</b> {@link SakuyaKnifeStorm} in response to damage, on a {@link #HURT_COOLDOWN}
 * cooldown.</li>
 * </ul>
 * Every move is driven off {@link CardHolder#target()} and does nothing without one, which keeps a
 * youkai that has lost its prey from filling the level with knives aimed at nothing.
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
	 * Forty bubbles in a loose cone at the target. Both the yaw and the pitch are scattered
	 * independently and symmetrically about the target line, so the cone is symmetric rather than
	 * lopsided the way a single spread angle applied to one axis would be.
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
	 * The ring. The circle is horizontal, at the caster's own centre height: knives thrown from
	 * somewhere above or below her read as a dome, and the two halves of a rotating ring are only
	 * legible as a ring when they share a plane.
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
	 * One knife, aimed at the target and given {@link #KNIFE_LIFE_FACTOR} times the ticks its flight
	 * needs, so it arrives with the tail of its own flight still to run and {@link SakuyaKnifeTrail}
	 * gets to send a second one back. The trail is set up here rather than left to the library, which
	 * only does it for player casts — see that class.
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
	 * The cooldown is checked here and not left to the storm, so a flurry of damage inside one window
	 * produces one storm rather than one per hit. {@code super} still runs, so a card that wants
	 * {@link #hit} counted is unaffected.
	 */
	@Override
	public void hurt(CardHolder holder, DamageSource source, float amount) {
		super.hurt(holder, source, amount);
		if (hurtCooldown > 0 || holder.target() == null) return;
		hurtCooldown = HURT_COOLDOWN;
		addTicker(new SakuyaKnifeStorm<SakuyaSpell>(HURT_PER_TICK, HURT_DURATION));
	}

}
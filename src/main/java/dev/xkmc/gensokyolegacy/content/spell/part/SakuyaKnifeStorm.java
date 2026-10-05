package dev.xkmc.gensokyolegacy.content.spell.part;

import dev.xkmc.danmakuapi.content.spell.mover.CompositeMover;
import dev.xkmc.danmakuapi.content.spell.mover.RectMover;
import dev.xkmc.danmakuapi.content.spell.mover.ZeroMover;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.Ticker;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

/**
 * The knife storm: a dense shell of knives that blooms out of the surface of a sphere wrapped around
 * the target and accelerates straight in on it.
 * <p>
 * This is the third move of {@code SakuyaSpell} and, verbatim, the whole of
 * {@code SakuyaItemSpell} — the only difference between the two is {@link #perTick}, which is why the
 * part is generic in its parent and takes the rate from its constructor rather than hard-coding it.
 * <p>
 * <b>Why the knives start at rest, and hold there.</b> A knife on the sphere is heading at the
 * target's centre, but it is also already close — which puts it inside its own hitbox — so a knife
 * launched at speed would resolve against the target on its first tick and the shell would read as
 * one solid ring rather than a converging burst. Every knife therefore sits still for {@link #delay}
 * ticks and only then accelerates along its own heading at {@link #accel}, so the shell is legible as
 * a shell: it hangs there first, then goes.
 * <p>
 * The hold is what makes the delay readable rather than merely slow. It also buys the aiming room
 * the crossing needs — measuring {@code t} from the end of the hold, a knife has covered
 * {@code 0.5 * accel * t^2}, so crossing the twelve-block radius takes about {@code t = 11}. Added to
 * the ten-tick hold that lands near tick twenty-one, inside the twenty-to-twenty-five tick life but
 * not by much, and the overshoot that follows is short. Raising {@link #accel} buys back that margin
 * if the knives need to arrive sooner; widening {@link #radius} without raising it makes them
 * expire short of the target instead.
 * <p>
 * The hold and the acceleration are two legs of a {@link CompositeMover} — a {@link ZeroMover} for
 * the former and a {@link RectMover} for the latter — rather than a mover stepped by hand, matching
 * {@code MystiaPart}'s hold-then-launch. That keeps the whole flight one path fixed by the spawn
 * packet alone, and the {@code RectMover} leg starts from the knife's own spawn point with its tick
 * offset by the hold, so the delay costs no accuracy: the knife accelerates along exactly the line it
 * was aimed on, only later. The two legs are handed {@link #delay} and {@code life - delay} ticks
 * respectively, so their windows add up to the knife's life.
 */
@SerialClass
public class SakuyaKnifeStorm<T> extends Ticker<T> {

	@SerialField
	private int perTick = 80, duration = 10;
	@SerialField
	private double radius = 12, accel = 0.2;
	/**
	 * Ticks a knife hangs at its spawn point before it starts accelerating, so the shell is legible
	 * as a shell before it collapses inward. Comfortably shorter than a knife's life, so there is
	 * always flight left after the hold.
	 */
	@SerialField
	private int delay = 10;

	public SakuyaKnifeStorm() {
	}

	public SakuyaKnifeStorm(int perTick, int duration) {
		this.perTick = perTick;
		this.duration = duration;
	}

	@Override
	public boolean tick(CardHolder holder, T card) {
		step(holder);
		super.tick(holder, card);
		return tick >= duration;
	}

	/**
	 * With no target there is nothing to converge on and nothing to be thrown at, so the ticks are
	 * spent and the storm ends. It is not worth rescheduling itself for a target that may never come:
	 * the card itself only creates one storm on damage, and an entity that has lost its target has
	 * already stopped attacking.
	 */
	private void step(CardHolder holder) {
		var target = holder.target();
		if (target == null) return;
		var r = holder.random();
		for (int i = 0; i < perTick; i++) {
			var pos = target.add(onSphere(r, radius));
			var heading = target.subtract(pos);
			if (heading.lengthSqr() < 1e-4) continue;
			var dir = heading.normalize();
			int life = 20 + r.nextInt(6);
			var e = holder.prepareDanmaku(life, Vec3.ZERO,
					DanmakuItems.Bullet.DAGGER, DyeColor.LIGHT_BLUE);
			e.setPos(pos);
			var mover = new CompositeMover();
			mover.add(delay, new ZeroMover(dir, dir, delay));
			mover.add(life - delay, new RectMover(pos, Vec3.ZERO, dir.scale(accel)));
			e.mover = mover;
			holder.shoot(e);
		}
	}

	/**
	 * A point at exactly {@code radius} from the origin, with the direction drawn uniformly over the
	 * sphere rather than by polar angles — sampling the two angles independently would crowd the
	 * poles and thin out the equator, which on a six-block shell is visible.
	 */
	private static Vec3 onSphere(RandomSource r, double radius) {
		var v = new Vec3(r.nextDouble() * 2 - 1, r.nextDouble() * 2 - 1, r.nextDouble() * 2 - 1);
		double len = v.length();
		return len < 1e-4 ? new Vec3(0, radius, 0) : v.scale(radius / len);
	}

}
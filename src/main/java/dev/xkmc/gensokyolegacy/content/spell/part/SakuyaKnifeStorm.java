package dev.xkmc.gensokyolegacy.content.spell.part;

import dev.xkmc.danmakuapi.content.spell.mover.RectMover;
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
 * <b>Why the knives start at rest.</b> A knife on the sphere is heading at the target's centre, but
 * it is also already close — six blocks, which is inside its own hitbox — so a knife launched at
 * speed would resolve against the target on its first tick and the shell would read as one solid
 * ring rather than a converging burst. Starting every knife at zero and accelerating along the same
 * heading at {@link #accel} separates the shell into an expanding-looking front instead: at tick
 * {@code t} a knife has covered {@code 0.5 * accel * t^2}, which is under a block at {@code t = 2}
 * and about six at {@code t = 8}, roughly where the target sits. The life window of twenty to
 * twenty-five ticks is deliberately longer than that crossing, so the survivors sail past the target
 * and fade behind it rather than all vanishing on the same tick.
 * <p>
 * The acceleration is expressed as a {@link RectMover} with a zero initial velocity rather than by
 * stepping the movement by hand, which keeps the whole flight one fixed geometric path that the
 * client can draw from the spawn packet alone.
 */
@SerialClass
public class SakuyaKnifeStorm<T> extends Ticker<T> {

	@SerialField
	private int perTick = 80, duration = 20;
	@SerialField
	private double radius = 12, accel = 0.2;

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
			var e = holder.prepareDanmaku(20 + r.nextInt(6), Vec3.ZERO,
					DanmakuItems.Bullet.DAGGER, DyeColor.LIGHT_BLUE);
			e.setPos(pos);
			e.mover = new RectMover(pos, Vec3.ZERO, heading.normalize().scale(accel));
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
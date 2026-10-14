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
 * The knife storm: a dense shell of knives blooming off a sphere around the target and accelerating into it. Every
 * knife holds for {@link #delay} ticks first, since launched at speed it would hit on its first tick and read as a
 * ring.
 */
@SerialClass
public class SakuyaKnifeStorm<T> extends Ticker<T> {

	@SerialField
	private int perTick = 80, duration = 10;
	@SerialField
	private double radius = 12, accel = 0.2;
	/**
	 * Ticks a knife hangs at its spawn point before it starts accelerating, so the shell is legible as a shell
	 * before it collapses inward; comfortably shorter than a knife's life, so flight is left after the hold.
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
	 * With no target there is nothing to converge on or be thrown at, so the ticks are spent and the storm ends.
	 * Rescheduling for a target that may never come is not worth it: one that has lost its target has stopped
	 * attacking.
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
	 * sphere rather than by polar angles, which would crowd the poles and thin out the equator.
	 */
	private static Vec3 onSphere(RandomSource r, double radius) {
		var v = new Vec3(r.nextDouble() * 2 - 1, r.nextDouble() * 2 - 1, r.nextDouble() * 2 - 1);
		double len = v.length();
		return len < 1e-4 ? new Vec3(0, radius, 0) : v.scale(radius / len);
	}

}
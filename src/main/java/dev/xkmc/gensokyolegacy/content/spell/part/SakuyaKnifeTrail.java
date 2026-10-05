package dev.xkmc.gensokyolegacy.content.spell.part;

import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.TrailAction;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

/**
 * The second stage of a knife thrown by {@code SakuyaSpell}'s orbiting ring: fired the moment the
 * first stage's life runs out, aimed at where the target is <em>now</em>.
 * <p>
 * The first stage is deliberately over-timed — it lives {@code 1.5x} the time its own flight needs
 * to reach the target, so it sails past rather than stopping on the target. The re-aim at expiry is
 * what turns the shot back: the first stage commits to a fixed line, and this one picks up the
 * target from a fresh sample and flies straight in. No mover is involved on either half, so the
 * whole trajectory is two constant-velocity legs meeting at a server-chosen point, which is the same
 * shape {@code ReimuPart} and {@code DaggerHomingTrail} use.
 * <p>
 * The successor carries no trail of its own: the chain is exactly one handoff, not a recursion. It
 * flies {@link #life} ticks at {@link #speed} and is then simply gone.
 * <p>
 * <b>The holder has to be pushed in by hand.</b> {@code TrailAction#setup} is what
 * {@code PlayerHolder#shoot} calls, so a player-cast spell arrives here with a holder already
 * cached. A youkai does not: {@code ItemBulletEntity#terminate} recovers a holder from the danmaku's
 * owner, and a {@code YoukaiEntity} is not itself a {@code CardHolder} — it merely owns one. Left to
 * the library, every youkai-thrown knife would arrive here with nothing cached and expire silently.
 * Hence the explicit {@code setup} call at each spawn site.
 */
@SerialClass
public class SakuyaKnifeTrail extends TrailAction {

	@SerialField
	private double speed = 2;
	@SerialField
	private int life = 40;

	public SakuyaKnifeTrail() {
	}

	public SakuyaKnifeTrail(double speed, int life) {
		this.speed = speed;
		this.life = life;
	}

	/**
	 * Aiming is re-read from {@link CardHolder#target()} rather than reused from the first stage, so
	 * a target that has walked during the first stage's flight is still met. A target that has gone
	 * away takes the knife with it: the flight simply ends, which is what the card does everywhere
	 * else its target disappears.
	 */
	@Override
	public void execute(CardHolder holder, Vec3 pos, Vec3 dir) {
		var target = holder.target();
		if (target == null) return;
		var heading = target.subtract(pos);
		if (heading.lengthSqr() < 1e-4) return;
		var e = holder.prepareDanmaku(life, heading.normalize().scale(speed),
				DanmakuItems.Bullet.DAGGER, DyeColor.LIGHT_BLUE);
		e.setPos(pos);
		holder.shoot(e);
	}

}
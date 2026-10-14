package dev.xkmc.gensokyolegacy.content.spell.part;

import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.TrailAction;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;

/**
 * The second stage of a knife from {@code SakuyaSpell}'s orbiting ring: fired as the first stage's life runs out, aimed
 * where the target is <em>now</em>. A youkai's holder is never cached, so {@code TrailAction#setup} must be called by
 * hand.
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
	 * Aiming is re-read from {@link CardHolder#target()} rather than reused from the first stage, so a target that
	 * has walked during the first stage's flight is still met. One that has gone away takes the knife with it.
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
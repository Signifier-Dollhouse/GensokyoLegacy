package dev.xkmc.gensokyolegacy.content.spell.item;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.spell.item.ItemSpell;
import dev.xkmc.danmakuapi.content.spell.mover.PolarMover;
import dev.xkmc.danmakuapi.content.spell.spellcard.CardHolder;
import dev.xkmc.danmakuapi.content.spell.spellcard.Ticker;
import dev.xkmc.gensokyolegacy.content.entity.misc.ButterflyDanmakuBulletEntity;
import dev.xkmc.gensokyolegacy.content.item.ghost.ButterflyDanmakuItem;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2serial.serialization.SerialClass;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The fan's scatter mode: a storm of butterflies summoned at the caster and spiralling away from
 * them. Every butterfly orbits the caster with an ever-growing radius, so it leaves the centre
 * while still curving around it.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
@SerialClass
public class YuyukoScatterItemSpell extends ItemSpell {

	@Override
	public void start(LivingEntity player, @Nullable LivingEntity target) {
		super.start(player, target);
		addTicker(new Scatter());
	}

	@SerialClass
	public static class Scatter extends Ticker<YuyukoScatterItemSpell> {

		private static final int PER_TICK = 100;
		private static final int DURATION = 20;
		private static final int LIFE = 40;

		@Override
		public boolean tick(CardHolder holder, YuyukoScatterItemSpell card) {
			Vec3 origin = holder.center();
			RandomSource random = holder.random();
			for (int i = 0; i < 20; i++) {
				ButterflyDanmakuItem item = (tick + i) % 2 == 0
						? GLItems.PINK_BUTTERFLY.get() : GLItems.BLUE_BUTTERFLY.get();
				Vec3 dir = randomDirection(random);
				var ori = DanmakuHelper.getOrientation(dir);
				ButterflyDanmakuBulletEntity danmaku = new ButterflyDanmakuBulletEntity(
						GLEntities.BUTTERFLY_DANMAKU.get(), holder.self(), holder.self().level());
				danmaku.setHaunting(true);
				danmaku.setItem(new ItemStack(item));
				danmaku.setup(item.type.damage(), LIFE + random.nextInt(10), true, true, Vec3.ZERO);
				danmaku.setPos(origin);
				danmaku.mover = PolarMover.ofPlane(origin, dir, ori.normal())
						.radial(1 + random.nextDouble() * 2, 0.4, 0)
						.angular(0, 0.03 * ((tick + i) % 2 * 2 - 1), 0);
				holder.shoot(danmaku);
			}
			super.tick(holder, card);
			return tick >= DURATION;
		}

		private static Vec3 randomDirection(RandomSource random) {
			Vec3 vec = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
			return vec.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : vec.normalize();
		}

	}

}

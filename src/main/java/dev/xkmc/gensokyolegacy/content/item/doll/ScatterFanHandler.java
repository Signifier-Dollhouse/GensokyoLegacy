package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.danmakuapi.content.entity.DanmakuHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.fastprojectileapi.collision.EntityStorageHelper;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Scatters a fan of butterfly danmaku: 21 rays three degrees apart, five danmaku per ray.
 * Within a ray the danmaku are spaced evenly over its colour's range and each moves at the
 * distance it spawned at, so the outermost one is the fastest.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
final class ScatterFanHandler implements YuyukoFanModeHandler {

	private static final int COST = 10;
	private static final int RAYS = 21;
	private static final double RAY_ANGLE = 3;
	private static final int PER_RAY = 5;
	private static final int LIFE = 40;

	@Override
	public boolean use(ServerPlayer sp, ItemStack stack, DollAttachment att) {
		if (SpiritualPower.get(stack) < COST) {
			sp.displayClientMessage(GLLang.Ghost.NEED_POWER.get(COST), true);
			return false;
		}
		SpiritualPower.add(stack, -COST);
		var orientation = DanmakuHelper.getOrientation(sp.getViewVector(1).normalize());
		var center = sp.position().add(0, sp.getBbHeight() / 2, 0);
		var level = sp.serverLevel();
		var r = sp.getRandom();
		for (int i = 0; i < RAYS; i++) {
			boolean odd = i % 2 == 0;
			double min = 0.55;
			if (odd) min += 0.1;
			for (int j = 0; j < PER_RAY; j++) {
				boolean pink = (i / 2 + j) % 2 == 0;
				DanmakuItem item = (pink ? GLItems.PINK_BUTTERFLY : GLItems.BLUE_BUTTERFLY).get();
				Vec3 dir = orientation.rotateDegrees((i - RAYS / 2 + 1d * j / PER_RAY) * RAY_ANGLE);
				double dist = min + 0.2 * j;
				int start = r.nextInt(20);
				ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), sp, level);
				danmaku.setItem(new ItemStack(item));
				danmaku.setup((float) item.type.damage(), start + (int) ((60 + r.nextInt(10)) / dist), true, item.type.bypass(), dir.scale(dist));
				danmaku.setPos(center.add(dir.scale(dist)));
				danmaku.tickCount = start;
				EntityStorageHelper.fastAdd(level, danmaku);
			}
		}
		return true;
	}

	@Override
	public ItemStack icon() {
		return new ItemStack(GLItems.PINK_BUTTERFLY.get());
	}

}

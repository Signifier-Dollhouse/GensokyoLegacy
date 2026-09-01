package dev.xkmc.gensokyolegacy.content.item.hexbrew;

import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.RenderableDanmakuType;
import dev.xkmc.danmakuapi.content.render.RotatingProjectileType;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class StarDanmakuItem extends DanmakuItem {

	public StarDanmakuItem(Properties p) {
		super(p, DanmakuItems.Bullet.STAR, DyeColor.YELLOW, DanmakuItems.Bullet.STAR.size);
	}

	@Override
	protected RenderableDanmakuType<?, ?> buildRenderer() {
		ResourceLocation loc = GensokyoLegacy.loc("textures/item/hexbrew/star.png");
		return new RotatingProjectileType(loc, type.display(), 40);
	}

	@Override
	public double modifyFading(double selfFading) {
		return selfFading / 2;
	}

}

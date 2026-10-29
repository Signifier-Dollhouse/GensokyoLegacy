package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.ButterflyProjectileType;
import dev.xkmc.danmakuapi.content.render.RenderableDanmakuType;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * A butterfly danmaku drawn from its own texture, so the fan can fire one colour per ray
 * instead of the shared dye palette.
 */
public class ButterflyDanmakuItem extends DanmakuItem {

	private final ResourceLocation texture;

	public ButterflyDanmakuItem(Properties p, DyeColor color, ResourceLocation texture) {
		super(p, DanmakuItems.Bullet.BUTTERFLY, color, DanmakuItems.Bullet.BUTTERFLY.size);
		this.texture = texture;
	}

	@Override
	protected RenderableDanmakuType<?, ?> buildRenderer() {
		return new ButterflyProjectileType(texture, type.display(), 20);
	}

	@Override
	public double modifyFading(double selfFading) {
		return selfFading / 2 + 0.5;
	}

}

package dev.xkmc.gensokyolegacy.content.item.anchor;

import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.RenderableDanmakuType;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.item.anchor.client.DropletProjectileType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * The water droplet the storm anchor sheds and bursts into, drawn from its own texture instead of
 * the shared ball palette, and leaning along its flight while staying camera-facing.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class DropletDanmakuItem extends DanmakuItem {

	private final ResourceLocation texture;

	public DropletDanmakuItem(Properties p, DyeColor color, ResourceLocation texture) {
		super(p, DanmakuItems.Bullet.BALL, color, 0.7f);
		this.texture = texture;
	}

	@Override
	protected RenderableDanmakuType<?, ?> buildRenderer() {
		return new DropletProjectileType(texture, type.display());
	}

	@Override
	public double modifyFading(double selfFading) {
		return selfFading / 2 + 0.5;
	}

}

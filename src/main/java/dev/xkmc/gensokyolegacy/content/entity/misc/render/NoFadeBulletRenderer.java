package dev.xkmc.gensokyolegacy.content.entity.misc.render;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.entity.ItemBulletRenderer;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * A thrown weapon is not a danmaku: it keeps full opacity at every range and is never hidden from
 * its own thrower, so the throw is visible from the first tick.
 *
 * @see "doc/design/nue_trident.md"
 */
public class NoFadeBulletRenderer<T extends ItemBulletEntity> extends ItemBulletRenderer<T> {

	public NoFadeBulletRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public double fading(SimplifiedProjectile e) {
		return 1;
	}

	@Override
	public boolean shouldRender(T e, Frustum frustum, double camX, double camY, double camZ) {
		return true;
	}

}

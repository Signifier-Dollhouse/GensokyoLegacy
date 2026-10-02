package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.ItemModelProjectileType;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import net.minecraft.world.item.DyeColor;

/**
 * Iron dagger: a thrown {@link DanmakuItems.Bullet#DAGGER} danmaku.
 * <p>
 * The default {@code buildRenderer} of the dagger bullet is an {@link ItemModelProjectileType},
 * so the danmaku is drawn from this item's own baked model instead of a hand-written quad and
 * the three-dimensional {@code models/custom/iron_dagger.json} doubles as the projectile art.
 * That renderer lays the item model flat with its image top pointing along the flight
 * direction, which is why the model is authored tip-up in the standard item layout; one unit
 * of model is one texel of {@code item/tool/iron_dagger.png}.
 * <p>
 * The model is deliberately parentless: {@code ModelBakery} rebuilds any model whose parent
 * chain bottoms out at {@code builtin/generated} (as {@code item/handheld} does) from its
 * {@code layer0}/{@code layer1} textures alone, which would discard the {@code elements} and
 * bake to zero quads. The hand-held display transforms are therefore spelled out inline.
 */
public class IronDaggerItem extends DanmakuItem {

	public IronDaggerItem(Properties p) {
		super(p, DanmakuItems.Bullet.DAGGER, DyeColor.WHITE, DanmakuItems.Bullet.DAGGER.size);
	}

	@Override
	public double modifyFading(double selfFading) {
		return selfFading / 2 + 0.5;
	}

}
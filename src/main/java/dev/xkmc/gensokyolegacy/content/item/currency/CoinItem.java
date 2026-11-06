package dev.xkmc.gensokyolegacy.content.item.currency;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.RenderableDanmakuType;
import dev.xkmc.danmakuapi.content.render.SimpleProjectileType;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.CoinDanmakuBulletEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * A coin thrown as a yellow circle danmaku. A paid throw lands as the coin that was spent;
 * a doll fires the same coin through the shared bullet, which leaves nothing behind.
 */
public class CoinItem extends DanmakuItem {

	public CoinItem(Properties p) {
		super(p, DanmakuItems.Bullet.CIRCLE, DyeColor.YELLOW, DanmakuItems.Bullet.CIRCLE.size);
	}

	@Override
	protected RenderableDanmakuType<?, ?> buildRenderer() {
		return new SimpleProjectileType(GensokyoLegacy.loc("textures/item/currency/coin.png"), type.display());
	}

	@Override
	protected ItemBulletEntity newBullet(Player player, Level level) {
		return new CoinDanmakuBulletEntity(GLEntities.COIN.get(), player, level);
	}

	@Override
	protected void spawnBullet(ItemBulletEntity danmaku, Player player, Level level, DanmakuUseEvent event) {
		((CoinDanmakuBulletEntity) danmaku).setDroppable(event.consume());
		super.spawnBullet(danmaku, player, level, event);
	}

}

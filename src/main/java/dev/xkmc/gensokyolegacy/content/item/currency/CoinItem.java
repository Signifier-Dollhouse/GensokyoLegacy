package dev.xkmc.gensokyolegacy.content.item.currency;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.RenderableDanmakuType;
import dev.xkmc.danmakuapi.content.render.SimpleProjectileType;
import dev.xkmc.danmakuapi.content.spell.mover.RectMover;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.CoinDanmakuBulletEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A coin thrown as a yellow circle danmaku. A paid throw arcs down under gravity and lands as the
 * coin that was spent; a doll fires the same coin through the shared bullet, which leaves nothing behind.
 */
public class CoinItem extends DanmakuItem {

	private static final Vec3 GRAVITY = new Vec3(0, -0.05, 0);

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
		Vec3 dir = RayTraceUtil.getRayTerm(Vec3.ZERO, player.getXRot(), player.getYRot(), 2);
		Vec3 pos = RayTraceUtil.getRayTerm(player.getEyePosition(), player.getXRot(), player.getYRot(), 2);
		// before the parent adds the bullet, so the client receives the mover in its spawn packet
		danmaku.mover = new RectMover(pos, dir, GRAVITY);
		super.spawnBullet(danmaku, player, level, event);
	}


	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemCommon.CURRENCY.get());
	}

	protected int cooldown() {
		return 10;
	}

	@Override
	public double modifyFading(double selfFading) {
		return selfFading / 2 + 0.5;
	}
}

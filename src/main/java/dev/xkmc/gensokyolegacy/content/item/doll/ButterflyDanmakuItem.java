package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.ButterflyProjectileType;
import dev.xkmc.danmakuapi.content.render.RenderableDanmakuType;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.ButterflyDanmakuBulletEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

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

	/**
	 * The petal this butterfly trails: the pink blossom for the pink butterfly, the purple one for
	 * the blue.
	 */
	public ParticleOptions trailParticle() {
		return (color == DyeColor.PINK ? GLParticles.PINK_CHERRY_BLOSSOM : GLParticles.PURPLE_CHERRY_BLOSSOM).get();
	}

	@Override
	protected ItemBulletEntity newBullet(Player player, Level level) {
		return new ButterflyDanmakuBulletEntity(GLEntities.BUTTERFLY_DANMAKU.get(), player, level);
	}

}

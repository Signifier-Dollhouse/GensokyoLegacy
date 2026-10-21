package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.ItemModelProjectileType;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Iron dagger: a thrown {@link DanmakuItems.Bullet#DAGGER} danmaku whose bullet returns, so the dagger is reused. Its
 * {@link ItemModelProjectileType} renderer lays the model flat with image top along flight, hence tip-up one unit per
 * texel, and the model must stay parentless since {@code ModelBakery} discards {@code elements} for anything ending at
 * {@code builtin/generated}.
 *
 * @see "doc/design/dagger_glove.md"
 */
public class IronDaggerItem extends DanmakuItem {

	public IronDaggerItem(Properties p) {
		super(p, DanmakuItems.Bullet.DAGGER, DyeColor.WHITE, DanmakuItems.Bullet.DAGGER.size);
	}

	@Override
	public double modifyFading(double selfFading) {
		return selfFading / 2 + 0.5;
	}

	/**
	 * Throws an {@link IronDaggerBulletEntity} instead of the shared danmaku bullet, so that this
	 * one can be handed back when it lands.
	 */
	@Override
	protected ItemBulletEntity newBullet(Player player, Level level) {
		return new IronDaggerBulletEntity(GLEntities.IRON_DAGGER.get(), player, level);
	}

	/**
	 * Tells the dagger whether this throw must be paid back: the throw is the only moment that knows, and a throw made
	 * in creative, or one a listener cleared {@link DanmakuUseEvent#consume()} on, spent nothing and must hand nothing
	 * back.
	 */
	@Override
	protected void spawnBullet(ItemBulletEntity danmaku, Player player, Level level, DanmakuUseEvent event) {
		((IronDaggerBulletEntity) danmaku).setReturnable(event.consume());
		super.spawnBullet(danmaku, player, level, event);
	}

	@Override
	protected void playThrowSound(Level level, Player player) {
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(),
				SoundSource.PLAYERS, 1F, 1F);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.IRON_DAGGER_LORE.get());
	}

}

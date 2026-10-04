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
 * <p>
 * The throw itself is {@link DanmakuItem}'s; what makes this a thrown weapon rather than a
 * danmaku item is the bullet it throws and the fact that the bullet comes back: the dagger is
 * spent from the stack on every throw and handed to the thrower again when it lands, so it is
 * reused rather than lost.
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
	 * Tells the dagger whether this throw has to be paid back before it goes anywhere else, since
	 * the throw is the only moment that knows: a throw made in creative, or one a listener cleared
	 * {@link DanmakuUseEvent#consume()} on, spent nothing and must hand nothing back. The bullet
	 * is this item's own, see {@link #newBullet}.
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

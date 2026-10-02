package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.render.ItemModelProjectileType;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.data.DanmakuConfig;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.gensokyolegacy.content.entity.misc.IronDaggerBulletEntity;
import dev.xkmc.gensokyolegacy.init.registrate.GLEntities;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

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

	/**
	 * Throws the dagger as an {@link IronDaggerBulletEntity} rather than the shared danmaku bullet,
	 * so that it can be handed back to this player when it lands. Everything else follows
	 * {@link DanmakuItem#use}, including the shrink: the dagger still costs one from the stack on
	 * every throw, and returning it is what makes the throw reusable.
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (GrazeHelper.forbidDanmaku(player))
			return InteractionResultHolder.fail(stack);
		int cooldown = DanmakuConfig.SERVER.playerDanmakuCooldown.get();
		var event = new DanmakuUseEvent(player, stack, cooldown);
		NeoForge.EVENT_BUS.post(event);
		if (event.isCanceled()) {
			return InteractionResultHolder.fail(stack);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!level.isClientSide) {
			ItemBulletEntity danmaku = new IronDaggerBulletEntity(GLEntities.IRON_DAGGER.get(), player, level);
			danmaku.setItem(stack);
			danmaku.setup(type.damage(), 40, false, type.bypass(),
					RayTraceUtil.getRayTerm(Vec3.ZERO, player.getXRot(), player.getYRot(), 2));
			danmaku.moveTo(RayTraceUtil.getRayTerm(player.getEyePosition(), player.getXRot(), player.getYRot(), 2));
			level.addFreshEntity(danmaku);
			if (player instanceof ServerPlayer sp)
				SpellContainer.track(sp, danmaku);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, event.getCooldown());
		if (event.consume()) {
			stack.shrink(1);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

}
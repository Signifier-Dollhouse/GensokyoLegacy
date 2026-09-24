package dev.xkmc.gensokyolegacy.content.item.tool;

import dev.xkmc.danmakuapi.api.DanmakuUseEvent;
import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.entity.ItemBulletEntity;
import dev.xkmc.danmakuapi.content.spell.item.SpellContainer;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.gensokyolegacy.content.item.hexbrew.StarDanmakuItem;
import dev.xkmc.l2library.content.raytrace.RayTraceUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Star wand: a reusable star-danmaku shooter with a fixed 1s cooldown.
 * Never consumed. Extends {@link StarDanmakuItem} (a {@code DanmakuItem}),
 * so dolls pick it up for their danmaku attack automatically.
 */
public class StarWandItem extends StarDanmakuItem {

	public StarWandItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (GrazeHelper.forbidDanmaku(player))
			return InteractionResultHolder.fail(stack);
		var event = new DanmakuUseEvent(player, stack, 20);
		NeoForge.EVENT_BUS.post(event);
		if (event.isCanceled()) {
			return InteractionResultHolder.fail(stack);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!level.isClientSide) {
			ItemBulletEntity danmaku = new ItemBulletEntity(DanmakuEntities.ITEM_DANMAKU.get(), player, level);
			danmaku.setItem(stack);
			danmaku.setup(type.damage(), 40, false, type.bypass(),
					RayTraceUtil.getRayTerm(Vec3.ZERO, player.getXRot(), player.getYRot(), 2));
			danmaku.moveTo(RayTraceUtil.getRayTerm(player.getEyePosition(), player.getXRot(), player.getYRot(), 2));
			level.addFreshEntity(danmaku);
			if (player instanceof ServerPlayer sp)
				SpellContainer.track(sp, danmaku);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		player.getCooldowns().addCooldown(this, 20);
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

}

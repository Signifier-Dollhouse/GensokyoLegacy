package dev.xkmc.gensokyolegacy.content.item.doll;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostPowerHolder;
import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import dev.xkmc.l2itemselector.init.data.L2Keys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Yuyuko's fan spends one spiritual power pool on ghosts or on a butterfly fan: absorb a ghost,
 * tend every summoned one, or scatter danmaku. The ghost modes are all ghosts at once rather than
 * one selected type.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class YuyukoFanItem extends Item {

	/**
	 * Intrinsic capacity stamped onto every fan; other spiritual items pick their own.
	 */
	public static final int MAX_POWER = 10000;

	private static final int COOLDOWN = 10;

	public YuyukoFanItem(Properties properties) {
		super(properties);
	}

	public static YuyukoFanMode getMode(ItemStack stack) {
		return GLItems.YUYUKO_FAN_MODE.getOrDefault(stack, YuyukoFanMode.SUMMON);
	}

	public static void setMode(ItemStack stack, YuyukoFanMode mode) {
		GLItems.YUYUKO_FAN_MODE.set(stack, mode);
	}

	public static ItemStack iconStack(YuyukoFanMode mode) {
		return mode.icon();
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack stack, Player player,
												   LivingEntity target, InteractionHand hand) {
		boolean stray = target instanceof StrayGhostEntity;
		boolean owned = target instanceof GhostEntity;
		if (!stray && !owned) return InteractionResult.PASS;
		if (player.level().isClientSide()) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
		if (sp.getCooldowns().isOnCooldown(this)) return InteractionResult.FAIL;
		int max = SpiritualPower.max(stack);
		int stored = SpiritualPower.get(stack);
		if (max <= 0) return InteractionResult.FAIL;
		if (owned) {
			GhostEntity ghost = (GhostEntity) target;
			if (!ghost.isOwner(sp)) {
				sp.displayClientMessage(GLLang.Ghost.NOT_YOURS.get(), true);
				return InteractionResult.FAIL;
			}
			if (stored >= max) {
				return release(sp, ghost, hand);
			}
		}
		if (stored >= max) {
			sp.displayClientMessage(GLLang.Ghost.FAN_FULL.get(), true);
			return InteractionResult.FAIL;
		}
		int power = ((GhostPowerHolder) target).getPower();
		if (power <= 0) {
			sp.displayClientMessage(GLLang.Ghost.NO_POWER.get(), true);
			return InteractionResult.FAIL;
		}
		int take = Math.min(power, max - stored);
		if (owned) {
			GhostEntity ghost = (GhostEntity) target;
			if (!GLMeta.DOLL.type().getOrCreate(sp).absorb(sp, ghost)) {
				sp.displayClientMessage(GLLang.Ghost.ABSORB_FAIL.get(), true);
				return InteractionResult.FAIL;
			}
		} else {
			target.discard();
		}
		// Vanilla hands creative players a copy of the stack; write the held one so the power sticks.
		SpiritualPower.add(sp.getItemInHand(hand), take);
		sp.displayClientMessage(GLLang.Ghost.ABSORBED.get(take), true);
		play(sp, 1.2F);
		sp.getCooldowns().addCooldown(this, COOLDOWN);
		sp.swing(hand, true);
		return InteractionResult.SUCCESS;
	}

	/**
	 * The full-fan escape hatch: an owned ghost that cannot be stored is set free rather than
	 * destroyed with its power still in it.
	 */
	private InteractionResult release(ServerPlayer sp, GhostEntity ghost, InteractionHand hand) {
		if (!GLMeta.DOLL.type().getOrCreate(sp).release(sp, ghost)) {
			sp.displayClientMessage(GLLang.Ghost.RELEASE_FAIL.get(), true);
			return InteractionResult.FAIL;
		}
		sp.displayClientMessage(GLLang.Ghost.RELEASED.get(), true);
		play(sp, 0.8F);
		sp.getCooldowns().addCooldown(this, COOLDOWN);
		sp.swing(hand, true);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
		if (sp.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
		DollAttachment att = GLMeta.DOLL.type().getOrCreate(sp);
		if (!getMode(stack).handler.use(sp, stack, att)) return InteractionResultHolder.fail(stack);
		return used(sp, stack, hand);
	}

	private InteractionResultHolder<ItemStack> used(ServerPlayer sp, ItemStack stack, InteractionHand hand) {
		play(sp, 1.0F);
		sp.awardStat(Stats.ITEM_USED.get(this));
		sp.getCooldowns().addCooldown(this, COOLDOWN);
		sp.swing(hand, true);
		return InteractionResultHolder.sidedSuccess(stack, false);
	}

	private static void play(ServerPlayer player, float pitch) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.8F, pitch);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		list.add(GLLang.ItemLores.YUYUKO_FAN_LORE.get());
		SpiritualPower.appendHoverText(stack, list);
		list.add(GLLang.ItemYuyukoFan.ABSORB.get());
		list.add(GLLang.ItemYuyukoFan.SUMMON.get());
		list.add(GLLang.ItemYuyukoFan.ABSORB_ALL.get());
		list.add(GLLang.ItemYuyukoFan.SCATTER.get());
		list.add(GLLang.ItemYuyukoFan.RELEASE.get());
		list.add(GLLang.ItemYuyukoFan.SELECTED.get(getMode(stack).getDisplayName()));
		list.add(GLLang.ItemGlove.WHEEL.get(L2Keys.WHEEL.map.getKey().getDisplayName())
				.withStyle(ChatFormatting.GRAY));
	}

}

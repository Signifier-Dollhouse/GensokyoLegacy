package dev.xkmc.gensokyolegacy.content.block.deco.donation;

import dev.xkmc.gensokyolegacy.content.item.spiritual.SpiritualPower;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class HakureiDonationBox extends DonationBoxShape implements UseItemOnBlockMethod {

	private static final int EFFECT_TICKS_PER_COIN = 20 * 60;
	private static final int POWER_PER_COIN = 10;

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
										   Player player, InteractionHand hand, BlockHitResult hit) {
		int coins;
		if (stack.is(GLItems.COIN)) {
			coins = 1;
		} else if (stack.is(GLItems.COIN_STACK)) {
			coins = GLItems.COIN_STACK_VALUE;
		} else {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!level.isClientSide()) {
			stack.shrink(1);
			int duration = coins * EFFECT_TICKS_PER_COIN;
			player.addEffect(new MobEffectInstance(GLEffects.LOOTING, duration));
			player.addEffect(new MobEffectInstance(GLEffects.HAKUREI_BLESSING, duration));
			SpiritualPower.restore(player, coins * POWER_PER_COIN);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide());
	}

}

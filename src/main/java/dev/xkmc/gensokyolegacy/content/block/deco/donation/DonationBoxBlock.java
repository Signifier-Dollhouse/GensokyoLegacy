package dev.xkmc.gensokyolegacy.content.block.deco.donation;

import dev.xkmc.l2modularblock.mult.SetPlacedByBlockMethod;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import dev.xkmc.l2modularblock.one.EntityInsideBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Right-clicking a donation box with a coin inserts that one coin, and a coin thrown onto it is
 * swallowed by {@link #entityInside}.
 *
 * @see "doc/design/structure/donation.md"
 */
public class DonationBoxBlock implements UseItemOnBlockMethod, SetPlacedByBlockMethod, EntityInsideBlockMethod {

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
											Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof DonationBoxBlockEntity be)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		int coins = DonationBoxBlockEntity.coinsOf(stack);
		if (coins <= 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (level.isClientSide) {
			return be.linked() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (player instanceof ServerPlayer sp && be.insert(sp, coins)) {
			stack.shrink(1);
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof DonationBoxBlockEntity be) {
			be.tryLink(sl);
		}
	}

	/**
	 * A coin item entity that lands on the box overlaps its block cell, so the whole stack is
	 * consumed here instead of being scanned for by a tick handler.
	 */
	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (level.isClientSide) return;
		if (!(entity instanceof ItemEntity item)) return;
		if (item.isRemoved()) return;
		if (!(item.getOwner() instanceof ServerPlayer player)) return;
		int coins = DonationBoxBlockEntity.coinsOf(item.getItem());
		if (coins <= 0) return;
		if (!(level.getBlockEntity(pos) instanceof DonationBoxBlockEntity be)) return;
		if (be.insert(player, coins * item.getItem().getCount())) {
			item.discard();
		}
	}

}

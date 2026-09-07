package dev.xkmc.gensokyolegacy.content.block.deco.seat;

import dev.xkmc.l2modularblock.mult.UseWithoutItemBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public record SeatableImpl() implements UseWithoutItemBlockMethod {

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult result) {
		if (player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		List<ChairEntity> seats = level.getEntitiesOfClass(ChairEntity.class, new AABB(pos));
		if (!seats.isEmpty()) {
			ChairEntity seat = seats.getFirst();
			List<Entity> passengers = seat.getPassengers();
			if (!passengers.isEmpty() && passengers.getFirst() instanceof Player) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide) {
				seat.ejectPassengers();
				player.startRiding(seat);
			}
			return InteractionResult.SUCCESS;
		}
		if (!level.isClientSide && state.getBlock() instanceof ISeatableBlock block) {
			block.sitDown(level, pos, player);
		}
		return InteractionResult.SUCCESS;
	}

}

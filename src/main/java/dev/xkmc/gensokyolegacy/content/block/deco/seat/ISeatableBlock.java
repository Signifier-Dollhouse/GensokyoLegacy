package dev.xkmc.gensokyolegacy.content.block.deco.seat;

import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;

public interface ISeatableBlock {

	static DelegateBlock of(BlockBehaviour.Properties p, float offset, BlockMethod... impl) {
		return new SeatableDelegateBlockImpl(p, offset, impl);
	}

	float offset();

	default void sitDown(Level world, BlockPos pos, Entity entity) {
		if (world.isClientSide) return;
		if (isSeatOccupied(world, pos)) return;
		ChairEntity seat = new ChairEntity(world, pos);
		seat.setPos(pos.getX() + 0.5F, pos.getY() + offset(), pos.getZ() + 0.5F);
		world.addFreshEntity(seat);
		entity.startRiding(seat, true);
	}

	static boolean isSeatOccupied(Level world, BlockPos pos) {
		return !world.getEntitiesOfClass(ChairEntity.class, new AABB(pos)).isEmpty();
	}

}

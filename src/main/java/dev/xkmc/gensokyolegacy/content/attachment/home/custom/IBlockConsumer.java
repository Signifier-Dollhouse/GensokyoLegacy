package dev.xkmc.gensokyolegacy.content.attachment.home.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlockConsumer {

	void consume(BlockPos pos, BlockState state);

}

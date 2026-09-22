package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

public class BlueFirLogBlock extends RotatedPillarBlock {

	private static final MapCodec<BlueFirLogBlock> CODEC = simpleCodec(BlueFirLogBlock::new);

	public BlueFirLogBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends RotatedPillarBlock> codec() {
		return CODEC;
	}

	@Override
	public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
		if (ability == ItemAbilities.AXE_STRIP) {
			return Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(AXIS, state.getValue(AXIS));
		}
		return super.getToolModifiedState(state, context, ability, simulate);
	}

}

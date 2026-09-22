package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.ItemAbilities;
import org.jetbrains.annotations.Nullable;

public class BlueFirWoodBlock extends RotatedPillarBlock {

	private static final MapCodec<BlueFirWoodBlock> CODEC = simpleCodec(BlueFirWoodBlock::new);

	public BlueFirWoodBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends RotatedPillarBlock> codec() {
		return CODEC;
	}

	@Override
	public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
		if (ability == ItemAbilities.AXE_STRIP) {
			return Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState().setValue(AXIS, state.getValue(AXIS));
		}
		return super.getToolModifiedState(state, context, ability, simulate);
	}

}

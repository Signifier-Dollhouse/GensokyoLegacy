package dev.xkmc.gensokyolegacy.content.block.nature;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A log or wood block that an axe strips to a vanilla block. The target is per instance, so one class serves
 * every wood; a wood with no vanilla counterpart of its own strips to the closest one the mod already uses.
 */
public class StrippableWoodBlock extends RotatedPillarBlock {


	private final Supplier<Block> stripped;

	public StrippableWoodBlock(Supplier<Block> stripped, Properties properties) {
		super(properties);
		this.stripped = stripped;
	}


	@Override
	public @Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
		if (toolAction == ToolActions.AXE_STRIP) {
			return stripped.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
		}
		return super.getToolModifiedState(state, context, toolAction, simulate);
	}

}

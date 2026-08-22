package dev.xkmc.gensokyolegacy.content.block.door;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.jetbrains.annotations.Nullable;

import static dev.xkmc.gensokyolegacy.content.block.door.SlidingDoor.HINGE;
import static dev.xkmc.gensokyolegacy.content.block.door.SlidingDoor.STACK;
import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HALF;

public class SlidingDoorUtils {

	public static boolean isSeatedAndOpenable(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.hasProperty(HALF)) return false;
		if (state.getValue(HALF) == Half.TOP) {
			pos = pos.below();
			state = level.getBlockState(pos);
		}
		if (!state.hasProperty(STACK) || !state.hasProperty(HINGE)) return false;
		return SlidingDoor.canOpen(level, pos, state);
	}

	public static @Nullable BlockPos tryOpen(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.hasProperty(HALF)) return null;
		if (state.getValue(HALF) == Half.TOP) {
			pos = pos.below();
			state = level.getBlockState(pos);
		}
		if (!state.hasProperty(STACK) || !state.hasProperty(HINGE)) return null;
		if (!SlidingDoor.canOpen(level, pos, state)) return null;
		SlidingDoor.doOpen(level, pos, state);
		return pos;
	}

	public static boolean tryClose(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.hasProperty(HALF)) return false;
		if (state.getValue(HALF) == Half.TOP) {
			pos = pos.below();
			state = level.getBlockState(pos);
		}
		if (!state.hasProperty(STACK) || !state.hasProperty(HINGE)) return false;
		if (!SlidingDoor.canClose(level, pos, state)) return false;
		SlidingDoor.doClose(level, pos, state);
		return true;
	}

	public static BlockPos seatOf(BlockState panelState, BlockPos panelPos) {
		return panelPos.relative(SlidingDoor.hingeDir(panelState).getOpposite());
	}

}

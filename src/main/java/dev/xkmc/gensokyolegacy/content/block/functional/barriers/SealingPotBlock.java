package dev.xkmc.gensokyolegacy.content.block.functional.barriers;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectEntry;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.content.attachment.area.ClientAreaEffectTracker;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.l2modularblock.mult.OnPlaceBlockMethod;
import dev.xkmc.l2modularblock.mult.OnReplacedBlockMethod;
import dev.xkmc.l2modularblock.mult.PlacementBlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SealingPotBlock implements OnPlaceBlockMethod, OnReplacedBlockMethod, PlacementBlockMethod {

	@Override
	public @Nullable BlockState getStateForPlacement(BlockState def, BlockPlaceContext context) {
		BlockPos target = context.replacingClickedOnBlock()
				? context.getClickedPos()
				: context.getClickedPos().relative(context.getClickedFace());
		// This must be checked on the client too: the client predicts block placement and shrinks the
		// held stack before the server rejects it, and that prediction is never rolled back for items.
		return isSealed(context.getLevel(), target) ? null : def;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		if (level.isClientSide || state.is(old.getBlock())) return;
		ServerLevel sl = (ServerLevel) level;
		if (!sl.isLoaded(pos)) return;
		if (isChunkSealed(sl, new ChunkPos(pos))) return;
		ChunkPosRange range = ChunkPosRange.ofOwner(pos, GLModConfig.SERVER.sealingPotRadius.get());
		AreaEffectManager.add(sl, pos, range, new SealingEffectData());
	}

	@Override
	public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (level.isClientSide || state.is(newState.getBlock())) return;
		if (level instanceof ServerLevel sl) AreaEffectManager.removeOwner(sl, pos);
	}

	/**
	 * Limit overlap: hide the effect if the chunk is already sealed by another pot.
	 */
	static boolean isChunkSealed(ServerLevel level, ChunkPos pos) {
		return hasSealingEffect(AreaEffectManager.getAffecting(level, pos));
	}

	/**
	 * Side-agnostic sealed check, so that the client's placement prediction matches the server.
	 */
	static boolean isSealed(Level level, BlockPos pos) {
		return level instanceof ServerLevel sl
				? hasSealingEffect(AreaEffectManager.getAffecting(sl, pos))
				: hasSealingEffect(ClientAreaEffectTracker.getAffecting(pos));
	}

	private static boolean hasSealingEffect(List<AreaEffectEntry> affecting) {
		for (AreaEffectEntry e : affecting) {
			if (e.data instanceof SealingEffectData) return true;
		}
		return false;
	}

}
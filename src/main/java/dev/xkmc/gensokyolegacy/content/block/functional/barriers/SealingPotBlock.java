package dev.xkmc.gensokyolegacy.content.block.functional.barriers;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectEntry;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
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

public class SealingPotBlock implements OnPlaceBlockMethod, OnReplacedBlockMethod, PlacementBlockMethod {

	@Override
	public @Nullable BlockState getStateForPlacement(BlockState def, BlockPlaceContext context) {
		if (!(context.getLevel() instanceof ServerLevel sl)) return def;
		BlockPos target = context.replacingClickedOnBlock()
				? context.getClickedPos()
				: context.getClickedPos().relative(context.getClickedFace());
		return isChunkSealed(sl, new ChunkPos(target)) ? null : def;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		if (level.isClientSide || state.is(old.getBlock())) return;
		ServerLevel sl = (ServerLevel) level;
		if (!sl.isLoaded(pos)) return;
		removeFor(sl, pos);
		if (isChunkSealed(sl, new ChunkPos(pos))) return;
		ChunkPosRange range = ChunkPosRange.ofOwner(pos, GLModConfig.SERVER.sealingPotRadius.get());
		AreaEffectManager.add(sl, pos, range, new SealingEffectData());
	}

	@Override
	public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (level.isClientSide || state.is(newState.getBlock())) return;
		if (level instanceof ServerLevel sl) removeFor(sl, pos);
	}

	/**
	 * Limit overlap: hide the effect if the chunk is already sealed by another pot.
	 */
	static boolean isChunkSealed(ServerLevel level, ChunkPos pos) {
		for (AreaEffectEntry e : AreaEffectManager.getAffecting(level, pos)) {
			if (e.data instanceof SealingEffectData) return true;
		}
		return false;
	}

	private static void removeFor(ServerLevel sl, BlockPos pos) {
		var att = GLMeta.LEVEL_EFFECT.type().getOrCreate(sl);
		att.getById().values().removeIf(e ->
				e.ownerPos.equals(pos) && e.data instanceof SealingEffectData && AreaEffectManager.remove(sl, e.id));
	}
}
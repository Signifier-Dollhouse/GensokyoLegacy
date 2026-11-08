package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

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

/**
 * Behavior of the treasure basket: owns one area effect that catches expiring items, and refuses
 * placement in a chunk another basket already covers.
 *
 * @see "doc/design/treasure_basket.md"
 */
public class TreasureBasketBlock implements OnPlaceBlockMethod, OnReplacedBlockMethod, PlacementBlockMethod {

	@Override
	public @Nullable BlockState getStateForPlacement(BlockState def, BlockPlaceContext context) {
		BlockPos target = context.replacingClickedOnBlock()
				? context.getClickedPos()
				: context.getClickedPos().relative(context.getClickedFace());
		// also on the client, since placement prediction shrinks the held stack irreversibly
		return isProtected(context.getLevel(), target) ? null : def;
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
		if (level.isClientSide || state.is(old.getBlock())) return;
		ServerLevel sl = (ServerLevel) level;
		if (!sl.isLoaded(pos)) return;
		if (hasBasketEffect(AreaEffectManager.getAffecting(sl, new ChunkPos(pos)))) return;
		ChunkPosRange range = ChunkPosRange.ofOwner(pos, GLModConfig.SERVER.treasureBasketRadius.get());
		AreaEffectManager.add(sl, pos, range, new TreasureBasketEffectData());
	}

	@Override
	public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (level.isClientSide || state.is(newState.getBlock())) return;
		if (!(level instanceof ServerLevel sl)) return;
		AreaEffectEntry entry = AreaEffectManager.removeOwner(sl, pos);
		if (entry != null && entry.data instanceof TreasureBasketEffectData data)
			data.dropContents(sl, pos);
	}

	/**
	 * Side-agnostic protected check, so that the client's placement prediction matches the server.
	 */
	static boolean isProtected(Level level, BlockPos pos) {
		List<AreaEffectEntry> affecting = level instanceof ServerLevel sl
				? AreaEffectManager.getAffecting(sl, pos)
				: ClientAreaEffectTracker.getAffecting(pos);
		return hasBasketEffect(affecting);
	}

	private static boolean hasBasketEffect(List<AreaEffectEntry> affecting) {
		for (AreaEffectEntry e : affecting) {
			if (e.data instanceof TreasureBasketEffectData) return true;
		}
		return false;
	}

}

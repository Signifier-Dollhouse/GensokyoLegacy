package dev.xkmc.gensokyolegacy.content.block.functional.treasure;

import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectEntry;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectManager;
import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.content.attachment.area.ClientAreaEffectTracker;
import dev.xkmc.gensokyolegacy.init.data.GLModConfig;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.mult.OnPlaceBlockMethod;
import dev.xkmc.l2modularblock.mult.OnReplacedBlockMethod;
import dev.xkmc.l2modularblock.mult.PlacementBlockMethod;
import dev.xkmc.l2modularblock.mult.UseWithoutItemBlockMethod;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Behavior of the treasure basket: owns one area effect that catches expiring items, and refuses
 * placement in a chunk another basket already covers.
 *
 * @see "doc/design/treasure_basket.md"
 */
public class TreasureBasketBlock implements OnPlaceBlockMethod, OnReplacedBlockMethod, PlacementBlockMethod,
		UseWithoutItemBlockMethod {

	public static final BlockMethod BE = new BlockEntityBlockMethodImpl<>(
			GLBlocks.TREASURE_BASKET_BE, TreasureBasketBlockEntity.class);

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

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
											BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof TreasureBasketBlockEntity be))
			return InteractionResult.PASS;
		// the block position is needed on the client, so it rides the open buffer
		sp.openMenu(be, buf -> buf.writeBlockPos(pos));
		return InteractionResult.SUCCESS;
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

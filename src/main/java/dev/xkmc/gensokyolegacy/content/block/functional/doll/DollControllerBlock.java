package dev.xkmc.gensokyolegacy.content.block.functional.doll;

import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.mult.OnReplacedBlockMethod;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import dev.xkmc.l2modularblock.mult.UseWithoutItemBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The doll controller block. Hosts a resident {@link dev.xkmc.gensokyolegacy.content.attachment.doll.DollData}
 * via {@link DollControllerBlockEntity} and is the interaction surface for it (doc/design/doll/controller.md).
 * <ul>
 *   <li>Use with a doll item in an empty controller: install the doll into the block (consumes the item).</li>
 *   <li>Use with empty hand: deploy the stored doll, or recall the deployed doll back into the block.</li>
 *   <li>Sneak-use with empty hand: reclaim the resident doll as a doll item.</li>
 *   <li>Breaking the block never loses the doll: it is dropped as a doll item.</li>
 * </ul>
 */
public class DollControllerBlock implements ShapeBlockMethod, UseWithoutItemBlockMethod, UseItemOnBlockMethod, OnReplacedBlockMethod {

	//public static final BlockMethod BE = new BlockEntityBlockMethodImpl<>(GLBlocks.DOLL_CONTROLLER_BE, DollControllerBlockEntity.class);

	public static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
		if (!(level.getBlockEntity(pos) instanceof DollControllerBlockEntity be)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!stack.is(GLItems.DOLL.get())) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (level.isClientSide()) {
			return ItemInteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer sp && be.install(stack)) {
			if (!sp.getAbilities().instabuild) stack.shrink(1);
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.FAIL;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult result) {
		if (!(level.getBlockEntity(pos) instanceof DollControllerBlockEntity be)) {
			return InteractionResult.PASS;
		}
		if (player.isSecondaryUseActive()) {
			// sneaking: reclaim the resident doll as an item
			if (player instanceof ServerPlayer sp && be.eject(sp)) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return be.hasDoll() ? InteractionResult.CONSUME : InteractionResult.PASS;
		}
		if (be.deploy()) return InteractionResult.SUCCESS;
		if (be.recall()) return InteractionResult.SUCCESS;
		return InteractionResult.PASS;
	}

	@Override
	public void onReplaced(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (isMoving) return;
		if (newState.getBlock() == state.getBlock()) return;
		if (level.getBlockEntity(pos) instanceof DollControllerBlockEntity be) {
			be.onDestroy();
		}
	}

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

}
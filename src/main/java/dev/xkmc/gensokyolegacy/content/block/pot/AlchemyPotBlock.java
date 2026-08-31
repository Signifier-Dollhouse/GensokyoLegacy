package dev.xkmc.gensokyolegacy.content.block.pot;

import dev.xkmc.gensokyolegacy.init.registrate.GLBlocks;
import dev.xkmc.l2modularblock.core.DelegateBlock;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import dev.xkmc.l2modularblock.mult.UseWithoutItemBlockMethod;
import dev.xkmc.l2modularblock.one.ShapeBlockMethod;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class AlchemyPotBlock implements ShapeBlockMethod, UseItemOnBlockMethod, UseWithoutItemBlockMethod {

	public static final BlockMethod BE = new BlockEntityBlockMethodImpl<>(GLBlocks.ALCHEMY_POT_BE, AlchemyPotBlockEntity.class);

	private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 1, 1); // full cube for now; model defines visual

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof AlchemyPotBlockEntity be))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		// forbid interaction during reaction except shift-clear (handled in useWithoutItem)
		// but bucket handling should be forbidden during reaction too
		if (be.isReacting()) {
			// fluid interaction forbidden, item add forbidden
			// we still allow fluid containers? spec says forbid during reaction, so reject
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		// try fluid handler first
		if (!stack.isEmpty() && FluidUtil.getFluidHandler(stack).isPresent()) {
			// check if stack is fluid container
			var fluidContained = FluidUtil.getFluidContained(stack);
			if (fluidContained.isPresent() && !fluidContained.get().isEmpty()) {
				// has fluid -> try fill
				int filled = be.tank.fill(fluidContained.get(), IFluidHandler.FluidAction.SIMULATE);
				if (filled > 0) {
					if (!level.isClientSide) {
						var result = FluidUtil.tryEmptyContainer(player.getItemInHand(hand), be.tank, filled, player, true);
						if (result.isSuccess()) {
							be.notifyTile();
							level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
							return ItemInteractionResult.SUCCESS;
						}
						// fallback manual
						be.tank.fill(fluidContained.get(), IFluidHandler.FluidAction.EXECUTE);
						be.notifyTile();
					}
					return ItemInteractionResult.SUCCESS;
				}
			} else {
				// empty container -> try drain
				// Actually for bucket, empty bucket item is Items.BUCKET
				// Use FluidUtil.tryFillContainer
				if (!be.getFluid().isEmpty()) {
					var result = FluidUtil.tryFillContainer(stack, be.tank, be.getFluid().getAmount(), player, true);
					if (result.isSuccess()) {
						if (!level.isClientSide) {
							be.notifyTile();
							level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
						}
						return ItemInteractionResult.SUCCESS;
					}
				}
			}
		}
		// otherwise try add item
		if (!stack.isEmpty()) {
			if (be.tryAddItem(stack, true)) {
				if (!level.isClientSide) {
					be.addItemWithContainer(player, stack);
					level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 1.2f);
				}
				return ItemInteractionResult.SUCCESS;
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof AlchemyPotBlockEntity be)) return InteractionResult.PASS;
		if (player.isShiftKeyDown()) {
			if (!level.isClientSide) {
				be.clearContents();
				level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 0.5f);
			}
			return InteractionResult.SUCCESS;
		}
		if (be.isReacting()) return InteractionResult.PASS;
		if (!level.isClientSide) {
			be.popLastItem(player);
		}
		return InteractionResult.SUCCESS;
	}

	public static void buildStates(com.tterrag.registrate.providers.DataGenContext<Block, DelegateBlock> ctx, com.tterrag.registrate.providers.RegistrateBlockstateProvider pvd) {
		// already handled in GLBlocks
	}
}

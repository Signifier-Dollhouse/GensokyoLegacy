package dev.xkmc.gensokyolegacy.content.block.functional.plate;

import dev.xkmc.gensokyolegacy.content.block.base.ShapePathFindBlockMethod;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.l2modularblock.impl.BlockEntityBlockMethodImpl;
import dev.xkmc.l2modularblock.mult.UseItemOnBlockMethod;
import dev.xkmc.l2modularblock.mult.UseWithoutItemBlockMethod;
import dev.xkmc.l2modularblock.type.BlockMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * One food per plate, no container of its own: right-click with food to serve it, right-click again to
 * take it back. Collection is deliberately main-hand only and needs an empty hand, so a click meant
 * for something else is never swallowed — vanilla tries the block, then the held item, and only the
 * main hand reaches {@link #useWithoutItem} at all.
 */
public class PlateBlock implements ShapePathFindBlockMethod, UseItemOnBlockMethod, UseWithoutItemBlockMethod {

	public static final BlockMethod BE = new BlockEntityBlockMethodImpl<>(GLBlocks.PLATE_BE, PlateBlockEntity.class);

	/** The model's disc only. Its second element is a utensil sliver lying over the rim, too thin to
	 *  be worth an outline of its own. */
	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 1, 13);

	@Override
	public @Nullable VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!isFood(stack) || !(level.getBlockEntity(pos) instanceof PlateBlockEntity be) || !be.stack.isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide) {
			be.stack = stack.copyWithCount(1);
			stack.shrink(1);
			be.notifyTile();
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
		}
		return ItemInteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof PlateBlockEntity be) || be.stack.isEmpty())
			return InteractionResult.PASS;
		if (!level.isClientSide) {
			be.take(player);
			level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 0.8F);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Edible and nothing handed back: mushroom stew is food, but its bowl has to go somewhere and a
	 * plate has nowhere to put it.
	 */
	private static boolean isFood(ItemStack stack) {
		var food = stack.get(DataComponents.FOOD);
		return food != null && food.usingConvertsTo().isEmpty();
	}

}

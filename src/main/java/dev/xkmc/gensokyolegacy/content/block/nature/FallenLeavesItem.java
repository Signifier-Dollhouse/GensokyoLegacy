package dev.xkmc.gensokyolegacy.content.block.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Places the carpet on the ground as usual, and on the surface of a still water source the way a lily pad does.
 *
 * @see "doc/design/fallen_leaves.md"
 */
public class FallenLeavesItem extends BlockItem {

	public FallenLeavesItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		if (player != null) {
			BlockHitResult water = waterHit(context.getLevel(), player);
			if (water != null) return super.useOn(new UseOnContext(player, context.getHand(), water));
		}
		return super.useOn(context);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		BlockHitResult water = waterHit(level, player);
		if (water != null) return new InteractionResultHolder<>(super.useOn(new UseOnContext(player, hand, water)), stack);
		return InteractionResultHolder.pass(stack);
	}

	/**
	 * The block above a still water source the player aims at, or null when the aim is not at one.
	 */
	private static @Nullable BlockHitResult waterHit(Level level, Player player) {
		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
		if (hit.getType() != HitResult.Type.BLOCK) return null;
		BlockPos spot = hit.getBlockPos().above();
		var fluid = level.getFluidState(hit.getBlockPos());
		if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) return null;
		if (!level.getFluidState(spot).isEmpty() || !level.getBlockState(spot).canBeReplaced()) return null;
		return hit.withPosition(spot);
	}

}

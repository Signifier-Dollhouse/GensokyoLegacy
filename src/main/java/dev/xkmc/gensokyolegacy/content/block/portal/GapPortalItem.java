package dev.xkmc.gensokyolegacy.content.block.portal;

import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

public class GapPortalItem extends BlockItem {

	public GapPortalItem(Block block, Properties properties) {
		super(block, properties.stacksTo(1));
	}

	@Override
	public InteractionResult place(BlockPlaceContext ctx) {
		var level = ctx.getLevel();
		var stack = ctx.getItemInHand();
		boolean hadUuid = stack.has(GLItems.DC_UUID);
		PortalSide hadSide = stack.has(GLItems.DC_PORTAL_SIDE) ? stack.get(GLItems.DC_PORTAL_SIDE) : null;
		boolean inGap = level.dimension().location().equals(GLDimensionGen.GAP.location());
		var player = ctx.getPlayer();
		InteractionHand hand = ctx.getHand();
		var result = super.place(ctx);
		if (!result.consumesAction() || player == null) return result;

		// Unified hand handling:
		// - Without id in GAP: replace hand with id+side copy (ENTRY placed -> EXIT copy), regardless of creative
		// - With id (anywhere): consume hand to empty, regardless of creative
		// This centralizes the previous scattered give/modify logic.
		if (!hadUuid && inGap) {
			// Fresh inside GAP: hand should be replaced with the paired EXIT item
			UUID id = null;
			PortalSide placedSide = PortalSide.ENTRY;
			GapPortalBlockEntity beFound = null;
			var pos = ctx.getClickedPos();
			if (level.getBlockEntity(pos) instanceof GapPortalBlockEntity be && be.id != null) {
				beFound = be;
				id = be.id;
			} else if (level.getBlockEntity(pos.above()) instanceof GapPortalBlockEntity be2 && be2.id != null) {
				beFound = be2;
				id = be2.id;
			} else if (level.getBlockEntity(pos.below()) instanceof GapPortalBlockEntity be3 && be3.id != null) {
				beFound = be3;
				id = be3.id;
			}
			if (beFound != null) placedSide = beFound.getSide();
			else if (hadSide != null) placedSide = hadSide;
			if (id == null) {
				// Fallback: try to read from mutated stack (creative case where stack not shrunk)
				if (stack.has(GLItems.DC_UUID)) id = stack.get(GLItems.DC_UUID);
			}
			if (id != null) {
				PortalSide copySide = placedSide == PortalSide.ENTRY ? PortalSide.EXIT : PortalSide.ENTRY;
				ItemStack copy = GLBlocks.GAP_PORTAL.asStack();
				copy.set(GLItems.DC_UUID, id);
				copy.set(GLItems.DC_PORTAL_SIDE, copySide);
				// Replace hand directly, regardless of creative
				player.setItemInHand(hand, copy);
			} else {
				// Fallback: ensure hand is not empty due to creative non-consume; force replace with what we can
				ItemStack copy = GLBlocks.GAP_PORTAL.asStack();
				// Try to find id from stack if mutated
				if (stack.has(GLItems.DC_UUID)) {
					copy.set(GLItems.DC_UUID, stack.get(GLItems.DC_UUID));
					copy.set(GLItems.DC_PORTAL_SIDE, PortalSide.EXIT);
					player.setItemInHand(hand, copy);
				}
			}
		} else if (hadUuid) {
			// With id (any dimension): consume hand even in creative
			// super.place already consumed in survival (hand empty); in creative hand still has item, so force empty
			ItemStack handStack = player.getItemInHand(hand);
			if (!handStack.isEmpty()) {
				// If stack is the same item we just placed, clear it
				// For stacksTo1, this will be size 1 with same id/side
				player.setItemInHand(hand, ItemStack.EMPTY);
			}
			// If hand already empty (survival), keep empty
		} else {
			// Without id outside GAP: no special replace, keep vanilla behavior
			// But for unified "consume regardless creative" for with id, we already handled.
			// For without id outside, vanilla is: survival -> consumed (already empty), creative -> not consumed (still has item with new id)
			// Spec says fresh outside should still create id but not give copy; in creative, should we keep the mutated item?
			// The spec says "when player place a portal without id, replace item on hand with portal item with id and side, regardless if player is creative or not"
			// This would imply without id outside also should be replaced, but second message narrowed to "in gap".
			// We follow second message: without id outside in creative keeps the mutated item (vanilla), no forced consume.
			// No action needed.
		}
		return result;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return super.isFoil(stack) || stack.has(GLItems.DC_UUID);
	}
}

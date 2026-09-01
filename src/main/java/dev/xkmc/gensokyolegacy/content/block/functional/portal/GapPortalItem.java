package dev.xkmc.gensokyolegacy.content.block.functional.portal;

import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.List;
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

		// Unified hand handling (now consistent for any fresh placement):
		// - Without id (anywhere, in gap or not): replace hand with portal item with same uuid and opposite side, regardless of creative.
		//   The placed block is ENTRY, the hand copy is EXIT (or vice versa for fresh EXIT, rare).
		// - With id (anywhere): consume hand to empty, regardless of creative.
		if (!hadUuid) {
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
			if (id == null && stack.has(GLItems.DC_UUID)) id = stack.get(GLItems.DC_UUID);
			if (id != null) {
				PortalSide copySide = placedSide == PortalSide.ENTRY ? PortalSide.EXIT : PortalSide.ENTRY;
				ItemStack copy = GLBlocks.GAP_PORTAL.asStack();
				copy.set(GLItems.DC_UUID, id);
				copy.set(GLItems.DC_PORTAL_SIDE, copySide);
				player.setItemInHand(hand, copy);
			} else {
				ItemStack copy = GLBlocks.GAP_PORTAL.asStack();
				if (stack.has(GLItems.DC_UUID)) {
					copy.set(GLItems.DC_UUID, stack.get(GLItems.DC_UUID));
					copy.set(GLItems.DC_PORTAL_SIDE, PortalSide.EXIT);
					player.setItemInHand(hand, copy);
				}
			}
		} else {
			// With id (any dimension): consume hand even in creative
			ItemStack handStack = player.getItemInHand(hand);
			if (!handStack.isEmpty()) {
				player.setItemInHand(hand, ItemStack.EMPTY);
			}
		}
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
		if (stack.has(GLItems.DC_UUID)) {
			UUID id = stack.get(GLItems.DC_UUID);
			if (id != null) {
				String hex = id.toString().substring(0, 8);
				list.add(Component.literal(hex).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return super.isFoil(stack) || stack.has(GLItems.DC_UUID);
	}
}

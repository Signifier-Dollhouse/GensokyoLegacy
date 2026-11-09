package dev.xkmc.gensokyolegacy.content.block.functional.portal;

import net.minecraft.world.level.Level;
import dev.xkmc.gensokyolegacy.content.dimension.GLDimensionGen;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLBlocks;
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
		boolean hadUuid = GLItems.DC_UUID.has(stack);
		PortalSide hadSide = GLItems.DC_PORTAL_SIDE.has(stack) ? GLItems.DC_PORTAL_SIDE.get(stack) : null;
		boolean inGap = level.dimension().location().equals(GLDimensionGen.GAP.location());
		var player = ctx.getPlayer();
		InteractionHand hand = ctx.getHand();
		var result = super.place(ctx);
		if (!result.consumesAction() || player == null) return result;

		// Unified hand handling: a fresh item leaves the opposite-side copy in the hand (placed ENTRY,
		// hand EXIT, either dimension); an item that already has an id is consumed, even in creative.
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
			if (id == null && GLItems.DC_UUID.has(stack)) id = GLItems.DC_UUID.get(stack);
			if (id != null) {
				PortalSide copySide = placedSide == PortalSide.ENTRY ? PortalSide.EXIT : PortalSide.ENTRY;
				ItemStack copy = GLBlocks.GAP_PORTAL.asStack();
				GLItems.DC_UUID.set(copy, id);
				GLItems.DC_PORTAL_SIDE.set(copy, copySide);
				player.setItemInHand(hand, copy);
			} else {
				ItemStack copy = GLBlocks.GAP_PORTAL.asStack();
				if (GLItems.DC_UUID.has(stack)) {
					GLItems.DC_UUID.set(copy, GLItems.DC_UUID.get(stack));
					GLItems.DC_PORTAL_SIDE.set(copy, PortalSide.EXIT);
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
	public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
		if (GLItems.DC_UUID.has(stack)) {
			UUID id = GLItems.DC_UUID.get(stack);
			if (id != null) {
				String hex = id.toString().substring(0, 8);
				list.add(Component.literal(hex).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return super.isFoil(stack) || GLItems.DC_UUID.has(stack);
	}
}

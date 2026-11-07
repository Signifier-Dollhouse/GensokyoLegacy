package dev.xkmc.gensokyolegacy.content.block.deco.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Draws the served food on the gravestone's base slab. It is moved two pixels towards the open side
 * so the item does not cut into the standing stone.
 */
public class WastelandGravestoneRenderer implements BlockEntityRenderer<WastelandGravestoneBlockEntity> {

	public WastelandGravestoneRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	@Override
	public void render(WastelandGravestoneBlockEntity be, float pTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (be.stack.isEmpty() || be.getBlockState().getValue(WastelandGravestoneBlock.HALF) != Half.BOTTOM) return;
		var level = be.getLevel();
		if (level == null) return;
		Direction facing = be.getBlockState().getValue(WastelandGravestoneBlock.FACING);
		ms.pushPose();
		ms.translate(0.5 + facing.getStepX() * 2 / 16F, 5 / 16F, 0.5 + facing.getStepZ() * 2 / 16F);
		ms.mulPose(Axis.YP.rotationDegrees(facing.toYRot() + 180));
		ms.mulPose(Axis.XP.rotationDegrees(90));
		ms.scale(0.5F, 0.5F, 0.5F);
		Minecraft.getInstance().getItemRenderer().renderStatic(be.stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, level, 0);
		ms.popPose();
	}

}

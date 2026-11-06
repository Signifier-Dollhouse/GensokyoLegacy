package dev.xkmc.gensokyolegacy.content.block.functional.plate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Draws the served food lying flat in the middle of the plate.
 * <p>
 * The item renderer centres whatever it is handed, so the food rests on the disc's own one pixel at
 * half block size, and it turns with the plate: the model is authored for {@code facing=north}, whose
 * blockstate variant carries no rotation, hence the extra half turn.
 */
public class PlateRenderer implements BlockEntityRenderer<PlateBlockEntity> {

	public PlateRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	@Override
	public void render(PlateBlockEntity be, float pTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (be.stack.isEmpty()) return;
		var level = be.getLevel();
		if (level == null) return;
		var facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
		ms.pushPose();
		ms.translate(0.5, 1 / 16F, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(facing.toYRot() + 180));
		ms.mulPose(Axis.XP.rotationDegrees(90));
		ms.scale(0.5F, 0.5F, 0.5F);
		Minecraft.getInstance().getItemRenderer().renderStatic(be.stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, level, 0);
		ms.popPose();
	}

}

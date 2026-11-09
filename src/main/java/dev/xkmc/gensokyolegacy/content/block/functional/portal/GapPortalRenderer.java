// Source recreated from a .class file by IntelliJ IDEA (Fernflower decompiler).

package dev.xkmc.gensokyolegacy.content.block.functional.portal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class GapPortalRenderer implements BlockEntityRenderer<GapPortalBlockEntity> {
	public static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("minecraft", "textures/environment/end_sky.png");
	public static final ResourceLocation END_PORTAL_LOCATION = new ResourceLocation("minecraft", "textures/entity/end_portal.png");
	public static final ResourceLocation FRAME = GensokyoLegacy.loc("textures/block/utensil/gap_portal.png");
	public static final ResourceLocation FRAME_BACK = GensokyoLegacy.loc("textures/block/utensil/gap_portal_back.png");

	public GapPortalRenderer(BlockEntityRendererProvider.Context ctx) {
	}

	public void render(GapPortalBlockEntity e, float pTick, PoseStack pose, MultiBufferSource source, int light, int overlay) {
		if (e.getBlockState().getValue(BlockStateProperties.HALF) == Half.TOP) return;
		var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
		pose.pushPose();
		pose.translate(0.5f, 0, 0.5f);
		pose.mulPose(Axis.YP.rotationDegrees(180 - cam.getYRot()));
		Matrix4f mat = pose.last().pose();
		Matrix3f normal = pose.last().normal();
		if (e.pending) {
			var vc = source.getBuffer(RenderType.entityCutout(FRAME_BACK));
			vc.vertex(mat, -0.5f, 0, 0.01f).color(-1).uv(0, 0).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, 1).endVertex();
			vc.vertex(mat, 0.5f, 0, 0.01f).color(-1).uv(1, 0).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, 1).endVertex();
			vc.vertex(mat, 0.5f, 2, 0.01f).color(-1).uv(1, 1).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, 1).endVertex();
			vc.vertex(mat, -0.5f, 2, 0.01f).color(-1).uv(0, 1).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, 1).endVertex();
			vc.vertex(mat, -0.5f, 2, -0.01f).color(-1).uv(0, 1).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, -1).endVertex();
			vc.vertex(mat, 0.5f, 2, -0.01f).color(-1).uv(1, 1).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, -1).endVertex();
			vc.vertex(mat, 0.5f, 0, -0.01f).color(-1).uv(1, 0).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, -1).endVertex();
			vc.vertex(mat, -0.5f, 0, -0.01f).color(-1).uv(0, 0).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, -1).endVertex();
		} else {
			{
				var vc = source.getBuffer(RenderType.entityCutout(FRAME));
				vc.vertex(mat, -0.5f, 0, 0.01f).color(-1).uv(0, 0).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, 1).endVertex();
				vc.vertex(mat, 0.5f, 0, 0.01f).color(-1).uv(1, 0).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, 1).endVertex();
				vc.vertex(mat, 0.5f, 2, 0.01f).color(-1).uv(1, 1).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, 1).endVertex();
				vc.vertex(mat, -0.5f, 2, 0.01f).color(-1).uv(0, 1).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, 1).endVertex();
			}
			{
				var vc = source.getBuffer(RenderType.entityCutout(FRAME_BACK));
				vc.vertex(mat, -0.5f, 2, -0.01f).color(-1).uv(0, 1).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, -1).endVertex();
				vc.vertex(mat, 0.5f, 2, -0.01f).color(-1).uv(1, 1).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, -1).endVertex();
				vc.vertex(mat, 0.5f, 0, -0.01f).color(-1).uv(1, 0).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, -1).endVertex();
				vc.vertex(mat, -0.5f, 0, -0.01f).color(-1).uv(0, 0).uv2(light).overlayCoords(overlay).normal(normal, 0, 0, -1).endVertex();
			}
			{
				VertexConsumer vc = source.getBuffer(RenderType.endPortal());
				vc.vertex(mat, -6 / 16f, 9 / 16f, 0).endVertex();
				vc.vertex(mat, 0, 0, 0).endVertex();
				vc.vertex(mat, 0, 2, 0).endVertex();
				vc.vertex(mat, -6 / 16f, 23 / 16f, 0).endVertex();

				vc.vertex(mat, 6 / 16f, 23 / 16f, 0).endVertex();
				vc.vertex(mat, 0, 2, 0).endVertex();
				vc.vertex(mat, 0, 0, 0).endVertex();
				vc.vertex(mat, 6 / 16f, 9 / 16f, 0).endVertex();
			}
		}
		pose.popPose();
	}

}

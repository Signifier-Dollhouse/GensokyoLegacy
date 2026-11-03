package dev.xkmc.gensokyolegacy.content.entity.misc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorHookEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Draws the bounded anchor with its flight rotation and a slow roll, so it points the way it flies
 * and reads as a thrown object rather than a billboarded icon.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorHookRenderer extends EntityRenderer<AnchorHookEntity> {

	private static final float ROLL_SPEED = 11;

	private final ItemRenderer itemRenderer;

	public AnchorHookRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(AnchorHookEntity entity, float yaw, float partialTick, PoseStack pose,
					   MultiBufferSource buffer, int packedLight) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
		pose.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * ROLL_SPEED));
		layFlat(pose);
		itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight,
				OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
		pose.popPose();
		super.render(entity, yaw, partialTick, pose, buffer, packedLight);
	}

	/**
	 * The model is authored upright with its shaft along +y, while the flight frame's +z is the
	 * throw direction. Tip it a quarter turn and cancel the {@code translate(-0.5)} the item
	 * renderer applies anyway, so the transform matches the danmaku renderer's exactly.
	 */
	private static void layFlat(PoseStack pose) {
		pose.translate(-0.5F, -0.5F, -0.5F);
		pose.mulPose(Axis.XP.rotationDegrees(90));
		pose.translate(0.5F, 0.5F, 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(AnchorHookEntity entity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}

}

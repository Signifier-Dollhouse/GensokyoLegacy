package dev.xkmc.gensokyolegacy.content.entity.misc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.content.entity.misc.TreasureTowerEntity;
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
 * Draws the tower as a slowly spinning item, bobbed a little so it reads as hovering rather than
 * thrown.
 *
 * @see "doc/design/item/tower.md"
 */
public class TreasureTowerRenderer extends EntityRenderer<TreasureTowerEntity> {

	private static final float SCALE = 1.6F;
	private static final float BOB = 0.08F;
	private static final float SPIN = 6F;

	private final ItemRenderer itemRenderer;

	public TreasureTowerRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(TreasureTowerEntity entity, float yaw, float partialTick, PoseStack pose,
					   MultiBufferSource buffer, int packedLight) {
		float time = entity.tickCount + partialTick;
		pose.pushPose();
		pose.translate(0, entity.getBbHeight() * 0.5 + Mth.sin(time * 0.15F) * BOB, 0);
		pose.mulPose(Axis.YP.rotationDegrees(time * SPIN));
		pose.scale(SCALE, SCALE, SCALE);
		itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.FIXED, packedLight,
				OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
		pose.popPose();
		super.render(entity, yaw, partialTick, pose, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(TreasureTowerEntity entity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}

}

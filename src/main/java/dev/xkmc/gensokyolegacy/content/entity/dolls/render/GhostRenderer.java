package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.core.object.Color;

/**
 * No held-item layer: a support ghost carries nothing. Power drives the model's opacity, so the
 * render type is translucent rather than the default cutout.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class GhostRenderer extends GeoEntityRenderer<GhostEntity> {

	public GhostRenderer(EntityRendererProvider.Context context) {
		super(context, new GhostModel());
	}

	/**
	 * The haunting ghost hides for a volley (doc/design/doll/ghost_power.md §3); GeckoLib has no
	 * null-tolerant render type, so hiding must skip the render, not return null from getRenderType.
	 */
	@Override
	public void render(GhostEntity animatable, float entityYaw, float partialTick, PoseStack poseStack,
					   MultiBufferSource bufferSource, int packedLight) {
		if (animatable.isInvisible()) return;
		super.render(animatable, entityYaw, partialTick, poseStack, bufferSource, packedLight);
	}

	@Override
	public Color getRenderColor(GhostEntity animatable, float partialTick, int packedLight) {
		Color base = super.getRenderColor(animatable, partialTick, packedLight);
		return Color.ofRGBA(base.getRed(), base.getGreen(), base.getBlue(),
				(int) (base.getAlpha() * animatable.powerAlpha()));
	}

	@Override
	public RenderType getRenderType(GhostEntity animatable, ResourceLocation texture,
									@Nullable MultiBufferSource bufferSource, float partialTick) {
		if (Minecraft.getInstance().shouldEntityAppearGlowing(animatable))
			return super.getRenderType(animatable, texture, bufferSource, partialTick);
		return RenderType.entityTranslucent(texture);
	}

}

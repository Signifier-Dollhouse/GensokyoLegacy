package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

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

	@Override
	public Color getRenderColor(GhostEntity animatable, float partialTick, int packedLight) {
		Color base = super.getRenderColor(animatable, partialTick, packedLight);
		return Color.ofARGB((int) (base.getAlpha() * animatable.powerAlpha()),
				base.getRed(), base.getGreen(), base.getBlue());
	}

	@Override
	public RenderType getRenderType(GhostEntity animatable, ResourceLocation texture,
									@Nullable MultiBufferSource bufferSource, float partialTick) {
		if (animatable.isInvisible()) return null;
		if (Minecraft.getInstance().shouldEntityAppearGlowing(animatable))
			return super.getRenderType(animatable, texture, bufferSource, partialTick);
		return RenderType.entityTranslucent(texture);
	}

}

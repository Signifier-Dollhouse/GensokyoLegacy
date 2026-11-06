package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * The wild ghost fades with its power too; its starting 500-1000 is visible before it is ever bound.
 *
 * @see "doc/design/doll/ghost_power.md"
 */
public class StrayGhostRenderer extends GeoEntityRenderer<StrayGhostEntity> {

	public StrayGhostRenderer(EntityRendererProvider.Context context) {
		super(context, new StrayGhostModel());
	}

	@Override
	public Color getRenderColor(StrayGhostEntity animatable, float partialTick, int packedLight) {
		Color base = super.getRenderColor(animatable, partialTick, packedLight);
		return Color.ofARGB((int) (base.getAlpha() * animatable.powerAlpha()),
				base.getRed(), base.getGreen(), base.getBlue());
	}

	@Override
	public RenderType getRenderType(StrayGhostEntity animatable, ResourceLocation texture,
									@Nullable MultiBufferSource bufferSource, float partialTick) {
		if (animatable.isInvisible() || Minecraft.getInstance().shouldEntityAppearGlowing(animatable))
			return super.getRenderType(animatable, texture, bufferSource, partialTick);
		return RenderType.entityTranslucent(texture);
	}

}

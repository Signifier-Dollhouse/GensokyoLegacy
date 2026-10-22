package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * No held-item layer: a support ghost carries nothing.
 */
public class GhostRenderer extends GeoEntityRenderer<GhostEntity> {

	public GhostRenderer(EntityRendererProvider.Context context) {
		super(context, new GhostModel());
	}

}

package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class StrayGhostRenderer extends GeoEntityRenderer<StrayGhostEntity> {

	public StrayGhostRenderer(EntityRendererProvider.Context context) {
		super(context, new StrayGhostModel());
	}

}

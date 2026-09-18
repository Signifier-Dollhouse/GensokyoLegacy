package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DollRenderer extends GeoEntityRenderer<DollEntity> {
	public DollRenderer(EntityRendererProvider.Context context) {
		super(context, new DollModel());
		addRenderLayer(new DollHeldItemLayer(this));
	}
}

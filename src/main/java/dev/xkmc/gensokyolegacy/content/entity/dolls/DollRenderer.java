package dev.xkmc.gensokyolegacy.content.entity.dolls;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DollRenderer extends GeoEntityRenderer<DollEntity> {
	public DollRenderer(EntityRendererProvider.Context context) {
		super(context, new DollModel());
	}
}

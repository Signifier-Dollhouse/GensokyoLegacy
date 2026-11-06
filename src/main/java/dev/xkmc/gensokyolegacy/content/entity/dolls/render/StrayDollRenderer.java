package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayDollEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class StrayDollRenderer extends GeoEntityRenderer<StrayDollEntity> {

	public StrayDollRenderer(EntityRendererProvider.Context context) {
		super(context, new StrayDollModel());
	}

}

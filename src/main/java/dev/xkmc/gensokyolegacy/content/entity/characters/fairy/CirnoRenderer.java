package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CirnoRenderer extends GeoEntityRenderer<CirnoEntity> {

	public CirnoRenderer(EntityRendererProvider.Context context) {
		super(context, new CirnoModel());
		addRenderLayer(new CirnoHeldItemLayer(this));
		addRenderLayer(new CirnoSpellCircleLayer(this));
	}

}

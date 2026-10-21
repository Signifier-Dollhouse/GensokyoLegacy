package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.SekibankiHeadEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * No {@code DollHeldItemLayer}: a gadget never holds anything.
 */
public class SekibankiHeadRenderer extends GeoEntityRenderer<SekibankiHeadEntity> {
	public SekibankiHeadRenderer(EntityRendererProvider.Context context) {
		super(context, new SekibankiHeadModel());
	}
}
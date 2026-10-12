package dev.xkmc.gensokyolegacy.content.entity.characters.oni;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiHeldItemLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SuikaRenderer extends GeoEntityRenderer<SuikaEntity> {

	public SuikaRenderer(EntityRendererProvider.Context context) {
		super(context, new SuikaModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
	}
}

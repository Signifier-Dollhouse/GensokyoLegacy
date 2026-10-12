package dev.xkmc.gensokyolegacy.content.entity.characters.maiden;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiHeldItemLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ReimuRenderer extends GeoEntityRenderer<ReimuEntity> {
	public ReimuRenderer(EntityRendererProvider.Context context) {
		super(context, new ReimuModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
	}
}

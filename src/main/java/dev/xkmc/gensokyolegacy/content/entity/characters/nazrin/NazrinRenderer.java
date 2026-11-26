package dev.xkmc.gensokyolegacy.content.entity.characters.nazrin;

import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiSpellCircleLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class NazrinRenderer extends GeoEntityRenderer<NazrinEntity> {

	public NazrinRenderer(EntityRendererProvider.Context context) {
		super(context, new NazrinModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
		addRenderLayer(new YoukaiSpellCircleLayer<>(this));
	}

}

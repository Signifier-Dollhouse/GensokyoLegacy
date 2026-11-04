package dev.xkmc.gensokyolegacy.content.entity.characters.minamitsu;

import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiSpellCircleLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MinamitsuRenderer extends GeoEntityRenderer<MinamitsuEntity> {

	public MinamitsuRenderer(EntityRendererProvider.Context context) {
		super(context, new MinamitsuModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
		addRenderLayer(new YoukaiSpellCircleLayer<>(this));
	}

}

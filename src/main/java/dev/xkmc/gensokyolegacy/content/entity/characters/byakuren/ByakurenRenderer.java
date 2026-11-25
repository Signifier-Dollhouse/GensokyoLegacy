package dev.xkmc.gensokyolegacy.content.entity.characters.byakuren;

import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiSpellCircleLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ByakurenRenderer extends GeoEntityRenderer<ByakurenEntity> {

	public ByakurenRenderer(EntityRendererProvider.Context context) {
		super(context, new ByakurenModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
		addRenderLayer(new YoukaiSpellCircleLayer<>(this));
	}

}

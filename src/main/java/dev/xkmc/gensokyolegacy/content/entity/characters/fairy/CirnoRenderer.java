package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiBubbleLayer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiSpellCircleLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class CirnoRenderer extends GeoEntityRenderer<CirnoEntity> {

	public CirnoRenderer(EntityRendererProvider.Context context) {
		super(context, new CirnoModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
		addRenderLayer(new YoukaiSpellCircleLayer<>(this));
		addRenderLayer(new YoukaiBubbleLayer<>(this));
	}

}
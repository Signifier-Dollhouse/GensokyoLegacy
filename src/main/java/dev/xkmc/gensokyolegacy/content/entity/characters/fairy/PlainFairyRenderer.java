package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiHeldItemLayer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiSpellCircleLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PlainFairyRenderer extends GeoEntityRenderer<PlainFairyEntity> {

	public PlainFairyRenderer(EntityRendererProvider.Context context) {
		super(context, new PlainFairyModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
		addRenderLayer(new YoukaiSpellCircleLayer<>(this));
	}

}
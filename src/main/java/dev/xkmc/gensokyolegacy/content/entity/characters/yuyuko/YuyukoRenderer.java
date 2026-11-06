package dev.xkmc.gensokyolegacy.content.entity.characters.yuyuko;

import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiBubbleLayer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiSpellCircleLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class YuyukoRenderer extends GeoEntityRenderer<YuyukoEntity> {

	public YuyukoRenderer(EntityRendererProvider.Context context) {
		super(context, new YuyukoModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
		addRenderLayer(new YoukaiSpellCircleLayer<>(this));
		addRenderLayer(new YoukaiBubbleLayer<>(this));
	}

}

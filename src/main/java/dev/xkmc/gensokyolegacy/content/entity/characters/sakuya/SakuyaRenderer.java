package dev.xkmc.gensokyolegacy.content.entity.characters.sakuya;

import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SakuyaRenderer extends GeoEntityRenderer<SakuyaEntity> {
	public SakuyaRenderer(EntityRendererProvider.Context context) {
		super(context, new SakuyaModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
	}
}
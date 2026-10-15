package dev.xkmc.gensokyolegacy.content.entity.characters.merchant;

import dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MorichikaRenderer extends GeoEntityRenderer<MorichikaEntity> {
	public MorichikaRenderer(EntityRendererProvider.Context context) {
		super(context, new MorichikaModel());
		addRenderLayer(new YoukaiHeldItemLayer<>(this));
	}
}

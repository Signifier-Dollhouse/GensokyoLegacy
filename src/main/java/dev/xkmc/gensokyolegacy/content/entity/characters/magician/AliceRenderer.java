package dev.xkmc.gensokyolegacy.content.entity.characters.magician;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AliceRenderer extends GeoEntityRenderer<AliceEntity> {

	public AliceRenderer(EntityRendererProvider.Context context) {
		super(context, new AliceModel());
	}

}

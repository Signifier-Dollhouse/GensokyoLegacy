package dev.xkmc.gensokyolegacy.content.entity.characters.maiden;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ReimuRenderer extends GeoEntityRenderer<ReimuEntity> {
    public ReimuRenderer(EntityRendererProvider.Context context) {
        super(context, new ReimuModel());
    }
}

package dev.xkmc.gensokyolegacy.content.entity.dolls;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class DollModel extends GeoModel<DollEntity> {
	private final ResourceLocation model = GensokyoLegacy.loc("geo/doll.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/doll.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/doll.animation.json");

	@Override
	public ResourceLocation getModelResource(DollEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(DollEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(DollEntity animatable) {
		return animations;
	}
}

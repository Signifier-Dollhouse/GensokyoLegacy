package dev.xkmc.gensokyolegacy.content.entity.characters.minamitsu;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class MinamitsuModel extends DefaultedEntityGeoModel<MinamitsuEntity> {

	public MinamitsuModel() {
		super(GensokyoLegacy.loc("minamitsu"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/minamitsu.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/minamitsu.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/minamitsu.animation.json");

	@Override
	public ResourceLocation getModelResource(MinamitsuEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(MinamitsuEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(MinamitsuEntity animatable) {
		return animations;
	}

}

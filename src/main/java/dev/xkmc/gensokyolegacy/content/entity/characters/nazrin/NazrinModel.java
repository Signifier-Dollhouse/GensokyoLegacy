package dev.xkmc.gensokyolegacy.content.entity.characters.nazrin;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import dev.xkmc.gensokyolegacy.util.HeadBoneGeoModel;

public class NazrinModel extends HeadBoneGeoModel<NazrinEntity> {

	public NazrinModel() {
		super(GensokyoLegacy.loc("nazrin"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/nazrin.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/nazrin.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/nazrin.animation.json");

	@Override
	public ResourceLocation getModelResource(NazrinEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(NazrinEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(NazrinEntity animatable) {
		return animations;
	}

}

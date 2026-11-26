package dev.xkmc.gensokyolegacy.content.entity.characters.nue;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.util.HeadBoneGeoModel;
import net.minecraft.resources.ResourceLocation;

public class NueModel extends HeadBoneGeoModel<NueEntity> {

	public NueModel() {
		super(GensokyoLegacy.loc("nue"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/nue.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/nue.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/nue.animation.json");

	@Override
	public ResourceLocation getModelResource(NueEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(NueEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(NueEntity animatable) {
		return animations;
	}

}

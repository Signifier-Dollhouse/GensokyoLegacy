package dev.xkmc.gensokyolegacy.content.entity.characters.maiden;

import dev.xkmc.gensokyolegacy.util.HeadBoneGeoModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;

public class MarisaModel extends HeadBoneGeoModel<MarisaEntity> {

	public MarisaModel() {
		super(GensokyoLegacy.loc("marisa"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/marisa.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/marisa.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/marisa.animation.json");

	@Override
	public ResourceLocation getModelResource(MarisaEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(MarisaEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(MarisaEntity animatable) {
		return animations;
	}
}

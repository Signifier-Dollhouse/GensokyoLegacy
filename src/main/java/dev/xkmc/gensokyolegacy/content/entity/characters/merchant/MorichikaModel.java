package dev.xkmc.gensokyolegacy.content.entity.characters.merchant;

import dev.xkmc.gensokyolegacy.util.HeadBoneGeoModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;

public class MorichikaModel extends HeadBoneGeoModel<MorichikaEntity> {

	public MorichikaModel() {
		super(GensokyoLegacy.loc("morichika"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/morichika.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/morichika.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/morichika.animation.json");

	@Override
	public ResourceLocation getModelResource(MorichikaEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(MorichikaEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(MorichikaEntity animatable) {
		return animations;
	}

}

package dev.xkmc.gensokyolegacy.content.entity.characters.sakuya;

import dev.xkmc.gensokyolegacy.util.HeadBoneGeoModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;

public class SakuyaModel extends HeadBoneGeoModel<SakuyaEntity> {

	public SakuyaModel() {
		super(GensokyoLegacy.loc("sakuya"), "Head");
	}

	// The rig ships as plain "sakuya" rather than under her registry id, so every
	// path is spelled out instead of left to DefaultedEntityGeoModel's naming.
	private final ResourceLocation model = GensokyoLegacy.loc("geo/sakuya.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/sakuya.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/sakuya.animation.json");

	@Override
	public ResourceLocation getModelResource(SakuyaEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(SakuyaEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(SakuyaEntity animatable) {
		return animations;
	}

}
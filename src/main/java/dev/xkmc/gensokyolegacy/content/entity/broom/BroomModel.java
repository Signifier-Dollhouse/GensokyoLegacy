package dev.xkmc.gensokyolegacy.content.entity.broom;

import dev.xkmc.gensokyolegacy.util.HeadBoneGeoModel;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;

/**
 * Geo model for the broom: one static asset per file, with no head bone to
 * auto-turn and no colour variants.
 */
public class BroomModel extends HeadBoneGeoModel<BroomEntity> {

	private static final ResourceLocation MODEL = GensokyoLegacy.loc("geo/broom.geo.json");
	private static final ResourceLocation TEXTURE = GensokyoLegacy.loc("textures/geo/broom.png");
	private static final ResourceLocation ANIMATIONS = GensokyoLegacy.loc("animations/broom.animation.json");

	public BroomModel() {
		super(GensokyoLegacy.loc("broom"), null);
	}

	@Override
	public ResourceLocation getModelResource(BroomEntity animatable) {
		return MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(BroomEntity animatable) {
		return TEXTURE;
	}

	@Override
	public ResourceLocation getAnimationResource(BroomEntity animatable) {
		return ANIMATIONS;
	}

}
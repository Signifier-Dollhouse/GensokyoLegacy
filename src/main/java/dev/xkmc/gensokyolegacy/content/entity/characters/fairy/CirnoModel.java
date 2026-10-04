package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Cirno's Blockbench rig. Carries her hair, the green bow and the wings as bones
 * of the one model ({@code Green_Bowknot}, {@code LeftWing}/{@code RightWing},
 * the latter two flapped by her idle clip), which is why the separate hairband and
 * wings models the vanilla mob used are gone — see {@link CirnoEntity}.
 */
public class CirnoModel extends DefaultedEntityGeoModel<CirnoEntity> {

	private final ResourceLocation model = GensokyoLegacy.loc("geo/cirno.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/cirno.png");
	private final ResourceLocation tannedTexture = GensokyoLegacy.loc("textures/geo/cirno_tanned.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/cirno.animation.json");

	public CirnoModel() {
		super(GensokyoLegacy.loc("cirno"), "Head");
	}

	@Override
	public ResourceLocation getModelResource(CirnoEntity animatable) {
		return model;
	}

	/** The tanned variant is the same rig over a re-tinted sheet; purely cosmetic. */
	@Override
	public ResourceLocation getTextureResource(CirnoEntity animatable) {
		return animatable.isTanned() ? tannedTexture : texture;
	}

	@Override
	public ResourceLocation getAnimationResource(CirnoEntity animatable) {
		return animations;
	}

}

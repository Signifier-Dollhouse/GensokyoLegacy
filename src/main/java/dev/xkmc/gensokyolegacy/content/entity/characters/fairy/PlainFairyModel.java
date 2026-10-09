package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The plain fairy rig: one mesh, four recolours of it, selected by
 * {@link PlainFairyEntity#getVariant()}.
 * <p>
 * Note the shipped {@code animations/fairy.animation.json} was exported against an
 * older version of this mesh, so nine of the bone names it keys do not exist here and
 * GeckoLib drops those keyframes silently: {@code LeftWing}/{@code RightWing} (this
 * rig calls them {@code Left_Wing}/{@code Right_Wing}, so the wings do not flap),
 * {@code Front_Hair1} (here {@code Front_Hair}), and the six with no counterpart at
 * all — {@code Hat}, {@code BodyDecoration}, {@code Front_LeftHair},
 * {@code Front_LeftHair1a}, {@code Front_RightHair}, {@code Front_RightHair1a}.
 * The animation is wired up as shipped rather than renamed here; re-export it from
 * Blockbench against this mesh to get the wing flap and the hover hair back.
 */
public class PlainFairyModel extends DefaultedEntityGeoModel<PlainFairyEntity> {

	private static final String TEXTURE_PREFIX = "textures/geo/fairy_";

	private final ResourceLocation model = GensokyoLegacy.loc("geo/fairy.geo.json");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/fairy.animation.json");

	public PlainFairyModel() {
		super(GensokyoLegacy.loc("plain_fairy"), "Head");
	}

	@Override
	public ResourceLocation getModelResource(PlainFairyEntity animatable) {
		return model;
	}

	/** All four sheets are the same mesh recoloured; purely cosmetic. */
	@Override
	public ResourceLocation getTextureResource(PlainFairyEntity animatable) {
		return GensokyoLegacy.loc(TEXTURE_PREFIX + animatable.getVariant() + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(PlainFairyEntity animatable) {
		return animations;
	}

}
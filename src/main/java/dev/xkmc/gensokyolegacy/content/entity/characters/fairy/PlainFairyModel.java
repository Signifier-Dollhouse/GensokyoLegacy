package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The plain fairy rig: one mesh, five recolours of it, each of them in a bare and a
 * flower-crowned twin, selected by {@link PlainFairyEntity#getVariant()} and
 * {@link PlainFairyEntity#hasFlower()}.
 * <p>
 * The flower needs nothing of its own here: the {@code Flower} bone is in the one mesh
 * unconditionally, and the crown is what the {@code *_flower.png} sheets paint into the
 * texels the bare sheets leave transparent. So it is a sheet choice, not a second rig.
 * <p>
 * Note the shipped {@code animations/fairy.animation.json} still keys five bones that do
 * not exist in this mesh - {@code Front_Hair1} (here {@code Front_Hair}),
 * {@code Front_LeftHair}, {@code Front_LeftHair1a}, {@code Front_RightHair} and
 * {@code Front_RightHair1a} - and GeckoLib drops those keyframes silently. The wings and
 * the hair bow, which the previous export keyed under names this rig never had, do resolve
 * now. It is wired up as shipped rather than renamed here; re-export it from Blockbench
 * against this mesh to get the front hair strands back.
 */
public class PlainFairyModel extends DefaultedEntityGeoModel<PlainFairyEntity> {

	private static final String TEXTURE_PREFIX = "textures/geo/fairy_";
	private static final String FLOWER_SUFFIX = "_flower";

	private final ResourceLocation model = GensokyoLegacy.loc("geo/fairy.geo.json");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/fairy.animation.json");

	public PlainFairyModel() {
		super(GensokyoLegacy.loc("plain_fairy"), "Head");
	}

	@Override
	public ResourceLocation getModelResource(PlainFairyEntity animatable) {
		return model;
	}

	/** All ten sheets are the same mesh recoloured; purely cosmetic. */
	@Override
	public ResourceLocation getTextureResource(PlainFairyEntity animatable) {
		return GensokyoLegacy.loc(TEXTURE_PREFIX + animatable.getVariant()
				+ (animatable.hasFlower() ? FLOWER_SUFFIX : "") + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(PlainFairyEntity animatable) {
		return animations;
	}

}
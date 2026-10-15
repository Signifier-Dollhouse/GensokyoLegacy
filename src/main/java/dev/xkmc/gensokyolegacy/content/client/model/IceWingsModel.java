package dev.xkmc.gensokyolegacy.content.client.model;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** The wings rig as a model of its own, for {@link IceWingsLayer} to draw on a player. It is no longer a slice of
 * Cirno's, and both wing items share this one file: only the texture differs, and the layer picks it per wearer.
 * One model instance is deliberate - {@code BakedModelFactory} caches by resource location, so a second one
 * loading the same geo would have two processors fight over the same mutable bones.
 */
public class IceWingsModel extends GeoModel<IceWingsAnimatable> {

	@Override
	public ResourceLocation getModelResource(IceWingsAnimatable animatable) {
		return GensokyoLegacy.loc("geo/ice_wings.geo.json");
	}

	/** Only the fallback: the layer hands geckolib the wearer's own texture per frame instead. */
	@Override
	public ResourceLocation getTextureResource(IceWingsAnimatable animatable) {
		return GensokyoLegacy.loc("textures/geo/ice_wings.png");
	}

	@Override
	public ResourceLocation getAnimationResource(IceWingsAnimatable animatable) {
		return GensokyoLegacy.loc("animations/ice_wings.animation.json");
	}

}
package dev.xkmc.gensokyolegacy.content.client.model;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Cirno's wings as a model of their own, for {@link IceWingsLayer} to draw on a player: six bones copied verbatim
 * out of {@code geo/cirno.geo.json} so the two render identically. The copy is deliberate: {@code BakedModelFactory}
 * caches by resource location, so a shared geo would have both renderers fight over the same mutable bones.
 */
public class IceWingsModel extends GeoModel<IceWingsAnimatable> {

	@Override
	public ResourceLocation getModelResource(IceWingsAnimatable animatable) {
		return GensokyoLegacy.loc("geo/ice_wings.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(IceWingsAnimatable animatable) {
		return GensokyoLegacy.loc("textures/geo/cirno.png");
	}

	@Override
	public ResourceLocation getAnimationResource(IceWingsAnimatable animatable) {
		return GensokyoLegacy.loc("animations/ice_wings.animation.json");
	}

}
package dev.xkmc.gensokyolegacy.content.client.model;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Cirno's wings as a model of their own, for {@link IceWingsLayer} to draw on a player.
 *
 * <p>Her own wings are six bones of {@code geo/cirno.geo.json} rather than a model, which
 * {@code geo/ice_wings.geo.json} copies out verbatim - same pivots, same per-feather tilts, same
 * UVs, same {@code mirror} flags, and the same top-left corner of the same sheet, so the two
 * render identically. The copy is deliberate rather than a reference back to her file:
 * {@code BakedModelFactory} caches baked models by resource location and hands the same mutable
 * {@code GeoBone} objects to everyone, so two renderers sharing one geo would write their two
 * animations into one set of bones and fight over it.
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
package dev.xkmc.gensokyolegacy.content.client.model;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * The open scroll model worn on the player's back. The devil mode reuses the superhuman art until it
 * gets its own.
 */
public class DevilScrollLayerModel extends GeoModel<DevilScrollAnimatable> {

	@Override
	public ResourceLocation getModelResource(DevilScrollAnimatable animatable) {
		return GensokyoLegacy.loc("geo/devils_scroll_superhuman.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(DevilScrollAnimatable animatable) {
		return GensokyoLegacy.loc("textures/geo/devils_scroll_superhuman.png");
	}

	@Override
	public ResourceLocation getAnimationResource(DevilScrollAnimatable animatable) {
		return GensokyoLegacy.loc("animations/devils_scroll_superhuman.animation.json");
	}

}

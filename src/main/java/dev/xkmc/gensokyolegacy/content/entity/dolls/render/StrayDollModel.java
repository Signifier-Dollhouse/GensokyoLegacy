package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayDollEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The wild doll reuses the plain doll's rig and animations with one fixed, undyed texture: nothing
 * owns it, so there is no colour to carry.
 */
public class StrayDollModel extends DefaultedEntityGeoModel<StrayDollEntity> {

	public StrayDollModel() {
		super(StrayDollEntity.TYPE, "Head");
	}

	@Override
	public ResourceLocation getModelResource(StrayDollEntity animatable) {
		return GensokyoLegacy.loc("geo/doll.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(StrayDollEntity animatable) {
		return GensokyoLegacy.loc("textures/geo/doll/red.png");
	}

	@Override
	public ResourceLocation getAnimationResource(StrayDollEntity animatable) {
		return GensokyoLegacy.loc("animations/doll.animation.json");
	}

}

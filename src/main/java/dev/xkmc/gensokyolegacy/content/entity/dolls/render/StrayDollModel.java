package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayDollEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The wild doll has its own rig, animation set and texture: nothing owns it, so there is no colour to
 * carry, and its idle and flight clips are written for a doll with no controller.
 */
public class StrayDollModel extends DefaultedEntityGeoModel<StrayDollEntity> {

	public StrayDollModel() {
		super(StrayDollEntity.TYPE, "Head");
	}

	@Override
	public ResourceLocation getModelResource(StrayDollEntity animatable) {
		return GensokyoLegacy.loc("geo/stray_doll.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(StrayDollEntity animatable) {
		return GensokyoLegacy.loc("textures/geo/stray_doll.png");
	}

	@Override
	public ResourceLocation getAnimationResource(StrayDollEntity animatable) {
		return GensokyoLegacy.loc("animations/stray_doll.animation.json");
	}

}

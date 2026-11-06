package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.StrayGhostEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The wild ghost shares the ghost geo/animation files and the variant textures, so the player can
 * see what they are about to bind.
 */
public class StrayGhostModel extends DefaultedEntityGeoModel<StrayGhostEntity> {

	public StrayGhostModel() {
		super(StrayGhostEntity.TYPE, "Body");
	}

	@Override
	public ResourceLocation getModelResource(StrayGhostEntity animatable) {
		return GensokyoLegacy.loc("geo/ghost.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(StrayGhostEntity animatable) {
		return GensokyoLegacy.loc("textures/geo/ghost/" + animatable.getVariant().texture() + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(StrayGhostEntity animatable) {
		return GensokyoLegacy.loc("animations/ghost.animation.json");
	}

}

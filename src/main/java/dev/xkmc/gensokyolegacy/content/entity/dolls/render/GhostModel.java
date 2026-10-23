package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.ghost.GhostEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The owned ghost's variant texture is the only difference between its three looks; the geo and
 * animation files are shared with the wild ghost.
 */
public class GhostModel extends DefaultedEntityGeoModel<GhostEntity> {

	public GhostModel() {
		super(GhostEntity.TYPE, "Body");
	}

	@Override
	public ResourceLocation getModelResource(GhostEntity animatable) {
		return GensokyoLegacy.loc("geo/ghost.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GhostEntity animatable) {
		return GensokyoLegacy.loc("textures/geo/ghost/" + animatable.getVariant().texture() + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(GhostEntity animatable) {
		return GensokyoLegacy.loc("animations/ghost.animation.json");
	}

}

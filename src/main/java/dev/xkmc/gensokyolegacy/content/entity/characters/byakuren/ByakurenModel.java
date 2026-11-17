package dev.xkmc.gensokyolegacy.content.entity.characters.byakuren;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class ByakurenModel extends DefaultedEntityGeoModel<ByakurenEntity> {

	public ByakurenModel() {
		super(GensokyoLegacy.loc("byakuren"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/byakuren.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/byakuren.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/byakuren.animation.json");

	@Override
	public ResourceLocation getModelResource(ByakurenEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(ByakurenEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(ByakurenEntity animatable) {
		return animations;
	}

}

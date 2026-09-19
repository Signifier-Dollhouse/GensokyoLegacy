package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class DollModel extends DefaultedEntityGeoModel<DollEntity> {
	public DollModel() {
		super(GensokyoLegacy.loc("doll"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/doll.geo.json");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/doll.animation.json");

	@Override
	public ResourceLocation getModelResource(DollEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(DollEntity animatable) {
		// one pre-tinted texture per DyeColor (default red for a malformed/null lookup)
		return GensokyoLegacy.loc("textures/geo/doll/" + animatable.getColor().getName() + ".png");
	}

	@Override
	public ResourceLocation getAnimationResource(DollEntity animatable) {
		return animations;
	}
}

package dev.xkmc.gensokyolegacy.content.entity.characters.magician;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class AliceModel extends DefaultedEntityGeoModel<AliceEntity> {

	public AliceModel() {
		super(GensokyoLegacy.loc("alice"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/alice.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/alice.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/alice.animation.json");

	@Override
	public ResourceLocation getModelResource(AliceEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(AliceEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(AliceEntity animatable) {
		return animations;
	}

}

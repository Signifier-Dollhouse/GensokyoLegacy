package dev.xkmc.gensokyolegacy.content.entity.characters.yuyuko;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Saigyouji Yuyuko's Blockbench rig.
 */
public class YuyukoModel extends DefaultedEntityGeoModel<YuyukoEntity> {

	private final ResourceLocation model = GensokyoLegacy.loc("geo/yuyuko.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/yuyuko.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/yuyuko.animation.json");

	public YuyukoModel() {
		super(GensokyoLegacy.loc("yuyuko"), "Head");
	}

	@Override
	public ResourceLocation getModelResource(YuyukoEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(YuyukoEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(YuyukoEntity animatable) {
		return animations;
	}

}

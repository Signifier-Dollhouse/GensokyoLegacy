package dev.xkmc.gensokyolegacy.content.entity.characters.maiden;// Made with Blockbench 4.10.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ReimuModel extends GeoModel<ReimuEntity> {
	private final ResourceLocation model = GensokyoLegacy.loc("geo/reimu.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/reimu.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/morichika.animation.json");

	@Override
	public ResourceLocation getModelResource(ReimuEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(ReimuEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(ReimuEntity animatable) {
		return animations;
	}
}
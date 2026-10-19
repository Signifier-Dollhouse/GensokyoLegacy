package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import dev.xkmc.gensokyolegacy.content.entity.dolls.SekibankiHeadEntity;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * The head is the doll rig without its body, so it shares the doll's {@code Head} root bone and
 * takes a single untinted texture — there is no dye to recolour a gadget with.
 */
public class SekibankiHeadModel extends DefaultedEntityGeoModel<SekibankiHeadEntity> {
	public SekibankiHeadModel() {
		super(SekibankiHeadEntity.TYPE, "Head");
	}

	@Override
	public ResourceLocation getModelResource(SekibankiHeadEntity animatable) {
		return GensokyoLegacy.loc("geo/sekibanki_head.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(SekibankiHeadEntity animatable) {
		return GensokyoLegacy.loc("textures/geo/sekibanki_head.png");
	}

	@Override
	public ResourceLocation getAnimationResource(SekibankiHeadEntity animatable) {
		return GensokyoLegacy.loc("animations/sekibanki_head.animation.json");
	}
}
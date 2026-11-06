package dev.xkmc.gensokyolegacy.content.entity.characters.oni;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Suika's Blockbench rig.
 * <p>
 * The animation file was exported against a slightly different mesh revision, so five bones
 * it keys do not exist here and GeckoLib drops those keyframes silently: {@code Hat},
 * {@code Front_Hair1} and the three {@code *_DownCloth} bones. They affect only the moving
 * cloth and hat of {@code sit}, {@code sleep} and {@code hurt}, and every clip she actually
 * plays still drives real bones, so it is wired as shipped. Re-export from Blockbench against
 * this mesh to get the cloth motion back.
 */
public class SuikaModel extends DefaultedEntityGeoModel<SuikaEntity> {

	public SuikaModel() {
		super(GensokyoLegacy.loc("suika"), "Head");
	}

	private final ResourceLocation model = GensokyoLegacy.loc("geo/suika.geo.json");
	private final ResourceLocation texture = GensokyoLegacy.loc("textures/geo/suika.png");
	private final ResourceLocation animations = GensokyoLegacy.loc("animations/suika.animation.json");

	@Override
	public ResourceLocation getModelResource(SuikaEntity animatable) {
		return model;
	}

	@Override
	public ResourceLocation getTextureResource(SuikaEntity animatable) {
		return texture;
	}

	@Override
	public ResourceLocation getAnimationResource(SuikaEntity animatable) {
		return animations;
	}
}

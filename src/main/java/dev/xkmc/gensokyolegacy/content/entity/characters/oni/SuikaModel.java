package dev.xkmc.gensokyolegacy.content.entity.characters.oni;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * Suika's Blockbench rig.
 * <p>
 * The animation file was exported against a slightly different revision of the mesh, so
 * five of the bones it keys do not exist here and GeckoLib drops those keyframes without
 * complaint: {@code Hat} and {@code Front_Hair1}, and the three {@code *_DownCloth}
 * bones ({@code Front_DownCloth}, {@code Left_DownCloth}, {@code Right_DownCloth}).
 * They affect only the parts of {@code 坐下}, {@code 睡觉} and {@code 受伤} that were
 * moving cloth and a hat, and every clip she actually plays still drives real bones, so
 * the animation is wired up as shipped rather than renamed here. Re-export it from
 * Blockbench against this mesh to get the missing cloth motion back.
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

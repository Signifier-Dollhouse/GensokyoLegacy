package dev.xkmc.gensokyolegacy.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * 1.20.1 GeckoLib only knows the hard-coded {@code "head"} bone; this restores the 1.21 head-bone
 * name argument so a rig can name its head bone otherwise.
 */
public class HeadBoneGeoModel<T extends GeoAnimatable> extends DefaultedEntityGeoModel<T> {

	@Nullable
	private final String headBone;

	public HeadBoneGeoModel(ResourceLocation assetSubpath, @Nullable String headBone) {
		super(assetSubpath);
		this.headBone = headBone;
	}

	@Override
	public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
		if (headBone == null) return;
		CoreGeoBone head = getAnimationProcessor().getBone(headBone);
		if (head == null) return;
		EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
		head.setRotX(data.headPitch() * Mth.DEG_TO_RAD);
		head.setRotY(data.netHeadYaw() * Mth.DEG_TO_RAD);
	}

}

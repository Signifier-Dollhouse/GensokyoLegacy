package dev.xkmc.gensokyolegacy.content.entity.broom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * The geo renderer only turns the model on the vertical axis, which is right for
 * a walking mob but not for a broom: the rider aims it by looking, so the view
 * pitch has to be applied too. Vanilla uses the same yaw-then-pitch order (see
 * {@code SquidRenderer}), and the seat itself never pitches, so the rider stays
 * upright while the broom noses over.
 */
public class BroomRenderer extends GeoEntityRenderer<BroomEntity> {

	public BroomRenderer(EntityRendererProvider.Context context) {
		super(context, new BroomModel());
	}

	@Override
	protected void applyRotations(BroomEntity animatable, PoseStack poseStack, float ageInTicks,
			float rotationYaw, float partialTick, float nativeScale) {
		super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
		poseStack.mulPose(Axis.XP.rotationDegrees(-animatable.getViewXRot(partialTick)));
	}

}
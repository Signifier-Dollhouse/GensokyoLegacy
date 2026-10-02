package dev.xkmc.gensokyolegacy.content.entity.broom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * The broom is aimed by its rider, not by itself: its orientation is read off the
 * player every frame rather than from the entity's synced rotation. A boat does
 * not work this way — its yaw comes out of its own steering — but a broom has no
 * steering of its own, it simply points wherever the rider looks.
 *
 * <p>Pitch is taken at two thirds so the model swings through ±60° while the
 * player can look through ±90°; at full scale the shaft would bury itself in the
 * ground long before the player stopped looking down.
 */
public class BroomRenderer extends GeoEntityRenderer<BroomEntity> {

	/** Player pitch range is ±90°; the model gets ±60°. */
	private static final float PITCH_SCALE = 2.0F / 3.0F;

	public BroomRenderer(EntityRendererProvider.Context context) {
		super(context, new BroomModel());
	}

	/**
	 * Deliberately does not call {@code super}: that applies
	 * {@code 180 - entity yaw}, which is the wrong source for a ridden vehicle and
	 * would fight the yaw derived from the rider below. {@code BroomEntity} is
	 * neither a {@code LivingEntity} nor ever sleeping or dying, so the remaining
	 * branches of the superclass implementation do not apply to it.
	 */
	@Override
	protected void applyRotations(BroomEntity animatable, PoseStack poseStack, float ageInTicks,
			float rotationYaw, float partialTick, float nativeScale) {
		Player rider = animatable.getRider();
		float yaw = rotationYaw;
		float pitch = 0;
		if (rider != null) {
			yaw = Mth.rotLerp(partialTick, rider.yRotO, rider.getYRot());
			pitch = Mth.lerp(partialTick, rider.xRotO, rider.getXRot()) * PITCH_SCALE;
		}
		// vanilla order: yaw first, then pitch about the already-yawed local X axis.
		// the bristle end trails behind, so raising the pitch lifts the rider's end.
		poseStack.mulPose(Axis.YP.rotationDegrees(180f - yaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
	}

}
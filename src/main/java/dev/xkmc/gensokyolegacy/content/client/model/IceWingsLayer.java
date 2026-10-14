package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/** Draws {@link dev.xkmc.gensokyolegacy.content.item.character.IceWingsItem} on whoever wears it, from the same
 * sweep as {@link TouhouHatLayer} rather than the curios back slot, which has no renderer to hook. Drawing is
 * geckolib's {@link GeoRenderer} pipeline; only the animation is started here, as no entity exists to tick it.
 */
public class IceWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
		implements GeoRenderer<IceWingsAnimatable> {

	/** Cirno's rig suits her 26.2-unit chibi scale but hangs too low on a 32-unit player, so the model is
	 * nudged up a quarter block: her fan hinge then lands on the small of a player's back, not the hip.
	 */
	private static final double Y = 4 / 16D;
	/** Her feathers are planes at z=4 where her own body is only a few units deep; a player's back is at
	 * z=2, so they come forward a sixteenth to sit against it instead of a sixteenth behind.
	 */
	private static final double Z = -1 / 16D;

	private final IceWingsModel model = new IceWingsModel();
	private final IceWingsAnimatable animatable = new IceWingsAnimatable();

	public IceWingsLayer(RenderLayerParent<T, M> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffer, int light, T entity, float limbSwing,
					   float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
					   float headPitch) {
		if (!CuriosManager.hasWings(entity, GLItems.ICE_WINGS.get(), true)) return;
		if (entity.isInvisible() && !Minecraft.getInstance().shouldEntityAppearGlowing(entity)) return;
		// baked first: this is what hands the bones to the animation processor, and geckolib will
		// not have anything to write the clip's keyframes into until it has happened
		BakedGeoModel baked = model.getBakedModel(model.getModelResource(animatable));
		// her rig swaps to the hover clip the moment she leaves the ground, and so do these
		boolean airborne = !entity.onGround() || entity.isFallFlying();
		AnimationState<IceWingsAnimatable> state = new AnimationState<>(animatable, limbSwing,
				limbSwingAmount, partialTick, airborne);
		// keyed by wearer rather than by this layer, so that one player in mid-air does not drag
		// everybody else the same renderer is drawing into the hover clip with them
		model.handleAnimations(animatable, entity.getUUID().hashCode(), state, partialTick);
		pose.pushPose();
		pose.translate(0, -Y, Z);
		RenderType renderType = getRenderType(animatable, getTextureLocation(animatable), buffer, partialTick);
		this.actuallyRender(pose, animatable, baked, renderType, buffer, buffer.getBuffer(renderType),
				false, partialTick, light, LivingEntityRenderer.getOverlayCoords(entity, 0), 0xFFFFFFFF);
		pose.popPose();
	}

	@Override
	public IceWingsModel getGeoModel() {
		return model;
	}

	@Override
	public IceWingsAnimatable getAnimatable() {
		return animatable;
	}

	@Override
	public void fireCompileRenderLayersEvent() {
	}

	@Override
	public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel bakedModel,
									  MultiBufferSource bufferSource, float partialTick, int packedLight) {
		return false;
	}

	@Override
	public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel bakedModel,
								   MultiBufferSource bufferSource, float partialTick, int packedLight) {
	}

	@Override
	public void updateAnimatedTextureFrame(IceWingsAnimatable animatable) {
	}

}
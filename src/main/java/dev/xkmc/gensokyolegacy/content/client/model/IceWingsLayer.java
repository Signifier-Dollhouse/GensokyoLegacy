package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.compat.curios.CuriosManager;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Draws a pair of wings on whoever wears one, from the same sweep as {@link TouhouHatLayer} rather than the
 * curios back slot, which has no renderer to hook. Drawing is geckolib's {@link GeoRenderer} pipeline; only the
 * animation is started here, as no entity exists to tick it.
 *
 * <p>Both wing items share one rig and this one draw - a wearer with a pair in each slot still gets one set
 * of wings, which is the only thing that looks right. The items are told apart by texture alone, so which one
 * is worn has to be answered per wearer; see {@link #texture}.
 */
public class IceWingsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
		implements GeoRenderer<IceWingsAnimatable> {

	private final IceWingsModel model = new IceWingsModel();
	private final IceWingsAnimatable animatable = new IceWingsAnimatable();

	public IceWingsLayer(RenderLayerParent<T, M> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffer, int light, T entity, float limbSwing,
					   float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
					   float headPitch) {
		ItemStack wings = CuriosManager.getWings(entity, true).orElse(null);
		if (wings == null) return;
		if (entity.isInvisible() && !Minecraft.getInstance().shouldEntityAppearGlowing(entity)) return;
		// baked first: this is what hands the bones to the animation processor, and geckolib will
		// not have anything to write the clip's keyframes into until it has happened
		BakedGeoModel baked = model.getBakedModel(model.getModelResource(animatable));
		// her clips swap the moment she leaves the ground, and so do these
		boolean airborne = !entity.onGround() || entity.isFallFlying();
		AnimationState<IceWingsAnimatable> state = new AnimationState<>(animatable, limbSwing,
				limbSwingAmount, partialTick, airborne);
		// keyed by wearer rather than by this layer, so that one player in mid-air does not drag
		// everybody else the same renderer is drawing into the hover clip with them
		model.handleAnimations(animatable, entity.getUUID().hashCode(), state, partialTick);
		pose.pushPose();
		pose.translate(0, 12 / 16D, -1 / 16D);
		pose.mulPose(Axis.ZN.rotationDegrees(180));
		// the texture is passed in rather than read off the model: one animatable and one baked rig are
		// shared by every player on screen, so neither is where the wearer's own pair can be remembered
		ResourceLocation texture = texture(wings);
		RenderType renderType = getRenderType(animatable, texture, buffer, partialTick);
		this.actuallyRender(pose, animatable, baked, renderType, buffer, buffer.getBuffer(renderType),
				false, partialTick, light, LivingEntityRenderer.getOverlayCoords(entity, 0), 0xFFFFFFFF);
		pose.popPose();
	}

	/**
	 * The two wing items are one rig painted twice over, so the worn item is what picks the texture.
	 */
	private static ResourceLocation texture(ItemStack wings) {
		return GensokyoLegacy.loc(wings.is(GLItems.FAIRY_WINGS.get())
				? "textures/geo/fairy_wings.png" : "textures/geo/ice_wings.png");
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
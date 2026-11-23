package dev.xkmc.gensokyolegacy.content.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.content.item.devil.DevilScrollItem;
import dev.xkmc.gensokyolegacy.content.item.devil.DevilScrollMode;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
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
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Draws the open scroll on a player holding a superhuman scroll in the main hand, or a devil scroll in
 * either hand. The passive scroll and the superhuman offhand scroll stay closed in the hand instead;
 * the hands the open scroll covers are empty item models.
 *
 * @see "doc/design/item/devils_scroll.md"
 */
public class DevilScrollLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M>
		implements GeoRenderer<DevilScrollAnimatable> {

	private final DevilScrollLayerModel model = new DevilScrollLayerModel();
	private final DevilScrollAnimatable animatable = new DevilScrollAnimatable();

	public DevilScrollLayer(RenderLayerParent<T, M> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack pose, MultiBufferSource buffer, int light, T entity, float limbSwing,
					   float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
					   float headPitch) {
		DevilScrollMode mode = layerMode(entity);
		if (mode == null) return;
		if (entity.isInvisible() && !Minecraft.getInstance().shouldEntityAppearGlowing(entity)) return;
		// baked first: this is what hands the bones to the animation processor
		BakedGeoModel baked = model.getBakedModel(model.getModelResource(animatable));
		AnimationState<DevilScrollAnimatable> state = new AnimationState<>(animatable, limbSwing,
				limbSwingAmount, partialTick, false);
		// keyed by wearer rather than by this layer, so one player's phase does not drag the others
		model.handleAnimations(animatable, entity.getUUID().hashCode(), state, partialTick);
		pose.pushPose();
		// the same flip as the wings layer: cancels the entity model's (-1, -1, 1), with the scroll's
		// own z = 0 plane pushed out to the back like the wings' z = 4 rig
		pose.translate(0, 12 / 16D, 4 / 16D);
		pose.mulPose(Axis.ZN.rotationDegrees(180));
		ResourceLocation texture = texture(mode);
		RenderType renderType = getRenderType(animatable, texture, buffer, partialTick);
		this.actuallyRender(pose, animatable, baked, renderType, buffer, buffer.getBuffer(renderType),
				false, partialTick, light, LivingEntityRenderer.getOverlayCoords(entity, 0), 0xFFFFFFFF);
		pose.popPose();
	}

	/**
	 * Superhuman only shows from the main hand; the devil's open scroll answers either hand.
	 */
	@Nullable
	private static DevilScrollMode layerMode(LivingEntity entity) {
		ItemStack main = entity.getMainHandItem();
		if (main.getItem() instanceof DevilScrollItem) {
			DevilScrollMode mode = DevilScrollItem.getMode(main);
			if (mode == DevilScrollMode.SUPERHUMAN || mode == DevilScrollMode.DEVIL) return mode;
		}
		ItemStack off = entity.getOffhandItem();
		if (off.getItem() instanceof DevilScrollItem && DevilScrollItem.getMode(off) == DevilScrollMode.DEVIL) {
			return DevilScrollMode.DEVIL;
		}
		return null;
	}

	/**
	 * Devil mode reuses the superhuman open scroll until it gets art of its own.
	 */
	private static ResourceLocation texture(DevilScrollMode mode) {
		return GensokyoLegacy.loc("textures/geo/devils_scroll_superhuman.png");
	}

	@Override
	public DevilScrollLayerModel getGeoModel() {
		return model;
	}

	@Override
	public DevilScrollAnimatable getAnimatable() {
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
	public void updateAnimatedTextureFrame(DevilScrollAnimatable animatable) {
	}

}

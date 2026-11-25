package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayDollEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import static dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer.itemBuffer;

/**
 * Renders the stray doll's held items at the rig's hand locators. The off hand is what carries the
 * leader's shield.
 */
public class StrayDollHeldItemLayer extends BlockAndItemGeoLayer<StrayDollEntity> {

	public StrayDollHeldItemLayer(GeoRenderer<StrayDollEntity> renderer) {
		super(renderer);
	}

	@Override
	@Nullable
	protected ItemStack getStackForBone(GeoBone bone, StrayDollEntity doll) {
		ItemStack stack = switch (bone.getName()) {
			case DollHeldItemLayer.RIGHT_HAND_BONE -> doll.getMainHandItem();
			case DollHeldItemLayer.LEFT_HAND_BONE -> doll.getOffhandItem();
			default -> ItemStack.EMPTY;
		};
		return stack.isEmpty() ? null : stack;
	}

	@Override
	protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, StrayDollEntity doll) {
		return bone.getName().equals(DollHeldItemLayer.LEFT_HAND_BONE)
				? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
				: ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	/**
	 * Rendered directly so the left-hand flag can cancel the mirror vanilla's mirrored transform
	 * carries; {@code super} hardcodes {@code leftHand = false}.
	 */
	@Override
	protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, StrayDollEntity doll,
									  MultiBufferSource bufferSource, float partialTick, int packedLight,
									  int packedOverlay) {
		var left = bone.getName().equals(DollHeldItemLayer.LEFT_HAND_BONE);
		poseStack.pushPose();
		poseStack.scale(0.8F, 0.8F, 0.8F);
		poseStack.translate(0.0D, 0.0D, -0.2D);
		Minecraft.getInstance().getItemRenderer().renderStatic(doll, stack,
				getTransformTypeForStack(bone, stack, doll), left, poseStack, itemBuffer(bufferSource),
				doll.level(), packedLight, packedOverlay, doll.getId());
		poseStack.popPose();
	}

}

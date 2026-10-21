package dev.xkmc.gensokyolegacy.content.entity.youkai.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeneralYoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Renders whatever a youkai holds at her wrist locator bones. A geo renderer draws no items by itself, so
 * without this a character would lose the held item the moment she became geo-rendered. Unlike the dolls,
 * their hands are ordinary vanilla equipment, so the stacks come straight off the entity.
 * <p>
 * Every rig here names the two wrist bones the same way ({@code RightHandLocator}/{@code LeftHandLocator}),
 * so one layer serves all of them.
 */
public class YoukaiHeldItemLayer<T extends GeneralYoukaiEntity & GeoYoukaiAnim> extends BlockAndItemGeoLayer<T> {

	public static final String RIGHT_HAND_BONE = "RightHandLocator";
	public static final String LEFT_HAND_BONE = "LeftHandLocator";

	public YoukaiHeldItemLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Override
	@Nullable
	protected ItemStack getStackForBone(GeoBone bone, T entity) {
		ItemStack stack = switch (bone.getName()) {
			case RIGHT_HAND_BONE -> entity.getItemBySlot(EquipmentSlot.MAINHAND);
			case LEFT_HAND_BONE -> entity.getItemBySlot(EquipmentSlot.OFFHAND);
			default -> ItemStack.EMPTY;
		};
		return stack.isEmpty() ? null : stack;
	}

	@Override
	protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, GeneralYoukaiEntity doll) {
		return bone.getName().equals(LEFT_HAND_BONE)
				? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
				: ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	/**
	 * Renders the stack itself instead of delegating to {@code super}, which hardcodes
	 * {@code leftHand = false} and so cannot reproduce vanilla's third-person left hand.
	 * <p>
	 * Vanilla authors {@code thirdperson_lefthand} as the mirror of
	 * {@code thirdperson_righthand} and cancels that mirror at apply time, so both hands
	 * get the <em>same</em> hand-relative transform and the mirrored arm bone does the
	 * mirroring. The two locator bones are exact X-mirrors of each other, so asking for
	 * the left-hand context alone lands the item one mirror too far; the flag is what
	 * takes that extra mirror back out.
	 */
	@Override
	protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, GeneralYoukaiEntity doll,
	                                  MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		var left = bone.getName().equals(LEFT_HAND_BONE);
		poseStack.pushPose();
		poseStack.scale(0.8F, 0.8F, 0.8F);
		poseStack.mulPose(Axis.XP.rotationDegrees(-90));
//		poseStack.translate(0.0D, 0.0D, -0.2D);
		Minecraft.getInstance().getItemRenderer().renderStatic(doll, stack,
				getTransformTypeForStack(bone, stack, doll), left, poseStack, itemBuffer(bufferSource),
				doll.level(), packedLight, packedOverlay, doll.getId());
		poseStack.popPose();
	}

	/**
	 * While glowing, the whole model is drawn through the {@link OutlineBufferSource},
	 * whose outline source is one shared buffer with no fixed buffers: asking it for a
	 * new render type ends the batch of the previous one, which drops the geometry of
	 * the following bones. The item is not part of the silhouette anyway, so route it
	 * to the main buffer source instead.
	 */
	public static MultiBufferSource itemBuffer(MultiBufferSource bufferSource) {
		if (bufferSource instanceof OutlineBufferSource)
			return Minecraft.getInstance().renderBuffers().bufferSource();
		return bufferSource;
	}
}
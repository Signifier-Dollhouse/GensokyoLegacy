package dev.xkmc.gensokyolegacy.content.entity.dolls.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.entity.dolls.impl.DollLoadout;
import dev.xkmc.gensokyolegacy.content.item.doll.DollSlot;
import dev.xkmc.gensokyolegacy.init.registrate.GLItems;
import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;
import software.bernie.geckolib.util.RenderUtil;

import static dev.xkmc.gensokyolegacy.content.entity.youkai.renderer.YoukaiHeldItemLayer.itemBuffer;

/**
 * Renders the doll loadout hand slots at the model's hand locator bones.
 * Reads the synced client mirror via {@link DollLoadout#getLoadoutItem} — dolls
 * deliberately bypass vanilla equipment slots, so {@code getMainHandItem} stays empty.
 *
 * @see "doc/design/doll/loadout.md"
 */
public class DollHeldItemLayer extends BlockAndItemGeoLayer<DollEntity> {

	public static final String RIGHT_HAND_BONE = "RightHandLocator";
	public static final String LEFT_HAND_BONE = "LeftHandLocator";

	private final DollRenderer dollRenderer;

	public DollHeldItemLayer(DollRenderer renderer) {
		super(renderer);
		this.dollRenderer = renderer;
	}

	@Override
	@Nullable
	protected ItemStack getStackForBone(GeoBone bone, DollEntity doll) {
		ItemStack stack = switch (bone.getName()) {
			case RIGHT_HAND_BONE -> doll.getLoadoutItem(DollSlot.MAIN_HAND);
			case LEFT_HAND_BONE -> doll.getLoadoutItem(DollSlot.OFF_HAND);
			default -> ItemStack.EMPTY;
		};
		return stack.isEmpty() ? null : stack;
	}

	/**
	 * The bone's own side, not always right: items whose model carries separate
	 * per-side transforms render unmirrored on the left arm otherwise.
	 * <p>
	 * Pairs with the {@code leftHand} flag in {@link #renderStackForBone}: the context
	 * picks <em>which</em> authored transform, the flag cancels the mirror it carries.
	 */
	@Override
	protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, DollEntity doll) {
		return bone.getName().equals(LEFT_HAND_BONE)
				? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
				: ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
	}

	@Override
	public void renderForBone(PoseStack poseStack, DollEntity doll, GeoBone bone, RenderType renderType,
							  MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
							  int packedLight, int packedOverlay) {
		ItemStack stack = getStackForBone(bone, doll);
		if (stack == null) return;
		poseStack.pushPose();
		RenderUtil.translateToPivotPoint(poseStack, bone);
		renderStackForBone(poseStack, bone, stack, doll, bufferSource, partialTick, packedLight, packedOverlay);
		poseStack.popPose();
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
	protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, DollEntity doll,
									  MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
		var left = bone.getName().equals(LEFT_HAND_BONE);
		poseStack.pushPose();
		poseStack.scale(0.8F, 0.8F, 0.8F);
		poseStack.translate(0.0D, 0.0D, -0.2D);
		if (!left && stack.is(GLItems.DOLL_LANCE.get())) {
			SpearPose spear = spearPose(poseStack, bone, stack, doll, partialTick);
			dollRenderer.stabilizeSpearPose(doll, poseStack, spear.direction());
			if (dollRenderer.consumeSpearParticle(doll)) {
				spear = spearPose(poseStack, bone, stack, doll, partialTick);
				Vec3 tip = spear.tip();
				Vec3 direction = spear.direction();
				doll.level().addParticle(GLParticles.SPEAR.get(), tip.x, tip.y, tip.z,
						direction.x, direction.y, direction.z);
			}
		}
		Minecraft.getInstance().getItemRenderer().renderStatic(doll, stack,
				getTransformTypeForStack(bone, stack, doll), left, poseStack, itemBuffer(bufferSource),
				doll.level(), packedLight, packedOverlay, doll.getId());
		poseStack.popPose();
	}

	private SpearPose spearPose(PoseStack poseStack, GeoBone bone, ItemStack stack,
							   DollEntity doll, float partialTick) {
		poseStack.pushPose();
		var itemRenderer = Minecraft.getInstance().getItemRenderer();
		var model = itemRenderer.getModel(stack, doll.level(), doll, doll.getId());
		ClientHooks.handleCameraTransforms(poseStack, model, getTransformTypeForStack(bone, stack, doll), false);
		poseStack.translate(-0.5F, -0.5F, -0.5F);
		Vec3 tip = dollRenderer.itemPointToWorld(doll, poseStack, new Vector3f(8F / 16, 2.5F / 16, -11F / 16),
				partialTick);
		Vec3 shaft = dollRenderer.itemPointToWorld(doll, poseStack, new Vector3f(8F / 16, 2.5F / 16, 17F / 16),
				partialTick);
		poseStack.popPose();
		return new SpearPose(tip, tip.subtract(shaft).normalize());
	}

	private record SpearPose(Vec3 tip, Vec3 direction) {
	}

}

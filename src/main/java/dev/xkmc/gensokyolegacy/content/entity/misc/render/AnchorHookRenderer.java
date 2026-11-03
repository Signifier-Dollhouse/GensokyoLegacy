package dev.xkmc.gensokyolegacy.content.entity.misc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.content.entity.misc.anchor.AnchorHookEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the bounded anchor with its fixed throw orientation, and the chain that ties it back to the
 * thrower.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public class AnchorHookRenderer extends EntityRenderer<AnchorHookEntity> {

	private static final ResourceLocation CHAIN = ResourceLocation.withDefaultNamespace("textures/block/chain.png");
	private static final RenderType CHAIN_TYPE = RenderType.entityCutoutNoCull(CHAIN);
	private static final float CHAIN_HALF = 3F / 32F;
	private static final float CHAIN_TEX = 3F / 16F;

	private final ItemRenderer itemRenderer;

	public AnchorHookRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(AnchorHookEntity entity, float yaw, float partialTick, PoseStack pose,
					   MultiBufferSource buffer, int packedLight) {
		renderChain(entity, partialTick, pose, buffer, packedLight);
		pose.pushPose();
		// projectile yaw is atan2(dx, dz) and pitch counts up, the mirror of the look convention
		pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
		pose.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
		layFlat(pose);
		itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.GROUND, packedLight,
				OverlayTexture.NO_OVERLAY, pose, buffer, entity.level(), entity.getId());
		pose.popPose();
		super.render(entity, yaw, partialTick, pose, buffer, packedLight);
	}

	/**
	 * Two crossed ribbons of the vanilla chain texture. Splitting the chain into one-block segments
	 * keeps the links a constant size and avoids relying on texture wrapping.
	 */
	private void renderChain(AnchorHookEntity entity, float partialTick, PoseStack pose,
							 MultiBufferSource buffer, int packedLight) {
		if (!(entity.getOwner() instanceof Entity owner)) return;
		Vec3 anchor = entity.getPosition(partialTick);
		Vec3 hand = owner.getPosition(partialTick).add(0, owner.getBbHeight() * 0.6, 0);
		Vec3 diff = anchor.subtract(hand);
		double len = diff.length();
		if (len < 0.2) return;
		Vec3 dir = diff.scale(1 / len);
		Vec3 side = dir.cross(new Vec3(0, 1, 0));
		if (side.lengthSqr() < 1e-4) side = dir.cross(new Vec3(1, 0, 0));
		side = side.normalize();
		Vec3 up = dir.cross(side).normalize();

		pose.pushPose();
		pose.translate(hand.x - anchor.x, hand.y - anchor.y, hand.z - anchor.z);
		PoseStack.Pose last = pose.last();
		VertexConsumer vc = buffer.getBuffer(CHAIN_TYPE);
		int segments = Mth.ceil(len);
		for (int i = 0; i < segments; i++) {
			double t0 = i * len / segments;
			double t1 = (i + 1) * len / segments;
			Vec3 p0 = dir.scale(t0);
			Vec3 p1 = dir.scale(t1);
			float v0 = 0;
			float v1 = (float) (t1 - t0);
			ribbon(vc, last, p0, p1, side, CHAIN_HALF, 0, CHAIN_TEX, v0, v1, packedLight);
			ribbon(vc, last, p0, p1, up, CHAIN_HALF, CHAIN_TEX, CHAIN_TEX * 2, v0, v1, packedLight);
		}
		pose.popPose();
	}

	private static void ribbon(VertexConsumer vc, PoseStack.Pose pose, Vec3 from, Vec3 to, Vec3 axis,
							   float half, float u0, float u1, float v0, float v1, int light) {
		Vec3 off = axis.scale(half);
		vertex(vc, pose, from.subtract(off), u0, v0, light);
		vertex(vc, pose, to.subtract(off), u0, v1, light);
		vertex(vc, pose, to.add(off), u1, v1, light);
		vertex(vc, pose, from.add(off), u1, v0, light);
	}

	private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 pos, float u, float v, int light) {
		vc.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, 0, 1, 0);
	}

	/**
	 * The model is authored upright with its shaft along +y, while the flight frame's +z is the
	 * throw direction. Tip it a quarter turn and cancel the {@code translate(-0.5)} the item
	 * renderer applies anyway, so the transform matches the danmaku renderer's exactly.
	 *
	 * @see "doc/design/minamitsu_anchor.md §5.4"
	 */
	private static void layFlat(PoseStack pose) {
		pose.mulPose(Axis.XP.rotationDegrees(90));
	}

	@Override
	public ResourceLocation getTextureLocation(AnchorHookEntity entity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}

}

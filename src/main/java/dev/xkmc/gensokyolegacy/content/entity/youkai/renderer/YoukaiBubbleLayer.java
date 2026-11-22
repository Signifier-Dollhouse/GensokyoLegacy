package dev.xkmc.gensokyolegacy.content.entity.youkai.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.AbstractBubbleModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.IBubbleModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
import dev.xkmc.gensokyolegacy.content.entity.youkai.impl.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import org.joml.Vector3f;

/**
 * The want bubble: the pill from {@code textures/bubble/bubble.png} above her head, with the item she is
 * asking for in the middle of it. Drawn for any character whose modules hold an {@link IBubbleModule}; one
 * without simply draws nothing, so the layer needs no gate of its own.
 * <p>
 * A {@link GeoRenderLayer} and not a world pass, because the pose a layer is handed is the one a name tag is
 * drawn in - which also puts the bubble in the entity's own pass, at the entity's own distance.
 * <p>
 * The bubble faces the player rather than the camera: the yaw is the horizontal player-to-entity direction, so
 * in third person it still turns to the player instead of to the camera behind them. Everything is drawn in the
 * frame that yaw and the {@link #SCALE} flip leave behind - the one {@code cameraOrientation()} would give a
 * camera at the player looking at her - {@code +x} right, {@code +y} down, {@code +z} away from the player, one
 * unit to a sheet pixel.
 *
 * @see "doc/design/want_bubble.md"
 */
public class YoukaiBubbleLayer<T extends YoukaiEntity & GeoYoukaiAnim> extends GeoRenderLayer<T> {

	private static final ResourceLocation TEXTURE = GensokyoLegacy.loc("textures/bubble/bubble.png");
	/**
	 * The text render type: a billboard sprite is the same draw as a name tag's text, so it gets the same
	 * translucent pass and vertex format. The quad is wound the way a name tag's glyphs are, so it is not
	 * culled.
	 */
	private static final RenderType BUBBLE = RenderType.text(TEXTURE);

	/**
	 * The sheet is a 256px square. The bubble is the {@code 48 x 24} cell at its top-left; the three tails are
	 * complete {@code 16 x 16} cells immediately below it.
	 */
	private static final float SHEET = 256f;
	private static final float PILL_W = 48f;
	private static final float PILL_H = 24f;
	private static final float TAIL_SIZE = 16f;
	private static final float TAIL_Y = 24f;

	/**
	 * Blocks per sheet pixel. The frame is scaled by this once, and everything after it is drawn in sheet
	 * pixels.
	 */
	private static final float SCALE = 0.025f;

	/**
	 * How far the bottom edge of the pill hangs above the top of her hitbox. A plain gap: she hovers, so there
	 * is nothing for the bubble to sit on, and it is measured from her hitbox because the model is not what
	 * the bubble belongs to.
	 */
	private static final float LIFT = 0.25f;

	/**
	 * How far off to one side the bubble is put, in sheet pixels. Small on purpose: a fairy is 0.4 blocks
	 * wide, and a bubble that had drifted a whole block off her head would stop reading as hers.
	 */
	private static final float SIDE_SHIFT = 12f;

	/**
	 * How long the bubble stays up: ten seconds. Long enough to be read, short enough that the next one twelve
	 * to twenty seconds later is a new thought rather than a flicker of the same one.
	 */
	private static final int SHOW_TICKS = 200;

	/**
	 * Past this the bubble is smaller than the item inside it. Sixty-four blocks is the range a name tag stops
	 * being drawn at, and there is nothing here that deserves to outlive a name tag.
	 */
	private static final double RANGE = 64;

	public YoukaiBubbleLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, @Nullable RenderType renderType,
					   MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
					   int packedLight, int packedOverlay) {
		var module = entity.getModule(IBubbleModule.class).orElse(null);
		if (module == null) return;
		ItemStack want = module.getWantBubble();
		if (want.isEmpty() || entity.isInvisible()) return;
		// a thought is not held back while she is eating or asleep, it simply is not shown: the bubble goes
		// the tick either starts, and the next one arrives on the usual clock
		if (!module.mayShowWant()) return;
		float age = module.getWantBubbleAge(partialTick);
		if (age < 0 || age > SHOW_TICKS) return;
		Minecraft mc = Minecraft.getInstance();
		var dispatcher = mc.getEntityRenderDispatcher();
		if (dispatcher.distanceToSqr(entity) > RANGE * RANGE) return;
		var player = mc.player;
		if (player == null) return;
		poseStack.pushPose();

		poseStack.translate(0, entity.getBbHeight() + LIFT + PILL_H * SCALE / 2, 0);
		poseStack.mulPose(Axis.YP.rotationDegrees(-yawToPlayer(entity, player, partialTick)));
		poseStack.scale(-SCALE, -SCALE, SCALE);
		poseStack.translate(sideShift(module.getWantPlacement()), 0, 0);
		var bubbleBuffer = bufferSource.getBuffer(BUBBLE);
		drawPill(poseStack, bubbleBuffer);
		drawTail(poseStack, bubbleBuffer, module.getWantPlacement());
		poseStack.scale(16,16,16);
		poseStack.mulPose(Axis.XP.rotationDegrees(180));
		// GUI context renders the inventory icon instead of a world item model. Full brightness keeps the icon
		// legible at night.
		mc.getItemRenderer().renderStatic(want, ItemDisplayContext.GUI, LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), 0);
		poseStack.popPose();
	}

	/**
	 * The yaw of a camera at the player's eyes looking at her, in degrees: interpolated the same way the entity
	 * and the player are drawn, so the bubble does not jitter against them. 1.20.1's {@code cameraOrientation()}
	 * for that yaw is {@code Axis.YP.rotationDegrees(-yaw)}.
	 */
	private static float yawToPlayer(YoukaiEntity entity, Player player, float pTick) {
		double ex = Mth.lerp(pTick, entity.xo, entity.getX());
		double ez = Mth.lerp(pTick, entity.zo, entity.getZ());
		double px = Mth.lerp(pTick, player.xo, player.getX());
		double pz = Mth.lerp(pTick, player.zo, player.getZ());
		return (float) Math.toDegrees(Math.atan2(px - ex, ez - pz));
	}

	/**
	 * How far to one side of her head the bubble is put, in sheet pixels: nothing in the middle, and
	 * {@link #SIDE_SHIFT} either way otherwise, as the player sees it.
	 */
	private static float sideShift(int placement) {
		return switch (placement) {
			case AbstractBubbleModule.WANT_LEFT -> -SIDE_SHIFT;
			case AbstractBubbleModule.WANT_RIGHT -> SIDE_SHIFT;
			default -> 0f;
		};
	}

	/**
	 * The pill, centred on the frame's origin: the sheet's top-left corner is the pill's own, so the four
	 * corners are the whole of it.
	 */
	private static void drawPill(PoseStack poseStack, VertexConsumer vc) {
		drawTextureRect(poseStack, vc, 0, 0, (int) PILL_W, (int) PILL_H, -PILL_W / 2, -PILL_H / 2);
	}

	/**
	 * Draws the one tail whose position was rolled with the current want.
	 */
	private static void drawTail(PoseStack poseStack, VertexConsumer vc, int placement) {
		int sourceX = switch (placement) {
			case AbstractBubbleModule.WANT_LEFT -> 32;
			case AbstractBubbleModule.WANT_RIGHT -> 0;
			default -> 16;
		};
		poseStack.pushPose();
		int offset = 24;
		switch (placement) {
			case AbstractBubbleModule.WANT_LEFT -> poseStack.translate(offset, 9, 0.05);
			case AbstractBubbleModule.WANT_RIGHT -> poseStack.translate(-offset, 9, 0.05);
			default -> 	poseStack.translate(0, 8, 0.05);
		}
		drawTextureRect(poseStack, vc, sourceX, (int) TAIL_Y, (int) TAIL_SIZE, (int) TAIL_SIZE,
				-TAIL_SIZE / 2, -TAIL_SIZE / 2);
		poseStack.popPose();
	}

	private static void drawTextureRect(PoseStack poseStack, VertexConsumer vc, int sourceX, int sourceY,
										int sourceWidth, int sourceHeight, float destinationX, float destinationY) {
		var pose = poseStack.last();
		float left = destinationX;
		float right = destinationX + sourceWidth;
		float top = destinationY;
		float bottom = destinationY + sourceHeight;
		float u0 = sourceX / SHEET;
		float u1 = (sourceX + sourceWidth) / SHEET;
		float v0 = sourceY / SHEET;
		float v1 = (sourceY + sourceHeight) / SHEET;
		addVertex(vc, pose, left, bottom, u0, v1);
		addVertex(vc, pose, right, bottom, u1, v1);
		addVertex(vc, pose, right, top, u1, v0);
		addVertex(vc, pose, left, top, u0, v0);
	}

	private static void addVertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float u, float v) {
		Vector3f n = pose.normal().transform(new Vector3f(0, 0, 1));
		vc.vertex(pose.pose(), x, y, 0)
				.color(255, 255, 255, 255)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(LightTexture.FULL_BRIGHT)
				.normal(n.x(), n.y(), n.z())
				.endVertex();
	}

}
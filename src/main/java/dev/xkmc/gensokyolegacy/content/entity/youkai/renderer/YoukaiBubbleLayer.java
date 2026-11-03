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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * The want bubble: the pill from {@code textures/bubble/bubble.png} above her head, with the item she is
 * asking for in the middle of it. Drawn for any character whose modules hold an {@link IBubbleModule}; one
 * without simply draws nothing, so the layer needs no gate of its own.
 * <p>
 * A {@link GeoRenderLayer} and not a world pass, because the pose a layer is handed is the one a name tag is
 * drawn in - which also puts the bubble in the entity's own pass, at the entity's own distance.
 * <p>
 * Everything is drawn in the frame {@code cameraOrientation()} leaves behind - {@code +x} right, {@code +y} up,
 * {@code +z} at the camera, one unit to a block - so a bubble off to one side is off to the side of the
 * screen rather than of the world.
 *
 * @see "doc/design/want_bubble.md"
 */
public class YoukaiBubbleLayer<T extends YoukaiEntity & GeoYoukaiAnim> extends GeoRenderLayer<T> {

	private static final ResourceLocation TEXTURE = GensokyoLegacy.loc("textures/bubble/bubble.png");
	/**
	 * {@code NoCull}: a flat sprite has no inside to hide, and a quad that came out wound the other way
	 * round would simply vanish if there were.
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
	 * The middle of the pill's inside, in sheet pixels, which runs {@code x 3-44, y 3-20} - so half a pixel off
	 * the pill's own centre. Where the item goes.
	 */
	private static final float ICON_CX = 23.5f;
	private static final float ICON_CY = 14.5f;

	/**
	 * How far the item is placed in front of the bubble in the pixel coordinate frame.
	 */
	private static final float ICON_LIFT = -0.01f;

	/**
	 * How far the bottom edge of the pill hangs above the top of her hitbox. A plain gap: she hovers, so there
	 * is nothing for the bubble to sit on, and it is measured from her hitbox because the model is not what
	 * the bubble belongs to.
	 */
	private static final float LIFT = 0.25f;

	/**
	 * How far off to one side the bubble is put. Small on purpose: a fairy is 0.4 blocks wide, and a bubble
	 * that had drifted a whole block off her head would stop reading as hers.
	 */
	private static final float SIDE_SHIFT = 0.3f;

	/**
	 * How long the bubble stays up: three seconds. Long enough to be read, short enough that the next one ten
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
		poseStack.pushPose();

		poseStack.translate(0, entity.getBbHeight() + LIFT, 0);

		poseStack.mulPose(dispatcher.cameraOrientation());
		poseStack.mulPose(Axis.YP.rotationDegrees(180));
		switch (module.getWantPlacement()) {
			case AbstractBubbleModule.WANT_LEFT -> poseStack.translate(1, 0, 0);
			case AbstractBubbleModule.WANT_RIGHT -> poseStack.translate(-1, 0, 0);
			default -> poseStack.translate(0, 0.2, 0);
		}
		poseStack.scale(-0.025F, -0.025F, 0.025F);
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
	 * How far to one side of her head the bubble is put, in blocks: nothing in the middle, and
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
			case AbstractBubbleModule.WANT_LEFT -> poseStack.translate(offset, 9, -0.05);
			case AbstractBubbleModule.WANT_RIGHT -> poseStack.translate(-offset, 9, -0.05);
			default -> 	poseStack.translate(0, 8, -0.05);
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
		vc.addVertex(pose, x, y, 0)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT)
				.setNormal(0, 0, 1);
	}

}
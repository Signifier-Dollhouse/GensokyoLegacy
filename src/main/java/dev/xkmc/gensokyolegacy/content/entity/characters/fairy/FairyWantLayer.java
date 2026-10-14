package dev.xkmc.gensokyolegacy.content.entity.characters.fairy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.gensokyolegacy.content.entity.youkai.GeoYoukaiAnim;
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
 * The want bubble: the pill from {@code textures/bubble/bubble.png} above her head, with the
 * item she is asking for - {@link FairyEntity#getWantBubble()} - in the middle of it, up for
 * three seconds at a time. Registered on the two fairy renderers that carry a rig, see
 * {@code PlainFairyRenderer} and {@code CirnoRenderer}.
 * <p>
 * The sheet is a pill with three marks hanging under it and only the pill is drawn. The marks
 * are the sheet offering the same bubble at three angles, and the bubble turned out to want
 * none of them: a bubble floating with a gap over her head reads as hers on its own, where a
 * tail has to be joined to the bottom border exactly right to read as anything but a wedge
 * cut off flat. Since nothing needs pointing at her any more, {@link #LIFT} is free to be a
 * plain gap, and the three placements move the whole bubble instead of choosing which mark
 * carries it - see {@link #sideShift}.
 * <p>
 * It is a {@link GeoRenderLayer} and not a world pass, because the pose a layer is handed is
 * already the one a name tag is drawn in. The body yaw is applied inside
 * {@code GeoEntityRenderer#actuallyRender}, in a frame it pushes and pops itself, so by the
 * time the layers run the pose carries the entity's position and nothing else - the same
 * place vanilla draws a name tag from. That makes billboarding one {@code mulPose} against
 * the camera orientation, and it also means the bubble is drawn with the entity: it goes into
 * the same buffers, in the same pass, at the same distance.
 * <p>
 * Everything is drawn in the frame that {@code mulPose} leaves behind - {@code +x} right,
 * {@code +y} up, {@code +z} at the camera, and one unit to the block - so a bubble put off to
 * one side is off to the side of the screen rather than of the world, which is what "at her
 * upper left" means once the camera is not straight on, and the item is shoved a little
 * further along {@code +z} so the pill's fill does not swallow it.
 */
public class FairyWantLayer<T extends FairyEntity & GeoYoukaiAnim> extends GeoRenderLayer<T> {

	private static final ResourceLocation TEXTURE = GensokyoLegacy.loc("textures/bubble/bubble.png");
	/**
	 * {@code NoCull}: a flat sprite has no inside to hide, so there is nothing for back-face
	 * culling to protect - and a quad that came out wound the other way round would simply
	 * vanish if there were.
	 */
	private static final RenderType BUBBLE = RenderType.entityCutoutNoCull(TEXTURE);

	/**
	 * The sheet is a 256px square and the pill occupies its top-left corner, {@code 48 x 24}.
	 */
	private static final float SHEET = 256f;
	private static final float PILL_W = 48f;
	private static final float PILL_H = 24f;

	/**
	 * Blocks per sprite pixel. The bubble comes out a little under a block wide, which is
	 * name-plate scale: big enough to read the item, small enough not to be the thing the
	 * player is looking at.
	 */
	private static final float SCALE = 0.02f;

	/**
	 * The middle of the pill's inside, in sheet pixels - where the item goes. The pill's
	 * inside runs {@code x 3-44, y 3-20}, so its centre is half a pixel off the pill's own.
	 */
	private static final float ICON_CX = 23.5f;
	private static final float ICON_CY = 11.5f;

	/**
	 * The item, in sprite pixels. The pill is 18px tall inside, so 13 leaves it a 2.5px margin
	 * all round rather than pressing against the outline.
	 */
	private static final float ICON_PX = 13f;
	private static final float ICON = ICON_PX * SCALE;

	/**
	 * How far towards the camera the item is pushed, so the pill's fill does not swallow it.
	 * Half the item's own size is enough for the deepest thing an item model can be - the box
	 * is a unit cube centred on the origin - and that is what an isometric block item like a
	 * cake needs, since it sticks out both ways from its own centre. The extra centimetre is
	 * for depth-buffer slop at range.
	 */
	private static final float ICON_LIFT = ICON / 2 + 0.01f;

	/**
	 * How far the bottom edge of the pill hangs above the top of her hitbox. A plain gap:
	 * she hovers, so there is nothing for the bubble to sit on, and it is measured from her
	 * hitbox rather than from her model because the model is not what the bubble belongs to.
	 */
	private static final float LIFT = 0.25f;

	/**
	 * How far off to one side the bubble is put when it is not straight over her head. Small
	 * on purpose: a fairy is 0.4 blocks wide, and a bubble that had drifted a whole block off
	 * her head would stop reading as hers.
	 */
	private static final float SIDE_SHIFT = 0.3f;

	/**
	 * How long the bubble stays up: three seconds. Long enough to be read, short enough that
	 * the next one ten to twenty seconds later is a new thought rather than a flicker of the
	 * same one.
	 */
	private static final int SHOW_TICKS = 60;

	/**
	 * Past this the bubble is smaller than the item inside it. Sixty-four blocks is the range
	 * a name tag stops being drawn at, and there is nothing here that deserves to outlive a
	 * name tag.
	 */
	private static final double RANGE = 64;

	public FairyWantLayer(GeoRenderer<T> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel, @Nullable RenderType renderType,
					   MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, float partialTick,
					   int packedLight, int packedOverlay) {
		ItemStack want = entity.getWantBubble();
		if (want.isEmpty() || entity.isInvisible()) return;
		float age = entity.getWantBubbleAge(partialTick);
		if (age < 0 || age > SHOW_TICKS) return;
		Minecraft mc = Minecraft.getInstance();
		var dispatcher = mc.getEntityRenderDispatcher();
		if (dispatcher.distanceToSqr(entity) > RANGE * RANGE) return;
		poseStack.pushPose();
		poseStack.translate(0, entity.getBbHeight() + LIFT + PILL_H * SCALE / 2, 0);
		poseStack.mulPose(dispatcher.cameraOrientation());
		poseStack.translate(sideShift(entity.getWantPlacement()), 0, 0);
		drawPill(poseStack, bufferSource.getBuffer(BUBBLE));
		poseStack.translate((ICON_CX - PILL_W / 2) * SCALE, (PILL_H / 2 - ICON_CY) * SCALE, ICON_LIFT);
		poseStack.scale(ICON, ICON, ICON);
		// GUI context, so an item looks the way it does in a slot and a cake comes out
		// isometric rather than as its flat inventory sprite. Full brightness for the same
		// reason an item frame uses it: what she is asking for has to be legible at night,
		// and it is a picture of an item rather than an item sitting in the dark.
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
			case FairyEntity.WANT_LEFT -> -SIDE_SHIFT;
			case FairyEntity.WANT_RIGHT -> SIDE_SHIFT;
			default -> 0f;
		};
	}

	/**
	 * The pill, centred on the frame's origin: the sheet's top-left corner is the pill's own,
	 * so the four corners are the whole of it.
	 */
	private static void drawPill(PoseStack poseStack, VertexConsumer vc) {
		var pose = poseStack.last();
		float hw = PILL_W * SCALE / 2;
		float hh = PILL_H * SCALE / 2;
		// the texture's v runs downwards where the frame's +y runs up, so the sheet's top row
		// is the quad's top edge
		addVertex(vc, pose, -hw, -hh, 0, PILL_H / SHEET);
		addVertex(vc, pose, hw, -hh, PILL_W / SHEET, PILL_H / SHEET);
		addVertex(vc, pose, hw, hh, PILL_W / SHEET, 0);
		addVertex(vc, pose, -hw, hh, 0, 0);
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

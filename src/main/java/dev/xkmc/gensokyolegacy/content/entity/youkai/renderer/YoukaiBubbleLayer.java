package dev.xkmc.gensokyolegacy.content.entity.youkai.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.AbstractBubbleModule;
import dev.xkmc.gensokyolegacy.content.entity.behavior.module.IBubbleModule;
import dev.xkmc.gensokyolegacy.content.entity.youkai.facet.GeoYoukaiAnim;
import dev.xkmc.gensokyolegacy.content.entity.youkai.YoukaiEntity;
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
	private static final RenderType BUBBLE = RenderType.entityCutoutNoCull(TEXTURE);

	/**
	 * The sheet is a 256px square and the pill occupies its top-left corner, {@code 48 x 24}. Only the pill is
	 * drawn: the three marks the sheet offers under it want a tail joined to the bottom border exactly right,
	 * and a plain gap over her head reads as hers on its own.
	 */
	private static final float SHEET = 256f;
	private static final float PILL_W = 48f;
	private static final float PILL_H = 24f;

	/**
	 * Blocks per sprite pixel. Just under a block wide, which is name-plate scale: big enough to read the
	 * item, small enough not to be the thing the player is looking at.
	 */
	private static final float SCALE = 0.02f;

	/**
	 * The middle of the pill's inside, in sheet pixels, which runs {@code x 3-44, y 3-20} - so half a pixel off
	 * the pill's own centre. Where the item goes.
	 */
	private static final float ICON_CX = 23.5f;
	private static final float ICON_CY = 11.5f;

	/**
	 * The item, in sprite pixels. The pill is 18px tall inside, so 13 leaves it a 2.5px margin all round.
	 */
	private static final float ICON_PX = 13f;
	private static final float ICON = ICON_PX * SCALE;

	/**
	 * How far towards the camera the item is pushed, so the pill's fill does not swallow it. Half the item's
	 * own size clears the deepest thing an item model can be - the box is a unit cube centred on the origin -
	 * and the extra centimetre is depth-buffer slop at range.
	 */
	private static final float ICON_LIFT = ICON / 2 + 0.01f;

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
	private static final int SHOW_TICKS = 60;

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
		poseStack.translate(0, entity.getBbHeight() + LIFT + PILL_H * SCALE / 2, 0);
		poseStack.mulPose(dispatcher.cameraOrientation());
		poseStack.translate(sideShift(module.getWantPlacement()), 0, 0);
		drawPill(poseStack, bufferSource.getBuffer(BUBBLE));
		poseStack.translate((ICON_CX - PILL_W / 2) * SCALE, (PILL_H / 2 - ICON_CY) * SCALE, ICON_LIFT);
		poseStack.scale(ICON, ICON, ICON);
		// GUI context so a cake comes out isometric rather than as its flat inventory sprite, full brightness
		// for the same reason an item frame uses it: a picture of an item has to be legible at night
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
		var pose = poseStack.last();
		float hw = PILL_W * SCALE / 2;
		float hh = PILL_H * SCALE / 2;
		// the texture's v runs downwards where the frame's +y runs up, so the sheet's top row is the quad's
		// top edge
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
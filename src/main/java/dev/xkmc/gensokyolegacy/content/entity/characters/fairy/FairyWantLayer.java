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
 * The want bubble: the sprite from {@code textures/bubble/bubble.png} over her head, with
 * the item she is asking for - {@link FairyEntity#getWantBubble()} - sitting in the middle
 * of it. Registered on the two fairy renderers that carry a rig, see
 * {@code PlainFairyRenderer} and {@code CirnoRenderer}.
 * <p>
 * It is a {@link GeoRenderLayer} and not a world pass, because the pose a layer is handed
 * is already the one a name tag is drawn in. The body yaw is applied inside
 * {@code GeoEntityRenderer#actuallyRender}, in a frame it pushes and pops itself, so by
 * the time the layers run the pose carries the entity's position and nothing else - the
 * same place vanilla draws a name tag from. That makes billboarding one {@code mulPose}
 * against the camera orientation, and it also means the bubble is drawn with the entity:
 * it goes into the same buffers, in the same pass, at the same distance.
 * <p>
 * Everything is drawn in the frame that {@code mulPose} leaves behind - {@code +x} right,
 * {@code +y} up, {@code +z} at the camera, and one unit to the block - so the sprite is
 * placed by its bottom edge, which is the part that has to sit over her head, and the item
 * is shoved a little further along {@code +z} so the pill's fill does not swallow it.
 */
public class FairyWantLayer<T extends FairyEntity & GeoYoukaiAnim> extends GeoRenderLayer<T> {

	private static final ResourceLocation TEXTURE = GensokyoLegacy.loc("textures/bubble/bubble.png");
	/**
	 * {@code NoCull}: a flat sprite has no inside to hide, so there is nothing for
	 * back-face culling to protect - and a quad that came out wound the other way round
	 * would simply vanish if there were.
	 */
	private static final RenderType BUBBLE = RenderType.entityCutoutNoCull(TEXTURE);

	/**
	 * The sheet is a 256px square and the art occupies its top-left corner: the pill spans
	 * {@code x 0-47, y 0-23}, and three marks hang below it at {@code y 30-41}. All three
	 * are drawn as one sprite; they are what the artist drew under the pill.
	 */
	private static final float SHEET = 256f;
	private static final float SPRITE_W = 48f;
	private static final float SPRITE_H = 42f;
	private static final float U_MAX = SPRITE_W / SHEET;
	private static final float V_MAX = SPRITE_H / SHEET;

	/**
	 * Blocks per sprite pixel. The bubble comes out a little under a block wide, which is
	 * name-plate scale: big enough to read the item, small enough not to be the thing the
	 * player is looking at.
	 */
	private static final float SCALE = 0.02f;

	/**
	 * The middle of the pill, in sprite pixels - where the item goes. The pill's inside is
	 * {@code x 3-44, y 3-20}, so its centre is half a pixel off the sprite's own.
	 */
	private static final float PILL_CX = 23.5f;
	private static final float PILL_CY = 11.5f;

	/**
	 * The item, in sprite pixels. The pill is 18px tall inside, so 13 leaves it a 2.5px
	 * margin all round rather than pressing against the outline.
	 */
	private static final float ICON_PX = 13f;
	private static final float ICON = ICON_PX * SCALE;

	/**
	 * How far towards the camera the item is pushed, so the pill's fill does not swallow
	 * it. Half the item's own size is enough for the deepest thing an item model can be -
	 * the box is a unit cube centred on the origin - and that is what an isometric block
	 * item like a cake needs, since it sticks out both ways from its own centre. The extra
	 * centimetre is for depth-buffer slop at range.
	 */
	private static final float ICON_LIFT = ICON / 2 + 0.01f;

	/**
	 * How far the bottom of the sprite - the tips of the three marks - hangs above the top
	 * of her hitbox. Close, because the marks are what points at her.
	 */
	private static final float LIFT = 0.15f;

	/**
	 * How long the bubble stays up: three seconds. Long enough to be read, short enough
	 * that the next one ten to twenty seconds later is a new thought rather than a flicker
	 * of the same one.
	 */
	private static final int SHOW_TICKS = 60;

	/**
	 * Past this the bubble is smaller than the item inside it. Sixty-four blocks is the
	 * range a name tag stops being drawn at, and there is nothing here that deserves to
	 * outlive a name tag.
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
		poseStack.translate(0, entity.getBbHeight() + LIFT, 0);
		poseStack.mulPose(dispatcher.cameraOrientation());
		drawSprite(poseStack, bufferSource.getBuffer(BUBBLE));
		poseStack.translate((PILL_CX - SPRITE_W / 2) * SCALE, (SPRITE_H - PILL_CY) * SCALE, ICON_LIFT);
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
	 * The sprite, in the same frame: the texture's {@code v} runs downwards, so its top row
	 * is the quad's top edge and the art lands the right way up.
	 */
	private static void drawSprite(PoseStack poseStack, VertexConsumer vc) {
		var pose = poseStack.last();
		float hw = SPRITE_W * SCALE / 2;
		float top = SPRITE_H * SCALE;
		addVertex(vc, pose, -hw, 0, 0, V_MAX);
		addVertex(vc, pose, hw, 0, U_MAX, V_MAX);
		addVertex(vc, pose, hw, top, U_MAX, 0);
		addVertex(vc, pose, -hw, top, 0, 0);
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

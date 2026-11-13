package dev.xkmc.gensokyolegacy.content.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/**
 * The drooping band vanilla draws a lead with, shared by the glove and stray-pack links.
 */
public class RopeRenderer {

	/** Midpoint droop as a fraction of the horizontal span, so a vertical link hangs taut. */
	private static final double SAG = 0.15;

	/** Samples per block of span: a long link reads as a curve and a short one as a band. */
	private static final double SEGMENTS_PER_BLOCK = 2;

	/** Full band width in blocks, matching a vanilla lead. */
	private static final float WIDTH = 0.025F;

	/**
	 * Draws the band along a parabolic droop, with alternating sample brightness reading as twisted
	 * cord and the lightmap blended from one end to the other.
	 */
	public static void rope(PoseStack pose, VertexConsumer vc,
							double x0, double y0, double z0, double x1, double y1, double z1,
							int blockLight0, int skyLight0, int blockLight1, int skyLight1, int rgb) {
		// DyeColor#getTextColor packs rgb alone, so the band keeps full opacity.
		float red = (rgb >> 16 & 255) / 255F;
		float green = (rgb >> 8 & 255) / 255F;
		float blue = (rgb & 255) / 255F;
		double dx = x1 - x0;
		double dz = z1 - z0;
		double span = Mth.sqrt((float) (dx * dx + dz * dz));
		double sag = span * SAG;
		int steps = Mth.clamp((int) (span * SEGMENTS_PER_BLOCK) + 2, 4, 32);
		// Width is laid across the horizontal path with the rails offset vertically, so the band
		// shows from above as well as from the side; hence two passes with the rails swapped.
		double ox = span < 1.0E-5 ? 0 : dz / span * WIDTH / 2;
		double oz = span < 1.0E-5 ? 0 : dx / span * WIDTH / 2;
		PoseStack.Pose mat = pose.last();
		for (int pass = 0; pass < 2; pass++) {
			boolean reverse = pass == 1;
			for (int i = 0; i <= steps; i++) {
				int index = reverse ? steps - i : i;
				double t = (double) index / steps;
				int light = LightTexture.pack((int) Mth.lerp(t, blockLight0, blockLight1),
						(int) Mth.lerp(t, skyLight0, skyLight1));
				// 4t(1-t) is 0 at both ends and 1 at the midpoint: the parabola's own shape factor.
				double x = Mth.lerp(t, x0, x1);
				double y = Mth.lerp(t, y0, y1) - sag * 4 * t * (1 - t);
				double z = Mth.lerp(t, z0, z1);
				// Vanilla's shading: alternating samples, and the second pass over the same sample,
				// take opposite ends of the 0.7-1.0 range, which is what reads as a twist.
				float shade = index % 2 == (reverse ? 1 : 0) ? 0.7F : 1.0F;
				float up = reverse ? 0 : WIDTH;
				vc.addVertex(mat, (float) (x - ox), (float) (y + up), (float) (z + oz))
						.setColor(red * shade, green * shade, blue * shade, 1F).setLight(light);
				vc.addVertex(mat, (float) (x + ox), (float) (y + WIDTH - up), (float) (z - oz))
						.setColor(red * shade, green * shade, blue * shade, 1F).setLight(light);
			}
		}
	}

	/**
	 * The partial tick the entity renderer would use for this entity: a frozen entity is not
	 * interpolated, exactly as {@code LevelRenderer#renderEntity} decides it.
	 */
	public static float partialTick(DeltaTracker delta, TickRateManager tickRate, Entity entity) {
		return delta.getGameTimeDeltaPartialTick(!tickRate.isEntityFrozen(entity));
	}

	/**
	 * The position the entity is actually drawn at, interpolated the way the entity renderer
	 * interpolates it — using {@code getX()} alone would leave every link a tick behind.
	 */
	public static Vec3 renderPos(Entity entity, float partialTick) {
		return new Vec3(Mth.lerp(partialTick, entity.xOld, entity.getX()),
				Mth.lerp(partialTick, entity.yOld, entity.getY()),
				Mth.lerp(partialTick, entity.zOld, entity.getZ()));
	}

	/**
	 * Block light where the entity's eyes are, matching {@code EntityRenderer#getBlockLightLevel} —
	 * including its burning-entity case, so a link to a doll on fire lights up the same way.
	 */
	public static int blockLight(Entity entity, float partialTick) {
		if (entity.isOnFire()) return 15;
		return entity.level().getBrightness(LightLayer.BLOCK, eyePos(entity, partialTick));
	}

	public static int skyLight(Entity entity, float partialTick) {
		return entity.level().getBrightness(LightLayer.SKY, eyePos(entity, partialTick));
	}

	private static BlockPos eyePos(Entity entity, float partialTick) {
		return BlockPos.containing(entity.getEyePosition(partialTick));
	}

}

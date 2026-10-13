package dev.xkmc.gensokyolegacy.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.glove.client.GloveDollHover;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class PlayerRenderEvent {

	/**
	 * How far a link droops at its midpoint, as a fraction of the horizontal span between its ends:
	 * the rope look. Measured against the horizontal span alone, so a doll the player stands on top
	 * of hangs taut instead of bowing out below the lower end.
	 */
	private static final double SAG = 0.15;

	/** Samples per block of span, clamped, so a long link reads as a curve and a short one as a band. */
	private static final double SEGMENTS_PER_BLOCK = 2;

	/** The leash band's full width in blocks, matching the width vanilla draws a lead at. */
	private static final float LEASH_WIDTH = 0.025F;

	/**
	 * A level stage rather than {@code RenderPlayerEvent}: the local player is not rendered at all
	 * in first person — the camera entity is filtered out of the entity pass — so an entity-render
	 * hook would draw nothing in the view the player actually plays from.
	 */
	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
		Player player = Minecraft.getInstance().player;
		if (player == null || !GloveDollHover.gloveInHand(player)) return;
		List<DollEntity> dolls = getControlledDolls(player);
		if (dolls.isEmpty()) return;
		renderLinks(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(),
				event.getCamera().getPosition(), player, dolls, event.getPartialTick());
	}

	/**
	 * One drooping link from the player's chest to the chest of every given doll, each drawn as a
	 * leash band.
	 * <p>
	 * {@code pose} is whatever frame the caller draws in and {@code origin} is the world position
	 * that frame's origin sits at — the player's own position in the entity-render pass, the camera
	 * in a level-stage pass — so a world position becomes local to the frame by subtracting it.
	 */
	public static void renderLinks(PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 origin,
	                               Player player, List<DollEntity> dolls, DeltaTracker delta) {
		TickRateManager tickRate = player.level().tickRateManager();
		float fromPartial = partialTick(delta, tickRate, player);
		Vec3 from = renderPos(player, fromPartial);
		double x0 = from.x - origin.x;
		double y0 = from.y + player.getBbHeight() * 0.5 - origin.y;
		double z0 = from.z - origin.z;
		RenderType leash = RenderType.leash();
		for (DollEntity doll : dolls) {
			float toPartial = partialTick(delta, tickRate, doll);
			Vec3 to = renderPos(doll, toPartial);
			// A connected-primitive type cannot consolidate consecutive geometry, so each getBuffer
			// opens a fresh strip: the links sharing this buffer are never welded into one ribbon.
			// The link takes the doll's own tint, the same DyeColor text color the doll item uses.
			leash(pose, buffers.getBuffer(leash),
					x0, y0, z0, to.x - origin.x, to.y + doll.getBbHeight() * 0.5 - origin.y, to.z - origin.z,
					blockLight(player, fromPartial), skyLight(player, fromPartial),
					blockLight(doll, toPartial), skyLight(doll, toPartial),
					doll.getColor().getTextColor());
		}
	}

	/**
	 * Block light where the entity's eyes are, matching {@code EntityRenderer#getBlockLightLevel} —
	 * including its burning-entity case, so a link to a doll on fire lights up the same way.
	 */
	private static int blockLight(Entity entity, float partialTick) {
		if (entity.isOnFire()) return 15;
		return entity.level().getBrightness(LightLayer.BLOCK, eyePos(entity, partialTick));
	}

	private static int skyLight(Entity entity, float partialTick) {
		return entity.level().getBrightness(LightLayer.SKY, eyePos(entity, partialTick));
	}

	private static BlockPos eyePos(Entity entity, float partialTick) {
		return BlockPos.containing(entity.getEyePosition(partialTick));
	}

	/**
	 * The partial tick the entity renderer would use for this entity: a frozen entity is not
	 * interpolated, exactly as {@code LevelRenderer#renderEntity} decides it.
	 */
	private static float partialTick(DeltaTracker delta, TickRateManager tickRate, Entity entity) {
		return delta.getGameTimeDeltaPartialTick(!tickRate.isEntityFrozen(entity));
	}

	/**
	 * The position the entity is actually drawn at, interpolated the way the entity renderer
	 * interpolates it — using {@code getX()} alone would leave every link a tick behind.
	 */
	private static Vec3 renderPos(Entity entity, float partialTick) {
		return new Vec3(Mth.lerp(partialTick, entity.xOld, entity.getX()),
				Mth.lerp(partialTick, entity.yOld, entity.getY()),
				Mth.lerp(partialTick, entity.zOld, entity.getZ()));
	}

	/**
	 * A rope between two points, drawn the way vanilla draws a lead: a narrow band bent along the
	 * path with two vertices per sample, alternating brightness so it reads as twisted cord, and a
	 * lightmap blended from one end's light to the other's.
	 * <p>
	 * The shape is the straight span plus a parabolic droop, the cheap stand-in for a catenary that
	 * reads the same at these lengths — vanilla instead bends the height profile alone, which is the
	 * same idea one degree weaker.
	 * <p>
	 * The band's width is laid out across the path horizontally <em>and</em> its two rails are offset
	 * vertically, which is what keeps it visible from above as well as from the side instead of
	 * thinning to an edge-on sliver. That is also why the path is walked twice with the rails
	 * swapped: the two passes give the band a cross-section rather than a single flat face.
	 * <p>
	 * {@code rgb} is the link's 0xRRGGBB color; the band is always drawn opaque, scaled by the twist
	 * shading the way vanilla shades a lead.
	 */
	private static void leash(PoseStack pose, VertexConsumer vc,
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
		// Half the band's width, resolved across the path's horizontal direction. A path with no
		// horizontal run is a vertical drop, which has no cross direction to splay the band over.
		double ox = span < 1.0E-5 ? 0 : dz / span * LEASH_WIDTH / 2;
		double oz = span < 1.0E-5 ? 0 : dx / span * LEASH_WIDTH / 2;
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
				float up = reverse ? 0 : LEASH_WIDTH;
				vc.addVertex(mat, (float) (x - ox), (float) (y + up), (float) (z + oz))
						.setColor(red * shade, green * shade, blue * shade, 1F).setLight(light);
				vc.addVertex(mat, (float) (x + ox), (float) (y + LEASH_WIDTH - up), (float) (z - oz))
						.setColor(red * shade, green * shade, blue * shade, 1F).setLight(light);
			}
		}
	}

	/**
	 * The dolls the given player currently controls, in ledger order.
	 * <p>
	 * Client-side this is the owner-synced roster ({@code DollRosterToClient}) resolved against
	 * the client level, re-checking each entry's id/uuid pair so a recycled entity id never
	 * resolves to the wrong doll — the same resolution {@code DollGloveClientModes} and the
	 * sidebar use. Only the doll owner is sent the roster, so any other player yields an empty
	 * list.
	 */
	public static List<DollEntity> getControlledDolls(Player player) {
		Level level = Minecraft.getInstance().level;
		if (level == null) return List.of();
		@Nullable DollAttachment att = GLMeta.DOLL.type().getExisting(player).orElse(null);
		if (att == null) return List.of();
		List<DollEntity> out = new ArrayList<>();
		for (var entry : att.getRoster()) {
			if (!(level.getEntity(entry.id()) instanceof DollEntity doll)) continue;
			if (!doll.getUUID().equals(entry.uuid())) continue;
			out.add(doll);
		}
		return out;
	}

}

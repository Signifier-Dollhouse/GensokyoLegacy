package dev.xkmc.gensokyolegacy.event;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollAttachment;
import dev.xkmc.gensokyolegacy.content.client.render.RopeRenderer;
import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;
import dev.xkmc.gensokyolegacy.content.item.glove.client.GloveDollHover;
import dev.xkmc.gensokyolegacy.init.registrate.GLMeta;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
	 * A level stage rather than {@code RenderPlayerEvent}: the local player is not rendered at all in first person (the
	 * camera entity is filtered out of the entity pass), so an entity-render hook would draw nothing in the view the
	 * player actually plays from.
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
	 * One drooping link from the player's chest to the chest of every given doll, each drawn as a leash band. <p>
	 * {@code pose} is whatever frame the caller draws in and {@code origin} the world position that frame sits at, so a
	 * world position becomes local by subtracting it.
	 */
	public static void renderLinks(PoseStack pose, MultiBufferSource.BufferSource buffers, Vec3 origin,
	                               Player player, List<DollEntity> dolls, DeltaTracker delta) {
		TickRateManager tickRate = player.level().tickRateManager();
		float fromPartial = RopeRenderer.partialTick(delta, tickRate, player);
		Vec3 from = RopeRenderer.renderPos(player, fromPartial);
		double x0 = from.x - origin.x;
		double y0 = from.y + player.getBbHeight() * 0.5 - origin.y;
		double z0 = from.z - origin.z;
		RenderType leash = RenderType.leash();
		for (DollEntity doll : dolls) {
			float toPartial = RopeRenderer.partialTick(delta, tickRate, doll);
			Vec3 to = RopeRenderer.renderPos(doll, toPartial);
			// A connected-primitive type cannot consolidate consecutive geometry, so each getBuffer opens a
			// fresh strip and the links sharing it are never welded into one ribbon. Tint is the doll's DyeColor.
			RopeRenderer.rope(pose, buffers.getBuffer(leash),
					x0, y0, z0, to.x - origin.x, to.y + doll.getBbHeight() * 0.5 - origin.y, to.z - origin.z,
					RopeRenderer.blockLight(player, fromPartial), RopeRenderer.skyLight(player, fromPartial),
					RopeRenderer.blockLight(doll, toPartial), RopeRenderer.skyLight(doll, toPartial),
					doll.getColor().getTextColor());
		}
	}

	/**
	 * The dolls the given player currently controls, in ledger order: the owner-synced roster resolved against the
	 * client level, re-checking each id/uuid pair so a recycled entity id never picks the wrong doll. Only the owner is
	 * sent the roster, so any other player yields an empty list.
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

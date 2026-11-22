package dev.xkmc.gensokyolegacy.content.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.gensokyolegacy.content.entity.dolls.StrayDollEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Draws the thread from a stray pack's leader to each follower. The client only has the synced leader
 * UUID, so this pass gathers the loaded strays into a map before linking them.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class StrayDollLinkRenderer {

	private static final int LINK_COLOR = DyeColor.PINK.getTextColor();

	@SubscribeEvent
	public static void onRenderLevelStage(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return;
		Map<UUID, StrayDollEntity> strays = new HashMap<>();
		for (Entity entity : level.entitiesForRendering())
			if (entity instanceof StrayDollEntity doll)
				strays.put(doll.getUUID(), doll);
		if (strays.size() < 2) return;

		PoseStack pose = event.getPoseStack();
		Vec3 origin = event.getCamera().getPosition();
		TickRateManager tickRate = level.tickRateManager();
		MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
		for (StrayDollEntity follower : strays.values()) {
			UUID leaderId = follower.getLeaderUUID();
			if (leaderId == null || !follower.isAlive()) continue;
			StrayDollEntity leader = strays.get(leaderId);
			if (leader == null || leader == follower || !leader.isAlive()) continue;

			float leaderPartial = RopeRenderer.partialTick(event.getPartialTick(), tickRate, leader);
			float followerPartial = RopeRenderer.partialTick(event.getPartialTick(), tickRate, follower);
			Vec3 from = RopeRenderer.renderPos(leader, leaderPartial);
			Vec3 to = RopeRenderer.renderPos(follower, followerPartial);
			// A connected-primitive type cannot consolidate consecutive geometry, so each getBuffer
			// opens a fresh strip rather than welding the pack's links into one ribbon.
			RopeRenderer.rope(pose, buffers.getBuffer(RenderType.leash()),
					from.x - origin.x, from.y + leader.getBbHeight() * 0.5 - origin.y, from.z - origin.z,
					to.x - origin.x, to.y + follower.getBbHeight() * 0.5 - origin.y, to.z - origin.z,
					RopeRenderer.blockLight(leader, leaderPartial), RopeRenderer.skyLight(leader, leaderPartial),
					RopeRenderer.blockLight(follower, followerPartial), RopeRenderer.skyLight(follower, followerPartial),
					LINK_COLOR);
		}
	}

}

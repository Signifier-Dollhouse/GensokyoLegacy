package dev.xkmc.gensokyolegacy.content.attachment.area;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AreaEffectRenderer {

	private static final float TILE = 16F;

	public static void onRenderLevel(Level level, RenderLevelStageEvent event) {
		Frustum frustum = event.getFrustum();
		Map<ResourceLocation, List<RegionBorderVisual>> passes = new LinkedHashMap<>();
		for (AreaEffectEntry entry : ClientAreaEffectTracker.getTracked()) {
			for (AreaEffectVisual visual : entry.data.getClientVisual()) {
				passes.computeIfAbsent(visual.texture(), k -> new ArrayList<>())
						.add(new RegionBorderVisual(entry.range, visual));
			}
		}
		if (passes.isEmpty()) return;
		Vec3 cam = event.getCamera().getPosition();
		float time = (level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(true)) / 20F;
		int minY = level.getMinBuildHeight();
		int maxY = level.getMaxBuildHeight();
		for (Map.Entry<ResourceLocation, List<RegionBorderVisual>> pass : passes.entrySet()) {
			drawPass(pass.getKey(), pass.getValue(), frustum, cam, minY, maxY, time);
		}
	}

	private static void drawPass(ResourceLocation texture, List<RegionBorderVisual> regions,
	                             Frustum frustum, Vec3 cam, int minY, int maxY, float time) {
		List<RegionBorderVisual> visible = new ArrayList<>();
		for (RegionBorderVisual rv : regions) {
			ChunkPosRange range = rv.range();
			AABB box = new AABB(range.minCX() * 16, minY, range.minCZ() * 16,
					range.maxCX() * 16 + 16, maxY, range.maxCZ() * 16 + 16);
			if (frustum.isVisible(box)) visible.add(rv);
		}
		if (visible.isEmpty()) return;
		BufferBuilder builder = Tesselator.getInstance().begin(
				VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (RegionBorderVisual rv : visible) {
			ChunkPosRange r = rv.range();
			addRegion(builder, r.minCX() * 16, r.maxCX() * 16 + 16, r.minCZ() * 16, r.maxCZ() * 16 + 16,
					minY, maxY, cam, rv.visual(), time);
		}
		MeshData mesh = builder.buildOrThrow();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		BufferUploader.drawWithShader(mesh);
		RenderSystem.enableCull();
	}

	private static void addRegion(VertexConsumer consumer, int minX, int maxX, int minZ, int maxZ,
	                              int minY, int maxY, Vec3 cam, AreaEffectVisual v, float time) {
		float camX = (float) cam.x, camY = (float) cam.y, camZ = (float) cam.z;
		float scroll = v.speed() * time;
		if (v.walls()) {
			addZWall(consumer, minX, maxX, minZ, minY, maxY, camX, camY, camZ, v, scroll, true);
			addZWall(consumer, minX, maxX, maxZ, minY, maxY, camX, camY, camZ, v, scroll, false);
			addXWall(consumer, minZ, maxZ, minX, minY, maxY, camX, camY, camZ, v, scroll, true);
			addXWall(consumer, minZ, maxZ, maxX, minY, maxY, camX, camY, camZ, v, scroll, false);
		}
		if (v.top()) {
			addHWall(consumer, minX, maxX, minZ, maxZ, maxY, camX, camY, camZ, v);
		}
		if (v.bottom()) {
			addHWall(consumer, minX, maxX, minZ, maxZ, minY, camX, camY, camZ, v);
		}
	}

	private static void addZWall(VertexConsumer consumer, int x0, int x1, int z, int y0, int y1,
	                             float camX, float camY, float camZ, AreaEffectVisual v, float scroll, boolean flipU) {
		float tx = (x1 - x0) / TILE;
		float ty = (y1 - y0) / TILE;
		if (flipU) {
			vertex(consumer, x0 - camX, y0 - camY, z - camZ, 0, ty - scroll, v);
			vertex(consumer, x1 - camX, y0 - camY, z - camZ, tx, ty - scroll, v);
			vertex(consumer, x1 - camX, y1 - camY, z - camZ, tx, -scroll, v);
			vertex(consumer, x0 - camX, y1 - camY, z - camZ, 0, -scroll, v);
		} else {
			vertex(consumer, x0 - camX, y0 - camY, z - camZ, tx, ty - scroll, v);
			vertex(consumer, x1 - camX, y0 - camY, z - camZ, 0, ty - scroll, v);
			vertex(consumer, x1 - camX, y1 - camY, z - camZ, 0, -scroll, v);
			vertex(consumer, x0 - camX, y1 - camY, z - camZ, tx, -scroll, v);
		}
	}

	private static void addXWall(VertexConsumer consumer, int z0, int z1, int x, int y0, int y1,
	                             float camX, float camY, float camZ, AreaEffectVisual v, float scroll, boolean flipU) {
		float tz = (z1 - z0) / TILE;
		float ty = (y1 - y0) / TILE;
		if (!flipU) {
			vertex(consumer, x - camX, y0 - camY, z0 - camZ, 0, ty - scroll, v);
			vertex(consumer, x - camX, y0 - camY, z1 - camZ, tz, ty - scroll, v);
			vertex(consumer, x - camX, y1 - camY, z1 - camZ, tz, -scroll, v);
			vertex(consumer, x - camX, y1 - camY, z0 - camZ, 0, -scroll, v);
		} else {
			vertex(consumer, x - camX, y0 - camY, z0 - camZ, tz, ty - scroll, v);
			vertex(consumer, x - camX, y0 - camY, z1 - camZ, 0, ty - scroll, v);
			vertex(consumer, x - camX, y1 - camY, z1 - camZ, 0, -scroll, v);
			vertex(consumer, x - camX, y1 - camY, z0 - camZ, tz, -scroll, v);
		}
	}

	private static void addHWall(VertexConsumer consumer, int x0, int x1, int z0, int z1, int y,
	                             float camX, float camY, float camZ, AreaEffectVisual v) {
		float tx = (x1 - x0) / TILE;
		float tz = (z1 - z0) / TILE;
		vertex(consumer, x0 - camX, y - camY, z0 - camZ, 0, tz, v);
		vertex(consumer, x1 - camX, y - camY, z0 - camZ, tx, tz, v);
		vertex(consumer, x1 - camX, y - camY, z1 - camZ, tx, 0, v);
		vertex(consumer, x0 - camX, y - camY, z1 - camZ, 0, 0, v);
	}

	private static void vertex(VertexConsumer consumer, float x, float y, float z, float u, float v,
	                           AreaEffectVisual vis) {
		consumer.addVertex(x, y, z).setColor(vis.r(), vis.g(), vis.b(), vis.a()).setUv(u, v);
	}
}
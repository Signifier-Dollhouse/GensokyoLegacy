package dev.xkmc.gensokyolegacy.content.block.functional.barriers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xkmc.gensokyolegacy.content.attachment.area.AreaEffectEntry;
import dev.xkmc.gensokyolegacy.content.attachment.area.ChunkPosRange;
import dev.xkmc.gensokyolegacy.content.attachment.area.ClientAreaEffectTracker;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;

@EventBusSubscriber(modid = GensokyoLegacy.MODID, value = Dist.CLIENT)
public class ClientSealingPotRenderer {

	private static final ResourceLocation TEX = GensokyoLegacy.loc("textures/barriers/sealing_pot.png");
	private static final float ALPHA = 0.35F;
	private static final float TILE = 16F;

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
		List<AreaEffectEntry> seals = ClientAreaEffectTracker.getTracked().stream()
				.filter(e -> e.data instanceof SealingEffectData)
				.toList();
		if (seals.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		renderSeals(event, mc, seals);
	}

	private static void renderSeals(RenderLevelStageEvent event, Minecraft mc, List<AreaEffectEntry> seals) {
		Camera camera = event.getCamera();
		Vec3 cam = camera.getPosition();
		int minY = mc.level.getMinBuildHeight();
		int maxY = mc.level.getMaxBuildHeight();
		BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (AreaEffectEntry entry : seals) {
			addRegion(builder, entry.range, cam, minY, maxY, event.getFrustum());
		}
		MeshData mesh = builder.buildOrThrow();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, TEX);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		BufferUploader.drawWithShader(mesh);
		RenderSystem.enableCull();
	}

	private static void addRegion(VertexConsumer consumer, ChunkPosRange range, Vec3 cam, int minY, int maxY, Frustum frustum) {
		int minX = range.minCX() * 16;
		int maxX = range.maxCX() * 16 + 16;
		int minZ = range.minCZ() * 16;
		int maxZ = range.maxCZ() * 16 + 16;
		if (!frustum.isVisible(new AABB(minX, minY, minZ, maxX, maxY, maxZ))) return;
		float camX = (float) cam.x, camY = (float) cam.y, camZ = (float) cam.z;
		addZWall(consumer, minX, maxX, minZ, minY, maxY, camX, camY, camZ);
		addZWall(consumer, minX, maxX, maxZ, minY, maxY, camX, camY, camZ);
		addXWall(consumer, minZ, maxZ, minX, minY, maxY, camX, camY, camZ);
		addXWall(consumer, minZ, maxZ, maxX, minY, maxY, camX, camY, camZ);
	}

	private static void addZWall(VertexConsumer consumer, int x0, int x1, int z, int y0, int y1,
			float camX, float camY, float camZ) {
		float tx = (x1 - x0) / TILE;
		float ty = (y1 - y0) / TILE;
		vertex(consumer, x0 - camX, y0 - camY, z - camZ, 0, ty);
		vertex(consumer, x1 - camX, y0 - camY, z - camZ, tx, ty);
		vertex(consumer, x1 - camX, y1 - camY, z - camZ, tx, 0);
		vertex(consumer, x0 - camX, y1 - camY, z - camZ, 0, 0);
	}

	private static void addXWall(VertexConsumer consumer, int z0, int z1, int x, int y0, int y1,
			float camX, float camY, float camZ) {
		float tz = (z1 - z0) / TILE;
		float ty = (y1 - y0) / TILE;
		vertex(consumer, x - camX, y0 - camY, z0 - camZ, 0, ty);
		vertex(consumer, x - camX, y0 - camY, z1 - camZ, tz, ty);
		vertex(consumer, x - camX, y1 - camY, z1 - camZ, tz, 0);
		vertex(consumer, x - camX, y1 - camY, z0 - camZ, 0, 0);
	}

	private static void vertex(VertexConsumer consumer, float x, float y, float z, float u, float v) {
		consumer.addVertex(x, y, z).setColor(1, 1, 1, ALPHA).setUv(u, v);
	}
}
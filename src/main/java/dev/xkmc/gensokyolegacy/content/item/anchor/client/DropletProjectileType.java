package dev.xkmc.gensokyolegacy.content.item.anchor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xkmc.danmakuapi.content.render.BulkDataWriter;
import dev.xkmc.danmakuapi.content.render.DanmakuRenderStates;
import dev.xkmc.danmakuapi.content.render.DisplayType;
import dev.xkmc.danmakuapi.content.render.RenderableDanmakuType;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

/**
 * Camera-facing droplet that still leans along its flight: the sprite is billboarded like a plain
 * ball, then spun in the screen plane so its tip points along the motion projected onto the view.
 *
 * @see "doc/design/minamitsu_anchor.md"
 */
public record DropletProjectileType(ResourceLocation tex, DisplayType display)
		implements RenderableDanmakuType<DropletProjectileType, DropletProjectileType.Ins> {

	@Override
	public void start(MultiBufferSource buffer, List<Ins> list) {
		BulkDataWriter vc = new BulkDataWriter(buffer.getBuffer(DanmakuRenderStates.danmaku(tex, display())), list.size());
		for (var e : list) {
			e.tex(vc);
		}
		vc.flush();
	}

	@Override
	public void create(Consumer<Ins> holder, ProjectileRenderer<?> r, SimplifiedProjectile e,
					   PoseStack pose, float pTick) {
		Quaternionf cam = r.cameraOrientation();
		pose.mulPose(cam);
		float angle = screenAngle(cam, pose.last().pose(), e.getDeltaMovement());
		if (angle != 0) {
			pose.mulPose(Axis.ZP.rotation(angle + Mth.PI));
		}
		Matrix4f m4 = new Matrix4f(pose.last().pose());
		int col = DanmakuRenderStates.fading(display, -1, r, e);
		holder.accept(new Ins(m4, col));
	}

	/**
	 * The in-screen rotation that points the sprite's up edge along the motion, or 0 when the
	 * motion has no visible component (flying almost straight at or away from the camera).
	 */
	private static float screenAngle(Quaternionf cam, Matrix4f pose, Vec3 motion) {
		Vector3f view = new Vector3f(pose.m30(), pose.m31(), pose.m32());
		if (view.lengthSquared() < 1e-6f || motion.lengthSqr() < 1e-12) return 0;
		view.normalize();
		Vector3f move = new Vector3f((float) motion.x, (float) motion.y, (float) motion.z).normalize();
		// within ~2.5 degrees of the view axis the projection direction is noise
		if (Math.abs(move.dot(view)) > 0.999f) return 0;
		Vector3f perp = move.sub(new Vector3f(view).mul(move.dot(view)));
		Vector3f right = cam.transform(new Vector3f(1, 0, 0));
		Vector3f up = cam.transform(new Vector3f(0, 1, 0));
		return (float) Math.atan2(-perp.dot(right), perp.dot(up));
	}

	public record Ins(Matrix4f m4, int color) {

		public void tex(BulkDataWriter vc) {
			vertex(vc, m4, 1, 1, 1, 0, color);
			vertex(vc, m4, 1, 0, 1, 1, color);
			vertex(vc, m4, 0, 0, 0, 1, color);
			vertex(vc, m4, 0, 1, 0, 0, color);
		}

		private static void vertex(BulkDataWriter vc, Matrix4f m4, float x, int y, int u, int v, int color) {
			vc.addVertex(m4, x - 0.5F, y - 0.5F, 0.0F, u, v, color);
		}

	}

}

package dev.xkmc.gensokyolegacy.content.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A stationary lance impact, aligned to the weapon axis and extending forward from its tip, one tick per frame.
 */
public class SpearParticle extends TextureSheetParticle {

	private static final int FRAMES = 6;

	private final SpriteSet sprites;
	private final Vector3f direction;

	protected SpearParticle(ClientLevel level, double x, double y, double z,
							double directionX, double directionY, double directionZ, SpriteSet sprites) {
		super(level, x, y, z);
		this.sprites = sprites;
		this.direction = new Vector3f((float) directionX, (float) directionY, (float) directionZ);
		if (this.direction.lengthSquared() > 1e-6F) this.direction.normalize();
		this.lifetime = FRAMES;
		this.quadSize = 0.75F;
		this.setPos(x + this.direction.x * this.quadSize, y + this.direction.y * this.quadSize,
				z + this.direction.z * this.quadSize);
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		this.setSprite(sprites.get(0, FRAMES - 1));
		hasPhysics = false;
	}

	@Override
	public void tick() {
		if (++this.age >= this.lifetime) {
			this.remove();
			return;
		}
		// SpriteSet maps ages inclusively, so age 5 must select the sixth frame.
		this.setSprite(this.sprites.get(this.age, FRAMES - 1));
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTick) {
		if (direction.lengthSquared() <= 1e-6F) {
			super.render(buffer, camera, partialTick);
			return;
		}
		Vec3 cameraPos = camera.getPosition();
		Vector3f toCamera = new Vector3f((float) (cameraPos.x - x), (float) (cameraPos.y - y),
				(float) (cameraPos.z - z));
		Vector3f cameraUp = new Vector3f(0, 1, 0).rotate(camera.rotation());
		renderRotatedQuad(buffer, camera, rotationFor(direction, toCamera, cameraUp), partialTick);
	}

	/**
	 * 1.20.1 has no rotated-quad helper on {@code Particle}; this is the vanilla single-quad vertex
	 * loop with an explicit orientation instead of the camera rotation.
	 */
	private void renderRotatedQuad(VertexConsumer buffer, Camera camera, Quaternionf rot, float partialTick) {
		Vec3 cam = camera.getPosition();
		float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
		float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
		float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());
		Vector3f[] verts = new Vector3f[]{
				new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F),
				new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
		float size = this.getQuadSize(partialTick);
		for (Vector3f v : verts) {
			v.rotate(rot);
			v.mul(size);
			v.add(x, y, z);
		}
		float u0 = this.getU0(), u1 = this.getU1(), v0 = this.getV0(), v1 = this.getV1();
		int light = this.getLightColor(partialTick);
		buffer.vertex(verts[0].x(), verts[0].y(), verts[0].z()).uv(u1, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
		buffer.vertex(verts[1].x(), verts[1].y(), verts[1].z()).uv(u1, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
		buffer.vertex(verts[2].x(), verts[2].y(), verts[2].z()).uv(u0, v0).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
		buffer.vertex(verts[3].x(), verts[3].y(), verts[3].z()).uv(u0, v1).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
	}

	static Quaternionf rotationFor(Vector3f direction, Vector3f toCamera, Vector3f cameraUp) {
		Vector3f right = new Vector3f(direction).normalize().negate();
		Vector3f up = new Vector3f(toCamera).cross(right);
		if (up.lengthSquared() <= 1e-6F) {
			up.set(cameraUp).fma(-cameraUp.dot(right), right);
			if (up.lengthSquared() <= 1e-6F) {
				up.set(Math.abs(right.y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0));
				up.fma(-up.dot(right), right);
			}
		}
		up.normalize();
		Vector3f normal = new Vector3f(right).cross(up).normalize();
		return new Quaternionf().setFromNormalized(new Matrix3f().setColumn(0, right)
				.setColumn(1, up).setColumn(2, normal));
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public int getLightColor(float partialTick) {
		return LightTexture.FULL_BRIGHT;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {

		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
									   double x, double y, double z,
									   double xSpeed, double ySpeed, double zSpeed) {
			return new SpearParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
		}

	}

}

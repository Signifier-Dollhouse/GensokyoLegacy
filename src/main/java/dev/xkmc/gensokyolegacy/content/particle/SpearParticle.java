package dev.xkmc.gensokyolegacy.content.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A stationary lance impact, aligned to the weapon axis and showing each frame for one tick.
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
		this.setSprite(sprites.get(0, FRAMES - 1));
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

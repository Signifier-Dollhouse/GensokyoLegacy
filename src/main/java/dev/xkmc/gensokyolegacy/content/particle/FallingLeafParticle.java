package dev.xkmc.gensokyolegacy.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A petal or a leaf falling out of the canopy, drifting like the vanilla cherry petal. The sprites are
 * variants of the same petal, one of them is picked per particle.
 */
public class FallingLeafParticle extends TextureSheetParticle {

	private static final int LIFETIME = 300;

	private float rotSpeed;
	private final float particleRandom;
	private final float spinAcceleration;

	protected FallingLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z);
		this.setSprite(sprites.get(this.random));
		this.rotSpeed = (float) Math.toRadians(this.random.nextBoolean() ? -30.0 : 30.0);
		this.particleRandom = this.random.nextFloat();
		this.spinAcceleration = (float) Math.toRadians(this.random.nextBoolean() ? -5.0 : 5.0);
		this.lifetime = LIFETIME;
		this.gravity = 7.5E-4F;
		float size = this.random.nextBoolean() ? 0.05F : 0.075F;
		this.quadSize = size;
		this.setSize(size, size);
		this.friction = 1.0F;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		if (this.lifetime-- <= 0) {
			this.remove();
		}
		if (!this.removed) {
			float age = (float) (LIFETIME - this.lifetime);
			float t = Math.min(age / LIFETIME, 1.0F);
			double curve = 2.0 * Math.pow(t, 1.25);
			double dx = Math.cos(Math.toRadians(this.particleRandom * 60.0F)) * curve;
			double dz = Math.sin(Math.toRadians(this.particleRandom * 60.0F)) * curve;
			this.xd += dx * 0.0025F;
			this.zd += dz * 0.0025F;
			this.yd = this.yd - this.gravity;
			this.rotSpeed = this.rotSpeed + this.spinAcceleration / 20.0F;
			this.oRoll = this.roll;
			this.roll = this.roll + this.rotSpeed / 20.0F;
			this.move(this.xd, this.yd, this.zd);
			if (this.onGround || this.lifetime < LIFETIME - 1 && (this.xd == 0.0 || this.zd == 0.0)) {
				this.remove();
			}
			if (!this.removed) {
				this.xd = this.xd * this.friction;
				this.yd = this.yd * this.friction;
				this.zd = this.zd * this.friction;
			}
		}
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
			return new FallingLeafParticle(level, x, y, z, sprites);
		}

	}

}

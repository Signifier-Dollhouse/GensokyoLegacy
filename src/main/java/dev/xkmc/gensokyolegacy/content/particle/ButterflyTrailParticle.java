package dev.xkmc.gensokyolegacy.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The petal a butterfly danmaku trails: the cherry blossom drift, but short-lived and shrinking,
 * so the trail fades behind the bullet instead of falling like a canopy petal.
 */
public class ButterflyTrailParticle extends TextureSheetParticle {

	private static final int LIFETIME = 10;

	private final float particleRandom;
	private final float startSize;

	protected ButterflyTrailParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z);
		this.setSprite(sprites.get(this.random));
		this.particleRandom = this.random.nextFloat();
		this.lifetime = LIFETIME;
		this.startSize = this.random.nextBoolean() ? 0.05F : 0.075F;
		this.quadSize = this.startSize;
		this.setSize(this.startSize, this.startSize);
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
		this.quadSize = this.startSize * ((float) this.lifetime / LIFETIME);
		if (this.lifetime-- <= 0) {
			this.remove();
			return;
		}
		float t = 1.0F - (float) this.lifetime / LIFETIME;
		double curve = 2.0 * Math.pow(t, 1.25);
		double dx = Math.cos(Math.toRadians(this.particleRandom * 60.0F)) * curve;
		double dz = Math.sin(Math.toRadians(this.particleRandom * 60.0F)) * curve;
		this.xd += dx * 0.0025F;
		this.zd += dz * 0.0025F;
		this.yd -= 7.5E-4F;
		this.move(this.xd, this.yd, this.zd);
		this.xd *= this.friction;
		this.yd *= this.friction;
		this.zd *= this.friction;
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
			return new ButterflyTrailParticle(level, x, y, z, sprites);
		}

	}

}

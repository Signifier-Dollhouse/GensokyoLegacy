package dev.xkmc.gensokyolegacy.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class MiasmaParticle extends TextureSheetParticle {

	private final SpriteSet sprites;

	protected MiasmaParticle(ClientLevel level, double x, double y, double z,
							 double xd, double yd, double zd, SpriteSet sprites) {
		this(level, x, y, z, xd, yd, zd, sprites, 0.2F);
	}

	protected MiasmaParticle(ClientLevel level, double x, double y, double z,
							 double xd, double yd, double zd, SpriteSet sprites, float size) {
		super(level, x, y, z, xd, yd, zd);
		this.sprites = sprites;
		this.setSize(size, size);
		this.quadSize *= this.random.nextFloat() * 0.5F + 0.75F;
		this.lifetime = 25 + this.random.nextInt(20);
		this.friction = 0.96F;
		this.setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites);
		if (this.age > this.lifetime / 2) {
			this.setAlpha(1.0F - ((float) this.age - (float) (this.lifetime / 2)) / (float) this.lifetime);
		}
	}

	@Override
	public void move(double x, double y, double z) {
		this.setBoundingBox(this.getBoundingBox().move(x, y, z));
		this.setLocationFromBoundingbox();
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public int getLightColor(float partialTick) {
		return 15728880;
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
			return create(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, 0.2F);
		}

	}

	public static class SmallProvider implements ParticleProvider<SimpleParticleType> {

		private final SpriteSet sprites;

		public SmallProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
									   double x, double y, double z,
									   double xSpeed, double ySpeed, double zSpeed) {
			return create(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, 0.12F);
		}

	}

	private static Particle create(ClientLevel level, double x, double y, double z,
								   double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites, float size) {
		double xd = xSpeed + (level.random.nextFloat() - 0.5F) * 0.03;
		double zd = zSpeed + (level.random.nextFloat() - 0.5F) * 0.03;
		return new MiasmaParticle(level, x, y, z, xd, ySpeed, zd, sprites, size);
	}

}

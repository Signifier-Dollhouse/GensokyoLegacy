package dev.xkmc.gensokyolegacy.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class SleepParticle extends TextureSheetParticle {

	protected SleepParticle(ClientLevel level, double x, double y, double z,
							double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
		super(level, x, y, z);
		pickSprite(sprites);
		xd = xSpeed;
		yd = ySpeed;
		zd = zSpeed;
		quadSize = 0.15F;
		lifetime = 40 + random.nextInt(10);
		friction = 1.0F;
		hasPhysics = false;
	}

	@Override
	public void tick() {
		super.tick();
		setAlpha(Math.min(1.0F, 2.0F * (1.0F - (float) age / lifetime)));
	}

	@Override
	public float getQuadSize(float partialTick) {
		return quadSize * (0.6F + 0.4F * (age + partialTick) / lifetime);
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
			return new SleepParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
		}

	}

}

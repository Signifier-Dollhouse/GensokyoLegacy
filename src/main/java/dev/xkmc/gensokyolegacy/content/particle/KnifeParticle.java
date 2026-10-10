package dev.xkmc.gensokyolegacy.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The knife flash: a short burst that runs through every frame of {@code particles/knife.json} and
 * is gone. Unlike {@link MiasmaParticle} it is not a drifting haze — it is an impact, so it holds
 * the spot it was spawned at and plays its frames instead of fading in place.
 * <p>
 * The frame count doubles as the lifetime, which makes the sprites land one per tick:
 * {@code SpriteSet#get(age, lifetime)} spreads the frames over the ages {@code 0..lifetime}
 * inclusive, so the last age is the one the particle is removed on. Nothing fades here, because the
 * frames already dim and shrink towards nothing on their own.
 */
public class KnifeParticle extends TextureSheetParticle {

	private static final int FRAMES = 8;

	private final SpriteSet sprites;

	protected KnifeParticle(ClientLevel level, double x, double y, double z,
							double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, xd, yd, zd);
		this.sprites = sprites;
		this.lifetime = FRAMES;
		this.friction = 0.96F;
		this.quadSize *= this.random.nextFloat() * 0.5F + 0.75F;
		this.setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites);
	}

	/**
	 * A flash that slid along the ground would read as a projectile, so collision is skipped and the
	 * burst only ever drifts the little its own speed gives it, as {@link MiasmaParticle} does.
	 */
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
			return new KnifeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
		}

	}

}

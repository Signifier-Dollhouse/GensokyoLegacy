package dev.xkmc.gensokyolegacy.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The knife flash: a short burst through every frame of {@code particles/knife.json}, gone. Unlike {@link
 * MiasmaParticle} it is an impact, not a drifting haze, so it holds its spawn spot. The frame count is the lifetime,
 * landing one sprite per tick; nothing fades, the frames dim and shrink on their own.
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

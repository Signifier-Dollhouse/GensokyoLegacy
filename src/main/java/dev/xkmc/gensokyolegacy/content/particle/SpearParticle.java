package dev.xkmc.gensokyolegacy.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/** A stationary lance impact, showing each of its six frames for one tick. */
public class SpearParticle extends TextureSheetParticle {

	private static final int FRAMES = 6;

	private final SpriteSet sprites;

	protected SpearParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
		super(level, x, y, z);
		this.sprites = sprites;
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
			return new SpearParticle(level, x, y, z, sprites);
		}

	}

}

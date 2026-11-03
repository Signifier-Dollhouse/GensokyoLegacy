package dev.xkmc.gensokyolegacy.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The knife flash: a short burst through one series of {@code particles/knife.json}, gone. Unlike {@link
 * MiasmaParticle} it is an impact, not a drifting haze, so it holds its spawn spot. The series length is the lifetime,
 * landing one sprite per tick; nothing fades, the frames dim and shrink on their own.
 * <p>
 * The sprite set holds two series of the same slash: {@code knife_01..04} are the dark one and {@code knife_05..08}
 * the light one. A flash draws its series when it is created and plays that series alone, so the order of the
 * textures in {@code knife.json} is what decides which half is which.
 */
public class KnifeParticle extends TextureSheetParticle {

	private static final int FRAMES = 4;
	private static final int SERIES = 2;
	private static final int SPRITES = FRAMES * SERIES;

	private final SpriteSet sprites;
	private final int series;

	protected KnifeParticle(ClientLevel level, double x, double y, double z,
							double xd, double yd, double zd, SpriteSet sprites) {
		super(level, x, y, z, xd, yd, zd);
		this.sprites = sprites;
		this.series = this.random.nextInt(SERIES);
		this.lifetime = FRAMES;
		this.friction = 0.96F;
		this.quadSize *= this.random.nextFloat() * 0.5F + 0.75F;
		this.setSprite(sprites.get(this.series * FRAMES, SPRITES - 1));
		hasPhysics = false;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.removed) return;
		// A max age of SPRITES - 1 makes SpriteSet index the list directly; the last frame holds until expiry.
		this.setSprite(this.sprites.get(this.series * FRAMES + Math.min(this.age, FRAMES - 1), SPRITES - 1));
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

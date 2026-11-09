package dev.xkmc.gensokyolegacy.compat.l2lib;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Client particle provider registration, deferred until the client event fires.
 */
public interface ParticleSupplier<T extends ParticleOptions> {

	static <T extends ParticleOptions> ParticleSupplier<T> provider(Supplier<ParticleProvider<T>> pvd) {
		return () -> (event, type) -> event.registerSpecial(type, pvd.get());
	}

	static <T extends ParticleOptions> ParticleSupplier<T> sprite(Supplier<ParticleProvider.Sprite<T>> pvd) {
		return () -> (event, type) -> event.registerSprite(type, pvd.get());
	}

	static <T extends ParticleOptions> ParticleSupplier<T> spriteSet(Supplier<ParticleEngine.SpriteParticleRegistration<T>> pvd) {
		return () -> (event, type) -> event.registerSpriteSet(type, pvd.get());
	}

	BiConsumer<RegisterParticleProvidersEvent, ParticleType<T>> register();

}

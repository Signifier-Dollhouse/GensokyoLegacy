package dev.xkmc.gensokyolegacy.compat.l2lib;

import com.tterrag.registrate.builders.NoConfigBuilder;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.xkmc.l2library.base.L2Registrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;

import java.util.function.Function;

/**
 * Registrate with the particle and config helpers the mod needs; everything else comes from
 * the legacy {@link L2Registrate}.
 */
public class GLRegistrate extends L2Registrate {

	public GLRegistrate(String modid) {
		super(modid);
	}

	public <T extends ParticleOptions, R extends ParticleType<T>> Val<R> particle(
			String name, NonNullSupplier<R> sup, NonNullSupplier<? extends ParticleSupplier<T>> pvd) {
		RegistryEntry<R> ans = entry(name, cb -> new NoConfigBuilder<>(this, this, name, cb,
				Registries.PARTICLE_TYPE, sup)).register();
		if (FMLEnvironment.dist == Dist.CLIENT) {
			OneTimeEventReceiver.addModListener(this, RegisterParticleProvidersEvent.class,
					event -> pvd.get().register().accept(event, ans.get()));
		}
		return new Val.Registrate<>(ans);
	}

	public <T extends ConfigInit> T registerUnsynced(Function<ConfigInit.Builder, T> factory) {
		return ConfigInit.register(this, ModConfig.Type.COMMON, factory);
	}

	public <T extends ConfigInit> T registerSynced(Function<ConfigInit.Builder, T> factory) {
		return ConfigInit.register(this, ModConfig.Type.SERVER, factory);
	}

}

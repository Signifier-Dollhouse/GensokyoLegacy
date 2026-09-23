package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.content.particle.MiasmaParticle;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate.ParticleSupplier;
import dev.xkmc.l2core.init.reg.simple.Val;
import net.minecraft.core.particles.SimpleParticleType;

public class GLParticles {

	public static final Val<SimpleParticleType> MIASMA = GensokyoLegacy.REGISTRATE.particle("miasma",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> MiasmaParticle.Provider::new));

	public static final Val<SimpleParticleType> MIASMA_SMALL = GensokyoLegacy.REGISTRATE.particle("miasma_small",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> MiasmaParticle.SmallProvider::new));

	public static void register() {

	}

}

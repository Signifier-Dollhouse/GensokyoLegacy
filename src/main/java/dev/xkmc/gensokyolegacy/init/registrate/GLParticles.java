package dev.xkmc.gensokyolegacy.init.registrate;

import dev.xkmc.gensokyolegacy.content.particle.FallingLeafParticle;
import dev.xkmc.gensokyolegacy.content.particle.KnifeParticle;
import dev.xkmc.gensokyolegacy.content.particle.MiasmaParticle;
import dev.xkmc.gensokyolegacy.content.particle.SpearParticle;
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

	public static final Val<SimpleParticleType> KNIFE = GensokyoLegacy.REGISTRATE.particle("knife",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> KnifeParticle.Provider::new));

	public static final Val<SimpleParticleType> SPEAR = GensokyoLegacy.REGISTRATE.particle("spear",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> SpearParticle.Provider::new));

	// the petals and the leaves that fall out of the canopy, one per species
	public static final Val<SimpleParticleType> PINK_CHERRY_BLOSSOM = GensokyoLegacy.REGISTRATE.particle("pink_cherry_blossom",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> FallingLeafParticle.Provider::new));

	public static final Val<SimpleParticleType> PURPLE_CHERRY_BLOSSOM = GensokyoLegacy.REGISTRATE.particle("purple_cherry_blossom",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> FallingLeafParticle.Provider::new));

	public static final Val<SimpleParticleType> ORANGE_MAPLE_LEAF = GensokyoLegacy.REGISTRATE.particle("orange_maple_leaf",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> FallingLeafParticle.Provider::new));

	public static final Val<SimpleParticleType> RED_MAPLE_LEAF = GensokyoLegacy.REGISTRATE.particle("red_maple_leaf",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> FallingLeafParticle.Provider::new));

	public static final Val<SimpleParticleType> YELLOW_MAPLE_LEAF = GensokyoLegacy.REGISTRATE.particle("yellow_maple_leaf",
			() -> new SimpleParticleType(false),
			() -> ParticleSupplier.spriteSet(() -> FallingLeafParticle.Provider::new));

	public static void register() {

	}

}

package dev.xkmc.gensokyolegacy.content.block.nature;

import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class MiasmaMushroomCapBlock extends WeightedMushroomBlock {

	public static final int MAX_INTENSITY = 3;

	public MiasmaMushroomCapBlock(ResourceKey<ConfiguredFeature<?, ?>> small,
	                              ResourceKey<ConfiguredFeature<?, ?>> medium,
	                              ResourceKey<ConfiguredFeature<?, ?>> large,
	                              BlockBehaviour.Properties props) {
		super(small, medium, large, props);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.randomTick(state, level, pos, random);
		MiasmaAirBlock.emitAround(level, pos, MAX_INTENSITY, random);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextFloat() < 0.1f) {
			double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
			double y = pos.getY() + 0.2 + random.nextDouble() * 0.6;
			double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
			var particle = GLParticles.MIASMA.get();
			level.addParticle(particle, x, y, z, 0, 0.03, 0);
		}
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (!level.isClientSide() && entity instanceof LivingEntity living) {
			if (living.hasEffect(GLEffects.MIASMA) && living.tickCount % 20 == 0) return;
			living.addEffect(new MobEffectInstance(GLEffects.MIASMA, 200, 0, true, false, true));
		}
	}


}

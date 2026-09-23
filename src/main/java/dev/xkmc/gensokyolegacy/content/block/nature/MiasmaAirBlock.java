package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import dev.xkmc.gensokyolegacy.init.registrate.GLEffects;
import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class MiasmaAirBlock extends AirBlock {

	public static final MapCodec<AirBlock> CODEC = simpleCodec(MiasmaAirBlock::new);

	public static final IntegerProperty INTENSITY = IntegerProperty.create("intensity", 1, 8);

	public MiasmaAirBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(INTENSITY, 8));
	}

	@Override
	public MapCodec<AirBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(INTENSITY);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int lvl = state.getValue(INTENSITY);
		trySpread(level, pos.below(), lvl, this, 1.0f, random);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			int next = lvl - 1;
			if (next >= 1) {
				trySpread(level, pos.relative(dir), next, this, 0.3f, random);
			}
		}
		int up = lvl - 2;
		if (up >= 1) {
			trySpread(level, pos.above(), up, this, 0.1f, random);
		}
	}

	public static void emitAround(ServerLevel level, BlockPos center, int max, RandomSource random) {
		var miasma = GLNaturalBlocks.MIASMA_AIR.get();
		trySpread(level, center.below(), max, miasma, 1.0f, random);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			trySpread(level, center.relative(dir), max, miasma, 0.3f, random);
		}
		trySpread(level, center.above(), max, miasma, 0.1f, random);
	}

	private static void trySpread(ServerLevel level, BlockPos target, int max, Block miasma, float chance, RandomSource random) {
		if (random.nextFloat() >= chance) {
			return;
		}
		var targetState = level.getBlockState(target);
		if (targetState.getBlock() instanceof MiasmaAirBlock) {
			int cur = targetState.getValue(INTENSITY);
			if (cur < max) {
				level.setBlock(target, targetState.setValue(INTENSITY, cur + 1), 3);
			}
		} else if (targetState.isAir()) {
			level.setBlock(target, miasma.defaultBlockState().setValue(INTENSITY, 1), 3);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		int lvl = state.getValue(INTENSITY);

		if (random.nextFloat() < 0.02f * Mth.clamp(lvl + 1, 1, 5)) {
			double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
			double y = pos.getY() + 0.2 + random.nextDouble() * 0.6;
			double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
			var particle = GLParticles.MIASMA_SMALL.get();
			level.addParticle(particle, x, y, z, 0, 0.03, 0);
		}

		if (random.nextFloat() < 0.04f * Mth.clamp(lvl - 4, 0, 4)) {
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
			int lvl = state.getValue(INTENSITY);
			int amplifier = lvl <= 4 ? 0 : 1;
			living.addEffect(new MobEffectInstance(GLEffects.MIASMA, 30, amplifier, true, false, true));
		}
	}

}

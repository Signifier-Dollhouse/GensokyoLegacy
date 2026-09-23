package dev.xkmc.gensokyolegacy.content.block.nature;

import dev.xkmc.gensokyolegacy.init.registrate.GLParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MiasmaMushroomHugeBlock extends HugeMushroomBlock {

	public static final int MAX_INTENSITY = 6;

	public MiasmaMushroomHugeBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		MiasmaAirBlock.emitAround(level, pos, MAX_INTENSITY, random);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextFloat() < 0.2f && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
			double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
			double y = pos.getY() + 1 + random.nextDouble() * 0.2;
			double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
			var particle = GLParticles.MIASMA.get();
			level.addParticle(particle, x, y, z, 0, 0.03, 0);
		}
	}

}

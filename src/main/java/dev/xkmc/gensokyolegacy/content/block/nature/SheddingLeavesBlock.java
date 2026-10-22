package dev.xkmc.gensokyolegacy.content.block.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Leaves that shed a petal or a leaf under the canopy and let a carpet of fallen leaves build up below.
 *
 * @see "doc/design/fallen_leaves.md"
 */
public class SheddingLeavesBlock extends LeavesBlock {

	private static final MapCodec<SheddingLeavesBlock> CODEC = simpleCodec(SheddingLeavesBlock::new);
	/**
	 * The petal or the leaf that falls out of the canopy.
	 */
	private final Supplier<ParticleOptions> particle;

	public SheddingLeavesBlock(Properties properties, Supplier<ParticleOptions> particle) {
		super(properties);
		this.particle = particle;
	}

	public SheddingLeavesBlock(Properties properties) {
		this(properties, () -> ParticleTypes.CHERRY_LEAVES);
	}

	@Override
	public MapCodec<? extends LeavesBlock> codec() {
		return CODEC;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		// every leaf of the tree sheds, not only the outermost ones, or the carpet would be a ring
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.randomTick(state, level, pos, random);
		if (!level.getBlockState(pos).is(this)) return;
		var kind = FallenLeafKind.of(this);
		if (kind != null && random.nextFloat() < 0.1F) {
			FallenLeavesBlock.accumulate(level, pos, kind, random);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		super.animateTick(state, level, pos, random);
		if (random.nextInt(10) == 0) {
			BlockPos below = pos.below();
			BlockState belowState = level.getBlockState(below);
			if (!isFaceFull(belowState.getCollisionShape(level, below), Direction.UP)) {
				ParticleUtils.spawnParticleBelow(level, pos, random, particle.get());
			}
		}
	}

}

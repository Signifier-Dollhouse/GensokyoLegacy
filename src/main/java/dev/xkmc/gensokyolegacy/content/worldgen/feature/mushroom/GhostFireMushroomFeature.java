package dev.xkmc.gensokyolegacy.content.worldgen.feature.mushroom;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Ghost fire mushroom: a short, tiered cap with a 5x5 base layer
 * topped by three 3x3 layers, tapering toward a point.
 */
public class GhostFireMushroomFeature extends AbstractHugeMushroomFeature {

	public GhostFireMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	protected int getTreeHeight(RandomSource random) {
		return 3 + random.nextInt(2);
	}

	@Override
	protected int getTreeRadiusForHeight(int i, int i1, int i2, int i3) {
		return 0;
	}

	@Override
	protected void makeCap(
			LevelAccessor level,
			RandomSource rand,
			BlockPos origin,
			int height,
			BlockPos.@NotNull MutableBlockPos pos,
			HugeMushroomFeatureConfiguration config
	) {
		int k = 0;
		for (int layer = 0; layer < 4; layer++) {
			int y = height - 1 + layer;
			int radius = layer == 0 ? 2 : 1;
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int d = Math.max(Math.abs(dx), Math.abs(dz));
					pos.setWithOffset(origin, dx, y, dz);
					if (!level.getBlockState(pos).isSolidRender(level, pos)) {
						BlockState state = config.capProvider.getState(rand, origin);
						if (state.hasProperty(HugeMushroomBlock.WEST)
								&& state.hasProperty(HugeMushroomBlock.EAST)
								&& state.hasProperty(HugeMushroomBlock.NORTH)
								&& state.hasProperty(HugeMushroomBlock.SOUTH)
								&& state.hasProperty(HugeMushroomBlock.UP)) {
							state = state.setValue(HugeMushroomBlock.UP, y >= height + 1 || layer == 0 && d == radius)
									.setValue(HugeMushroomBlock.WEST, dx < -k)
									.setValue(HugeMushroomBlock.EAST, dx > k)
									.setValue(HugeMushroomBlock.NORTH, dz < -k)
									.setValue(HugeMushroomBlock.SOUTH, dz > k);
						}
						this.setBlock(level, pos, state);
					}
				}
			}
		}
	}

}

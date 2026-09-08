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
 * Demonic miasma mushroom: a short 3-layer dome, one layer shorter than the vanilla red
 * huge mushroom cap.
 */
public class DemonicMiasmaMushroomFeature extends AbstractHugeMushroomFeature {

	public DemonicMiasmaMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	protected int getTreeRadiusForHeight(int i, int height, int foliageRadius, int y) {
		int radius = 0;
		if (y < height && y >= height - 2) {
			radius = foliageRadius;
		} else if (y == height) {
			radius = foliageRadius;
		}
		return radius;
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
		for (int y = height - 2; y <= height; y++) {
			int radius = y < height ? config.foliageRadius : config.foliageRadius - 1;
			int k = config.foliageRadius - 2;
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					boolean west = dx == -radius;
					boolean east = dx == radius;
					boolean north = dz == -radius;
					boolean south = dz == radius;
					boolean horizontal = west || east;
					boolean vertical = north || south;
					if (y >= height || horizontal != vertical) {
						pos.setWithOffset(origin, dx, y, dz);
						if (!level.getBlockState(pos).isSolidRender(level, pos)) {
							BlockState state = config.capProvider.getState(rand, origin);
							if (state.hasProperty(HugeMushroomBlock.WEST)
									&& state.hasProperty(HugeMushroomBlock.EAST)
									&& state.hasProperty(HugeMushroomBlock.NORTH)
									&& state.hasProperty(HugeMushroomBlock.SOUTH)
									&& state.hasProperty(HugeMushroomBlock.UP)) {
								state = state.setValue(HugeMushroomBlock.UP, y >= height - 1)
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

}

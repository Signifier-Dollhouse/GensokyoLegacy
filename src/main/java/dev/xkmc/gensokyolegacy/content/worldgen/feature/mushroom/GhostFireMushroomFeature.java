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
 * Ghost fire mushroom: a short, tiered cap that tapers toward a point.
 * Height is always 3-4, with three stacked cap layers: a 5x5 ring (hollow in the
 * middle ring corners removed), a corners-only 5x5, and a solid 5x5 top.
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
		for (int layer = 0; layer < 3; layer++) {
			int y = height - 1 + layer;
			for (int dx = -2; dx <= 2; dx++)
				for (int dz = -2; dz <= 2; dz++) {
					int d = Math.max(Math.abs(dx), Math.abs(dz));
					boolean place = switch (layer) {
						case 0 -> d >= 1;
						case 1 -> d == 2;
						default -> true;
					};
					if (!place) continue;
					pos.setWithOffset(origin, dx, y, dz);
					if (!level.getBlockState(pos).isSolidRender(level, pos)) {
						BlockState state = config.capProvider.getState(rand, origin);
						if (state.hasProperty(HugeMushroomBlock.DOWN)) {
							state = state.setValue(HugeMushroomBlock.DOWN, false);
						}
						this.setBlock(level, pos, state);
					}
				}
		}
	}

}

package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import com.mojang.serialization.Codec;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.GLNaturalBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

public class MushroomFeatures {

	public enum MushroomTreeType {
		GHOST_FIRE("ghost_fire_mushroom", GhostFireMushroomFeature::new,
				() -> GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET, null);

		public final String id;
		public final ResourceKey<ConfiguredFeature<?, ?>> cfKey;
		public final Function<Codec<HugeMushroomFeatureConfiguration>, AbstractHugeMushroomFeature> factory;
		public final Supplier<GLNaturalBlocks.MushroomSet> set;
		public final int radius;

		MushroomTreeType(String id,
		                 Function<Codec<HugeMushroomFeatureConfiguration>, AbstractHugeMushroomFeature> factory,
		                 Supplier<GLNaturalBlocks.MushroomSet> set, @Nullable Integer radius) {
			this.id = id;
			this.cfKey = ResourceKey.create(Registries.CONFIGURED_FEATURE, GensokyoLegacy.loc(id));
			this.factory = factory;
			this.set = set;
			this.radius = radius == null ? 0 : radius;
		}
	}

	public static class GhostFireMushroomFeature extends AbstractHugeMushroomFeature {

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
						if (!level.getBlockState(pos).isSolidRender(level, pos))
							this.setBlock(level, pos, config.capProvider.getState(rand, origin));
					}
			}
		}
	}


}

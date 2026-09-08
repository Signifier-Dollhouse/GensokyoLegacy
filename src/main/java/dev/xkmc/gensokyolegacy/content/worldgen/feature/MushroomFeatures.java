package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import com.mojang.serialization.Codec;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.registrate.block.GLNaturalBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
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
				() -> GLNaturalBlocks.GHOST_FIRE_MUSHROOM_SET, null),
		DREAM("dream_mushroom", DreamMushroomFeature::new,
				() -> GLNaturalBlocks.DREAM_MUSHROOM_SET, 2),
		DEMONIC_MIASMA("demonic_miasma_mushroom", DemonicMiasmaMushroomFeature::new,
				() -> GLNaturalBlocks.DEMONIC_MIASMA_MUSHROOM_SET, 2);

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

	public static class DreamMushroomFeature extends AbstractHugeMushroomFeature {

		public DreamMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		protected int getTreeRadiusForHeight(int i, int height, int foliageRadius, int y) {
			return y <= 3 ? 0 : foliageRadius;
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
			int radius = config.foliageRadius;
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					boolean west = dx == -radius;
					boolean east = dx == radius;
					boolean north = dz == -radius;
					boolean south = dz == radius;
					boolean onEdge = (west || east) && (north || south);
					if (onEdge) continue;
					pos.setWithOffset(origin, dx, height, dz);
					if (!level.getBlockState(pos).isSolidRender(level, pos)) {
						boolean hasWest = west || (north || south) && dx == 1 - radius;
						boolean hasEast = east || (north || south) && dx == radius - 1;
						boolean hasNorth = north || (west || east) && dz == 1 - radius;
						boolean hasSouth = south || (west || east) && dz == radius - 1;
						BlockState state = config.capProvider.getState(rand, origin);
						if (state.hasProperty(HugeMushroomBlock.WEST)) {
							state = state.setValue(HugeMushroomBlock.WEST, hasWest);
						}
						if (state.hasProperty(HugeMushroomBlock.EAST)) {
							state = state.setValue(HugeMushroomBlock.EAST, hasEast);
						}
						if (state.hasProperty(HugeMushroomBlock.NORTH)) {
							state = state.setValue(HugeMushroomBlock.NORTH, hasNorth);
						}
						if (state.hasProperty(HugeMushroomBlock.SOUTH)) {
							state = state.setValue(HugeMushroomBlock.SOUTH, hasSouth);
						}
						if (state.hasProperty(HugeMushroomBlock.DOWN)) {
							state = state.setValue(HugeMushroomBlock.DOWN, false);
						}
						this.setBlock(level, pos, state);
					}
				}
			}
		}
	}

	public static class DemonicMiasmaMushroomFeature extends AbstractHugeMushroomFeature {

		public DemonicMiasmaMushroomFeature(Codec<HugeMushroomFeatureConfiguration> codec) {
			super(codec);
		}

		@Override
		protected int getTreeRadiusForHeight(int i, int height, int foliageRadius, int y) {
			int radius = 0;
			if (y < height && y >= height - 3) {
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
			for (int y = height - 3; y <= height; y++) {
				int radius = y < height ? config.foliageRadius : config.foliageRadius - 1;
				int rim = config.foliageRadius - 2;
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
									state = state
											.setValue(HugeMushroomBlock.UP, y >= height - 1)
											.setValue(HugeMushroomBlock.WEST, dx < -rim)
											.setValue(HugeMushroomBlock.EAST, dx > rim)
											.setValue(HugeMushroomBlock.NORTH, dz < -rim)
											.setValue(HugeMushroomBlock.SOUTH, dz > rim);
								}
								if (state.hasProperty(HugeMushroomBlock.DOWN)) {
									state = state.setValue(HugeMushroomBlock.DOWN, false);
								}
								this.setBlock(level, pos, state);
							}
						}
					}
				}
			}
		}
	}

}

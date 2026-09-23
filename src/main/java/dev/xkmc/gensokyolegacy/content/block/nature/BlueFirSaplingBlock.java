package dev.xkmc.gensokyolegacy.content.block.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Blue fir sapling growing hand-built template trees: 1 sapling the medium tree,
 * 2x2 saplings the large tree (via the {@link TreeGrower} mega slot), 3x3 saplings
 * the giant tree. Vanilla has no 3x3 slot, so the giant is checked here first and
 * anything smaller falls back to the grower, mirroring vanilla mega handling.
 */
public class BlueFirSaplingBlock extends SaplingBlock {

	private final ResourceKey<ConfiguredFeature<?, ?>> giant;

	public BlueFirSaplingBlock(TreeGrower grower, ResourceKey<ConfiguredFeature<?, ?>> giant, BlockBehaviour.Properties props) {
		super(grower, props);
		this.giant = giant;
	}

	@Override
	public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		if (state.getValue(STAGE) == 0) {
			level.setBlock(pos, state.cycle(STAGE), 4);
		} else if (!growGiant(level, pos, state, random)) {
			treeGrower.growTree(level, level.getChunkSource().getGenerator(), pos, state, random);
		}
	}

	private boolean growGiant(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		Holder<ConfiguredFeature<?, ?>> holder = level.registryAccess()
				.registryOrThrow(Registries.CONFIGURED_FEATURE)
				.getHolder(giant).orElse(null);
		var event = EventHooks.fireBlockGrowFeature(level, random, pos, holder);
		holder = event.getFeature();
		if (event.isCanceled()) return true;
		if (holder == null) return false;
		Block block = state.getBlock();
		for (int i = 0; i >= -2; i--) {
			for (int j = 0; j >= -2; j--) {
				if (!isThreeByThree(block, level, pos, i, j)) continue;
				for (int dx = 0; dx < 3; dx++) {
					for (int dz = 0; dz < 3; dz++) {
						level.setBlock(pos.offset(i + dx, 0, j + dz), Blocks.AIR.defaultBlockState(), 4);
					}
				}
				if (holder.value().place(level, level.getChunkSource().getGenerator(), random, pos.offset(i + 1, 0, j + 1))) {
					return true;
				}
				for (int dx = 0; dx < 3; dx++) {
					for (int dz = 0; dz < 3; dz++) {
						level.setBlock(pos.offset(i + dx, 0, j + dz), state, 4);
					}
				}
				return true;
			}
		}
		return false;
	}

	private static boolean isThreeByThree(Block block, ServerLevel level, BlockPos pos, int xOffset, int zOffset) {
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 3; dz++) {
				if (!level.getBlockState(pos.offset(xOffset + dx, 0, zOffset + dz)).is(block)) return false;
			}
		}
		return true;
	}

}

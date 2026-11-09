package dev.xkmc.gensokyolegacy.content.block.nature;

import dev.xkmc.gensokyolegacy.content.worldgen.feature.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

/**
 * Grows a {@link TreeFeatures.TreeType} sapling: the mega tree on the usual 1-in-10 roll, the
 * ordinary one otherwise, mirroring vanilla's {@code TreeGrower} on 1.21.
 */
public class GLTreeGrower extends AbstractTreeGrower {

	private final TreeFeatures.TreeType type;

	public GLTreeGrower(TreeFeatures.TreeType type) {
		this.type = type;
	}

	@Nullable
	@Override
	protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
		var mega = type.saplingMega;
		if (mega.isPresent() && random.nextInt(10) == 0) return mega.get();
		return type.saplingTree.orElse(null);
	}

}

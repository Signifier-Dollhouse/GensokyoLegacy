package dev.xkmc.gensokyolegacy.content.block.nature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.neoforge.event.EventHooks;

import java.util.Optional;

/**
 * Mushroom cap growing the template tree of its own kind: 50% small, 40% medium,
 * 10% large. Vanilla {@link MushroomBlock} only supports a single feature, so the
 * size is rolled here and growth otherwise mirrors vanilla.
 */
public class WeightedMushroomBlock extends MushroomBlock {

	private static final float SMALL_CHANCE = 0.5F, MEDIUM_CHANCE = 0.4F;

	private final ResourceKey<ConfiguredFeature<?, ?>> small;
	private final ResourceKey<ConfiguredFeature<?, ?>> medium;
	private final ResourceKey<ConfiguredFeature<?, ?>> large;

	public WeightedMushroomBlock(ResourceKey<ConfiguredFeature<?, ?>> small,
	                             ResourceKey<ConfiguredFeature<?, ?>> medium,
	                             ResourceKey<ConfiguredFeature<?, ?>> large,
	                             BlockBehaviour.Properties props) {
		super(medium, props);
		this.small = small;
		this.medium = medium;
		this.large = large;
	}

	@Override
	public boolean growMushroom(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		float roll = random.nextFloat();
		ResourceKey<ConfiguredFeature<?, ?>> key = roll < SMALL_CHANCE ? small :
				roll < SMALL_CHANCE + MEDIUM_CHANCE ? medium : large;
		Optional<? extends Holder<ConfiguredFeature<?, ?>>> optional = level.registryAccess()
				.registryOrThrow(Registries.CONFIGURED_FEATURE)
				.getHolder(key);
		var event = EventHooks.fireBlockGrowFeature(level, random, pos, optional.orElse(null));
		if (event.isCanceled()) {
			return false;
		}
		optional = Optional.ofNullable(event.getFeature());
		if (optional.isEmpty()) {
			return false;
		} else {
			level.removeBlock(pos, false);
			if (optional.get().value().place(level, level.getChunkSource().getGenerator(), random, pos)) {
				return true;
			} else {
				level.setBlock(pos, state, 3);
				return false;
			}
		}
	}

}

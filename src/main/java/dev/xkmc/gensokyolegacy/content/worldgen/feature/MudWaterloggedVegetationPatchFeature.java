package dev.xkmc.gensokyolegacy.content.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WaterloggedVegetationPatchFeature;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

/**
 * A flooded vegetation patch whose plants stand on mud.
 * <p>
 * The same as {@link MudVegetationPatchFeature}, one cell lower: the plant grows in the water the patch leaves, so
 * what it stands on is the block under the water rather than the water's own cell.
 *
 * @see MudVegetationPatchFeature
 * @see "doc/design/gensokyo_biomes.md" §1
 */
public class MudWaterloggedVegetationPatchFeature extends WaterloggedVegetationPatchFeature {

	public MudWaterloggedVegetationPatchFeature(Codec<VegetationPatchConfiguration> codec) {
		super(codec);
	}

	@Override
	protected boolean placeVegetation(WorldGenLevel level, VegetationPatchConfiguration config, ChunkGenerator generator,
									  RandomSource random, BlockPos pos) {
		// the plant grows in the water at pos, so what it stands on is the cell under the water
		level.setBlock(pos.below(), Blocks.MUD.defaultBlockState(), 2);
		return super.placeVegetation(level, config, generator, random, pos);
	}

}

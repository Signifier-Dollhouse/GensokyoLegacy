package dev.xkmc.gensokyolegacy.content.dimension;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

import java.util.function.Supplier;

/**
 * The Gensokyo chunk generator: the overworld's, with the aquifer's lava sea eight blocks higher.
 * <p>
 * Only the fluid picker differs. Everything else — the settings, the biome source, every gate on the router — is
 * {@link NoiseBasedChunkGenerator}, which is why this is a subclass and not a copy. It is also a subclass because
 * the generator's type is its codec, and the level stem has to name something the registry can resolve back.
 * <p>
 * The picker is a field the parent fills from a private static factory, so it is replaced rather than overridden:
 * see {@link #gensokyo$fluidPicker()} and the access transformer.
 *
 * @see "doc/design/gensokyo_dimension.md" §11
 */
public class GensokyoChunkGenerator extends NoiseBasedChunkGenerator {

	public static final MapCodec<GensokyoChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
			i -> i.group(
					BiomeSource.CODEC.fieldOf("biome_source").forGetter(NoiseBasedChunkGenerator::getBiomeSource),
					NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(NoiseBasedChunkGenerator::generatorSettings)
			).apply(i, GensokyoChunkGenerator::new));

	/**
	 * Where the lava sea's surface sits, in absolute y. The engine's picker reads {@code min(-54, seaLevel)}, which
	 * on a {@code -64} world is {@code -54}; eight blocks higher is {@code -46}.
	 * <p>
	 * This is the level at which every air block turns to lava whatever the aquifers are doing, so it is what the
	 * bottom of the world looks like. The cave carver's own {@code lava_level} is a separate thing and is left
	 * where vanilla has it.
	 *
	 * @see "doc/design/gensokyo_dimension.md" §11
	 */
	private static final int LAVA_LEVEL = -46;

	private final Supplier<Aquifer.FluidPicker> fluidPicker;

	public GensokyoChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
		super(biomeSource, settings);
		// the three lines of `NoiseBasedChunkGenerator.createFluidPicker`, with the one constant moved, and lazy
		// for the reason the parent's is: during datagen the settings holder is not bound yet
		this.fluidPicker = Suppliers.memoize(() -> {
			BlockState lava = Blocks.LAVA.defaultBlockState();
			BlockState water = settings.value().defaultFluid();
			int seaLevel = settings.value().seaLevel();
			return (Aquifer.FluidPicker) (x, y, z) -> y < LAVA_LEVEL
					? new Aquifer.FluidStatus(LAVA_LEVEL, lava)
					: new Aquifer.FluidStatus(seaLevel, water);
		});
		this.globalFluidPicker = this.fluidPicker;
	}

	/**
	 * The picker this generator builds its aquifers with. Kept so the accessor above and the field assignment below
	 * read as one thing: the parent's is discarded, and this is what replaces it.
	 *
	 * @see "doc/design/gensokyo_dimension.md" §11
	 */
	public Supplier<Aquifer.FluidPicker> gensokyo$fluidPicker() {
		return fluidPicker;
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

}
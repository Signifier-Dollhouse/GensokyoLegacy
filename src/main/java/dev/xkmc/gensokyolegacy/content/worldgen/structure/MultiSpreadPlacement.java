package dev.xkmc.gensokyolegacy.content.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Random-spread placement with {@code attempts} uniform candidates per region
 * instead of one. Candidates cover the full {@code spacing x spacing} region
 * (no separation margin band); anti-collision for band positions is enforced
 * structure-side by {@link FlatCheckStructure}, which also suppresses every
 * candidate except the least-index passing one in each region, so at most one
 * structure start is placed per region.
 *
 * <p>Extends {@link RandomSpreadStructurePlacement} (with a dummy separation
 * of 0, never used since both placement hooks are overridden) so that vanilla
 * {@code /locate} handling keeps working, resolving to the attempt-0
 * candidate of each region.
 */
public class MultiSpreadPlacement extends RandomSpreadStructurePlacement {

	public static final MapCodec<MultiSpreadPlacement> CODEC = RecordCodecBuilder.<MultiSpreadPlacement>mapCodec((i) -> placementCodec(i).and(
			i.group(
					Codec.intRange(1, 4096).fieldOf("spacing").forGetter(MultiSpreadPlacement::spacing),
					RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(MultiSpreadPlacement::spreadType),
					Codec.intRange(1, 32).fieldOf("attempts").forGetter(MultiSpreadPlacement::attempts)
			)).apply(i, MultiSpreadPlacement::new));

	private final int spacing;
	private final RandomSpreadType spreadType;
	private final int attempts;

	public MultiSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency, int salt,
	                            Optional<ExclusionZone> exclusionZone, int spacing, RandomSpreadType spreadType, int attempts) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, 0, spreadType);
		this.spacing = spacing;
		this.spreadType = spreadType;
		this.attempts = attempts;
	}

	public MultiSpreadPlacement(int spacing, RandomSpreadType spreadType, int salt, int attempts) {
		this(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, salt, Optional.empty(), spacing, spreadType, attempts);
	}

	@Override
	public int spacing() {
		return this.spacing;
	}

	@Override
	public RandomSpreadType spreadType() {
		return this.spreadType;
	}

	public int attempts() {
		return this.attempts;
	}

	/**
	 * Ordered candidate chunks for the region containing the given chunk.
	 * Shared deterministic sequence used by both the placement and
	 * {@link FlatCheckStructure} (which mirrors spacing, spread type, salt
	 * and attempts in its own datapack entry, synced by datagen).
	 */
	public static List<ChunkPos> candidates(long seed, int chunkX, int chunkZ, int spacing, RandomSpreadType spreadType, int salt, int attempts) {
		int rx = Math.floorDiv(chunkX, spacing);
		int rz = Math.floorDiv(chunkZ, spacing);
		WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
		random.setLargeFeatureWithSalt(seed, rx, rz, salt);
		List<ChunkPos> ans = new ArrayList<>(attempts);
		for (int n = 0; n < attempts; n++) {
			ans.add(new ChunkPos(
					rx * spacing + spreadType.evaluate(random, spacing),
					rz * spacing + spreadType.evaluate(random, spacing)));
		}
		return ans;
	}

	public List<ChunkPos> getPotentialChunks(long seed, int chunkX, int chunkZ) {
		return candidates(seed, chunkX, chunkZ, this.spacing, this.spreadType, this.salt(), this.attempts);
	}

	@Override
	public ChunkPos getPotentialStructureChunk(long seed, int regionX, int regionZ) {
		return this.getPotentialChunks(seed, regionX, regionZ).getFirst();
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState structureState, int x, int z) {
		return this.getPotentialChunks(structureState.getLevelSeed(), x, z).contains(new ChunkPos(x, z));
	}

	@Override
	public StructurePlacementType<?> type() {
		return GLWorldGen.MULTI_SPREAD.get();
	}

}

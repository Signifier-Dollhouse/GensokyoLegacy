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
 * Random-spread placement yielding {@code attempts} uniform candidates over the full {@code spacing x spacing} region
 * instead of one; anti-collision for the old separation band positions is enforced structure-side by {@link
 * FlatCheckStructure}, which keeps only the least-index passing candidate, so at most one start is placed per region.
 * <p> Extends {@link RandomSpreadStructurePlacement} (dummy separation of 0, both hooks overridden) so vanilla {@code
 * /locate} handling keeps working, resolving to the attempt-0 candidate of each region.
 */
public class MultiSpreadPlacement extends RandomSpreadStructurePlacement {

	public static final MapCodec<MultiSpreadPlacement> CODEC = RecordCodecBuilder.mapCodec((i) -> placementCodec(i).and(
			i.group(
					Codec.intRange(1, 4096).fieldOf("spacing").forGetter(MultiSpreadPlacement::spacing),
					RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(MultiSpreadPlacement::spreadType),
					Codec.intRange(1, 128).fieldOf("attempts").forGetter(MultiSpreadPlacement::attempts)
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

	/**
	 * Widened from {@code protected} so the structure held by the set can salt its region pick with the same value the
	 * candidate stream uses.
	 */
	@Override
	public int salt() {
		return super.salt();
	}

	public int attempts() {
		return this.attempts;
	}

	/**
	 * Ordered candidate chunks for the region containing the given chunk. Shared deterministic sequence used by both
	 * the placement and {@link FlatCheckStructure}, which mirrors spacing, spread type, salt and attempts in its own
	 * datapack entry (synced by datagen).
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

	/**
	 * Chunk distance from the nearest edge of the region containing {@code pos}.
	 */
	public static int borderDistance(ChunkPos pos, int spacing) {
		int minX = Math.floorDiv(pos.x, spacing) * spacing;
		int minZ = Math.floorDiv(pos.z, spacing) * spacing;
		int maxX = minX + spacing - 1;
		int maxZ = minZ + spacing - 1;
		return Math.min(Math.min(pos.x - minX, maxX - pos.x), Math.min(pos.z - minZ, maxZ - pos.z));
	}

	public static long distanceSquared(ChunkPos a, ChunkPos b) {
		long dx = (long) a.x - b.x;
		long dz = (long) a.z - b.z;
		return dx * dx + dz * dz;
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

package dev.xkmc.gensokyolegacy.mixin;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.MultiSpreadPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorMultiSpreadMixin {

	@Shadow
	private static Pair<BlockPos, Holder<Structure>> getStructureGeneratingAt(
			Set<Holder<Structure>> structureHoldersSet, LevelReader level, StructureManager structureManager,
			boolean skipKnownStructures, StructurePlacement placement, ChunkPos chunkPos
	) {
		throw new AssertionError();
	}

	/**
	 * Shortcut for {@link MultiSpreadPlacement}: search region rings outward instead of letting vanilla re-enter the
	 * per-placement search for every radius step. Bails out when any placement is not a multi-spread.
	 */
	@Inject(method = "findNearestMapStructure", at = @At("HEAD"), cancellable = true)
	private void gensokyolegacy$multiSpreadFindNearest(
			ServerLevel level, HolderSet<Structure> structures, BlockPos pos, int searchRadius, boolean skipKnown,
			CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir
	) {
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		Map<MultiSpreadPlacement, Set<Holder<Structure>>> byPlacement = new HashMap<>();
		for (Holder<Structure> holder : structures) {
			for (StructurePlacement placement : state.getPlacementsForStructure(holder)) {
				if (!(placement instanceof MultiSpreadPlacement multi)) return;
				byPlacement.computeIfAbsent(multi, k -> new HashSet<>()).add(holder);
			}
		}
		if (byPlacement.isEmpty()) {
			cir.setReturnValue(null);
			return;
		}
		cir.setReturnValue(gensokyolegacy$findNearest(level, level.structureManager(), byPlacement,
				SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()),
				searchRadius, skipKnown, state.getLevelSeed()));
	}

	/**
	 * Nearest start within {@code radius} region rings around a chunk, or null.
	 * Ring {@code n} is the perimeter of the {@code (2n+1)^2} region square, so no cell is visited twice.
	 */
	private static Pair<BlockPos, Holder<Structure>> gensokyolegacy$findNearest(
			LevelReader level, StructureManager manager, Map<MultiSpreadPlacement, Set<Holder<Structure>>> byPlacement,
			int chunkX, int chunkZ, int radius, boolean skipKnown, long seed
	) {
		for (int ring = 0; ring <= radius; ring++) {
			Pair<BlockPos, Holder<Structure>> pair = gensokyolegacy$searchRing(
					level, manager, byPlacement, chunkX, chunkZ, ring, skipKnown, seed);
			if (pair != null) return pair;
		}
		return null;
	}

	private static Pair<BlockPos, Holder<Structure>> gensokyolegacy$searchRing(
			LevelReader level, StructureManager manager, Map<MultiSpreadPlacement, Set<Holder<Structure>>> byPlacement,
			int chunkX, int chunkZ, int ring, boolean skipKnown, long seed
	) {
		if (ring == 0) {
			return gensokyolegacy$searchRegion(level, manager, byPlacement, chunkX, chunkZ, 0, 0, skipKnown, seed);
		}
		for (int dx = -ring; dx <= ring; dx++) {
			Pair<BlockPos, Holder<Structure>> pair = gensokyolegacy$searchRegion(
					level, manager, byPlacement, chunkX, chunkZ, dx, ring, skipKnown, seed);
			if (pair == null) {
				pair = gensokyolegacy$searchRegion(
						level, manager, byPlacement, chunkX, chunkZ, dx, -ring, skipKnown, seed);
			}
			if (pair != null) return pair;
		}
		for (int dz = -ring + 1; dz < ring; dz++) {
			Pair<BlockPos, Holder<Structure>> pair = gensokyolegacy$searchRegion(
					level, manager, byPlacement, chunkX, chunkZ, ring, dz, skipKnown, seed);
			if (pair == null) {
				pair = gensokyolegacy$searchRegion(
						level, manager, byPlacement, chunkX, chunkZ, -ring, dz, skipKnown, seed);
			}
			if (pair != null) return pair;
		}
		return null;
	}

	private static Pair<BlockPos, Holder<Structure>> gensokyolegacy$searchRegion(
			LevelReader level, StructureManager manager, Map<MultiSpreadPlacement, Set<Holder<Structure>>> byPlacement,
			int chunkX, int chunkZ, int dx, int dz, boolean skipKnown, long seed
	) {
		for (Map.Entry<MultiSpreadPlacement, Set<Holder<Structure>>> entry : byPlacement.entrySet()) {
			MultiSpreadPlacement multi = entry.getKey();
			int regionX = chunkX + multi.spacing() * dx;
			int regionZ = chunkZ + multi.spacing() * dz;
			for (ChunkPos chunkpos : multi.getPotentialChunks(seed, regionX, regionZ)) {
				if (!gensokyolegacy$biomePlausible(level, entry.getValue(), chunkpos)) continue;
				Pair<BlockPos, Holder<Structure>> pair = getStructureGeneratingAt(
						entry.getValue(), level, manager, skipKnown, multi, chunkpos);
				if (pair != null) return pair;
			}
		}
		return null;
	}

	@Inject(method = "getNearestGeneratedStructure(Ljava/util/Set;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/StructureManager;IIIZJLnet/minecraft/world/level/levelgen/structure/placement/RandomSpreadStructurePlacement;)Lcom/mojang/datafixers/util/Pair;",
			at = @At("HEAD"), cancellable = true)
	private static void gensokyolegacy$multiSpreadLocate(
			Set<Holder<Structure>> structures, LevelReader level, StructureManager manager,
			int chunkX, int chunkZ, int radius, boolean skipKnown, long seed,
			RandomSpreadStructurePlacement placement,
			CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir
	) {
		if (!(placement instanceof MultiSpreadPlacement multi)) return;
		cir.setReturnValue(gensokyolegacy$findNearest(
				level, manager, Map.of(multi, structures), chunkX, chunkZ, radius, skipKnown, seed));
	}

	/**
	 * True if any located structure's biome tag contains any sampled biome in the candidate's middle column.
	 * Covers the full height range so a chunk that could pass the middle-column prefilter is never skipped.
	 */
	private static boolean gensokyolegacy$biomePlausible(
			LevelReader level, Set<Holder<Structure>> structures, ChunkPos chunkpos
	) {
		if (!(level instanceof ServerLevel server)) return true;
		var biomes = server.getChunkSource().getGenerator().getBiomeSource();
		var sampler = server.getChunkSource().randomState().sampler();
		int x = QuartPos.fromBlock(chunkpos.getMiddleBlockX());
		int z = QuartPos.fromBlock(chunkpos.getMiddleBlockZ());
		for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y += 8) {
			var biome = biomes.getNoiseBiome(x, QuartPos.fromBlock(y), z, sampler);
			for (Holder<Structure> holder : structures) {
				if (holder.value().biomes().contains(biome)) return true;
			}
		}
		return false;
	}

}

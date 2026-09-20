package dev.xkmc.gensokyolegacy.mixin;

import com.mojang.datafixers.util.Pair;
import dev.xkmc.gensokyolegacy.init.data.structure.MultiSpreadPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

	@Inject(method = "getNearestGeneratedStructure(Ljava/util/Set;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/StructureManager;IIIZJLnet/minecraft/world/level/levelgen/structure/placement/RandomSpreadStructurePlacement;)Lcom/mojang/datafixers/util/Pair;",
			at = @At("HEAD"), cancellable = true)
	private static void gensokyolegacy$multiSpreadLocate(
			Set<Holder<Structure>> structures, LevelReader level, StructureManager manager,
			int chunkX, int chunkZ, int radius, boolean skipKnown, long seed,
			RandomSpreadStructurePlacement placement,
			CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir
	) {
		if (!(placement instanceof MultiSpreadPlacement multi)) return;
		int spacing = multi.spacing();
		for (int j = -radius; j <= radius; j++) {
			boolean edgeJ = j == -radius || j == radius;
			for (int k = -radius; k <= radius; k++) {
				boolean edgeK = k == -radius || k == radius;
				if (edgeJ || edgeK) {
					int l = chunkX + spacing * j;
					int i1 = chunkZ + spacing * k;
					for (ChunkPos chunkpos : multi.getPotentialChunks(seed, l, i1)) {
						Pair<BlockPos, Holder<Structure>> pair = getStructureGeneratingAt(
								structures, level, manager, skipKnown, multi, chunkpos);
						if (pair != null) {
							cir.setReturnValue(pair);
							return;
						}
					}
				}
			}
		}
		cir.setReturnValue(null);
	}

}

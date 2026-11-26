package dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.PieceTree;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Set;

/**
 * Flat ground rule: the root footprint is level within the tolerance, every rigid piece stands within it, and
 * rigid groups joined by terrain-matching pieces meet within it. Kept from the original flat check.
 */
public record UniformHeightCheck(int tolerance) implements TerrainPredicate {

	public static final ResourceLocation ID = GensokyoLegacy.loc("uniform_height");

	public static final Codec<UniformHeightCheck> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.intRange(1, 128).fieldOf("height_tolerance").forGetter(UniformHeightCheck::tolerance)
	).apply(i, UniformHeightCheck::new));

	@Override
	public ResourceLocation type() {
		return ID;
	}

	@Override
	public Codec<UniformHeightCheck> codec() {
		return CODEC;
	}

	@Override
	public FitResult fit(TerrainFit fit) {
		PieceTree tree = fit.tree();
		int sea = fit.seaLevel();
		int[] root = fit.ring(tree.piece(0).getBoundingBox());
		int min = TerrainFit.min(root), max = TerrainFit.max(root);
		for (int h : root) {
			if (h <= sea) {
				return FitResult.fail();
			}
		}
		if (max - min > 2 * tolerance) {
			return FitResult.fail();
		}
		int spawn = (max + min) / 2;
		int delta = tree.shiftRigidGroupToGround(0, spawn);
		for (int i = 0; i < tree.size(); i++) {
			if (!tree.isRigid(i)) continue;
			BoundingBox b = tree.piece(i).getBoundingBox();
			int h = fit.height((b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2);
			if (h <= sea) {
				return FitResult.fail();
			}
			int g = tree.groundOf(i);
			if (Math.abs(h - g) > tolerance) {
				return FitResult.fail();
			}
		}
		for (Set<Integer> grounds : tree.flexJointGrounds()) {
			if (grounds.size() < 2) continue;
			int lo = grounds.stream().mapToInt(Integer::intValue).min().orElse(0);
			int hi = grounds.stream().mapToInt(Integer::intValue).max().orElse(0);
			if (hi - lo > tolerance) {
				return FitResult.fail();
			}
		}
		return FitResult.pass(delta);
	}

}

package dev.xkmc.gensokyolegacy.content.worldgen.structure.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.xkmc.gensokyolegacy.content.worldgen.structure.PieceTree;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
				return FitResult.fail("sea");
			}
		}
		if (max - min > 2 * tolerance) {
			return FitResult.fail("height[%d-%d]".formatted(min, max));
		}
		int spawn = (max + min) / 2;
		int delta = tree.shiftRigidGroupToGround(0, spawn);
		for (int i = 0; i < tree.size(); i++) {
			if (!tree.isRigid(i)) continue;
			BoundingBox b = tree.piece(i).getBoundingBox();
			int h = fit.height((b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2);
			if (h <= sea) {
				return FitResult.fail("rigid-sea-%d".formatted(i));
			}
			int g = tree.groundOf(i);
			if (Math.abs(h - g) > tolerance) {
				return FitResult.fail("rigid-%d[%d-vs-%d]".formatted(i, h, g));
			}
		}
		for (Set<Integer> grounds : tree.flexJointGrounds()) {
			if (grounds.size() < 2) continue;
			int lo = grounds.stream().mapToInt(Integer::intValue).min().orElse(0);
			int hi = grounds.stream().mapToInt(Integer::intValue).max().orElse(0);
			if (hi - lo > tolerance) {
				return FitResult.fail("joint[%d-%d]".formatted(lo, hi));
			}
		}
		return FitResult.pass(delta);
	}

	@Override
	public List<String> describe(TerrainFit fit) {
		PieceTree tree = fit.tree();
		List<String> out = new ArrayList<>();
		out.add("pieces=%d rigidGroups=%d flexGroups=%d".formatted(
				tree.size(), tree.rigidGroupCount(), tree.flexGroupCount()));
		int[] root = fit.ring(tree.piece(0).getBoundingBox());
		out.add("root ring=%s min=%d max=%d spread=%d limit=%d".formatted(Arrays.toString(root),
				TerrainFit.min(root), TerrainFit.max(root), TerrainFit.max(root) - TerrainFit.min(root), 2 * tolerance));
		for (int i = 0; i < tree.size(); i++) {
			if (!tree.isRigid(i)) continue;
			BoundingBox b = tree.piece(i).getBoundingBox();
			int h = fit.height((b.minX() + b.maxX()) / 2, (b.minZ() + b.maxZ()) / 2);
			int g = tree.groundOf(i);
			String verdict = h <= fit.seaLevel() ? "FAIL-sea" : Math.abs(h - g) > tolerance ? "FAIL" : "ok";
			out.add("#%d h=%d g=%d d=%d limit=%d %s".formatted(i, h, g, h - g, tolerance, verdict));
		}
		for (Set<Integer> grounds : tree.flexJointGrounds()) {
			if (grounds.size() < 2) continue;
			int lo = grounds.stream().mapToInt(Integer::intValue).min().orElse(0);
			int hi = grounds.stream().mapToInt(Integer::intValue).max().orElse(0);
			out.add("joint %s spread=%d limit=%d".formatted(grounds, hi - lo, tolerance));
		}
		return out;
	}

}

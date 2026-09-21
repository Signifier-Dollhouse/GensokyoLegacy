package dev.xkmc.gensokyolegacy.content.worldgen.placement;

import com.mojang.serialization.MapCodec;
import dev.xkmc.gensokyolegacy.init.registrate.GLWorldGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Emits the points of a {@link GridLayer} that fall into the chunk being decorated. Replaces
 * count + in_square for large vegetation: guaranteed spacing, no dependence on generation order.
 * Must be the first modifier, it expects the chunk position vanilla hands to placed features.
 */
public class JitteredGridPlacement extends PlacementModifier {

	public static final MapCodec<JitteredGridPlacement> CODEC = GridLayer.CODEC.xmap(JitteredGridPlacement::new, e -> e.layer);

	private final GridLayer layer;

	public JitteredGridPlacement(GridLayer layer) {
		this.layer = layer;
	}

	@Override
	public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
		long seed = ctx.getLevel().getSeed();
		int minX = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(pos.getX()));
		int minZ = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(pos.getZ()));
		int cell = layer.cell();
		List<BlockPos> ans = new ArrayList<>(1);
		for (int cx = Math.floorDiv(minX, cell); cx <= Math.floorDiv(minX + 15, cell); cx++) {
			for (int cz = Math.floorDiv(minZ, cell); cz <= Math.floorDiv(minZ + 15, cell); cz++) {
				BlockPos point = layer.point(seed, cx, cz);
				if (point == null) continue;
				if (point.getX() < minX || point.getX() > minX + 15 || point.getZ() < minZ || point.getZ() > minZ + 15) continue;
				ans.add(point.atY(pos.getY()));
			}
		}
		return ans.stream();
	}

	@Override
	public PlacementModifierType<?> type() {
		return GLWorldGen.JITTERED_GRID.get();
	}

}

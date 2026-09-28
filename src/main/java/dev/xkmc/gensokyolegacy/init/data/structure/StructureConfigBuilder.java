package dev.xkmc.gensokyolegacy.init.data.structure;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.InteriorNode;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.InteriorRoom;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureInterior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Repair tags per structure, plus precalculated in-room boxes.
 * Room boxes (youkai wander/interact area) are hardcoded here in
 * template-local coords (root only for jigsaw) from the verified
 * {@code RoomBoxScanner} output: every box holds only in-room air and
 * wall/solid blocks, never outdoor or under-roof air. Regenerate with the
 * {@code StructureSpaceVerifier} report after editing templates.
 * House bound (integrity snapshot) still shrinks the placed piece box at
 * runtime. Primary blocks are the load-bearing shell restored first;
 * wouldFix blocks are furniture/containers restored second. Template blocks
 * in neither tag are cleared to air when fixing (foliage, grass, soil,
 * small deco) instead of being restored.
 */
public class StructureConfigBuilder {

	// marisa_house template is 30x13x25 with bed at local y=6
	// ground floor feet at y=2 (ceiling slabs at y=6), upper floor feet at
	// y=7 (roof above y=8). West ground splits at the x=9 wall (door z=16),
	// west upper splits at the x=14 wall (door z=18); wings join at the
	// x=17/18 archway (z=15-21) on the ground floor and the open hall
	// (z=16-20) on the upper floor. East stair shaft at x=23-25, z=14-18,
	// alchemy platform at z=9-13 behind the z=13 railing gap.
	// Room boxes are disjoint: shared walls belong to roomA of the doorway
	// node, the neighbor room is reduced on that side. Stair staging cells
	// stand clear of the stair run, on ground and upper floors respectively.
	public static StructureConfig.Builder marisa() {
		return StructureConfig.builder()
				.rooms(List.of(
						new BoundingBox(18, 2, 9, 26, 8, 21),
						new BoundingBox(3, 2, 14, 17, 8, 22)
				)).interior(marisaInterior())
				.house(1, 2, 1)
				.primary(GLStructureTagGen.MARISA_PRIMARY)
				.wouldFix(GLStructureTagGen.MARISA_FIX);
	}

	private static StructureInterior marisaInterior() {
		var rooms = new ArrayList<>(List.of(
				new InteriorRoom("west_ground_west", new BoundingBox(3, 2, 14, 9, 6, 22)),
				new InteriorRoom("west_ground_east", new BoundingBox(10, 2, 14, 17, 6, 22)),
				new InteriorRoom("west_upper_bedroom", new BoundingBox(3, 7, 14, 14, 8, 22)),
				new InteriorRoom("upper_hall", new BoundingBox(15, 7, 14, 26, 8, 22)),
				new InteriorRoom("east_ground", new BoundingBox(18, 2, 9, 26, 6, 21)),
				new InteriorRoom("east_upper_platform", new BoundingBox(18, 7, 9, 26, 8, 13))
		));
		var nodes = new ArrayList<>(List.of(
				new InteriorNode(new BlockPos(9, 2, 16), new BlockPos(9, 2, 16), 0, 1),
				new InteriorNode(new BlockPos(14, 7, 18), new BlockPos(14, 7, 18), 2, 3),
				new InteriorNode(new BlockPos(17, 2, 18), new BlockPos(17, 2, 18), 1, 4),
				new InteriorNode(new BlockPos(21, 7, 13), new BlockPos(21, 7, 13), 5, 3),
				new InteriorNode(new BlockPos(24, 2, 13), new BlockPos(24, 7, 19), 4, 3),
				new InteriorNode(new BlockPos(3, 2, 17), new BlockPos(3, 2, 17), 0, -1),
				new InteriorNode(new BlockPos(21, 2, 9), new BlockPos(21, 2, 9), 4, -1)
		));
		return new StructureInterior(rooms, nodes);
	}

	// hakurei_shrine root is 19x14x19 with bed at local (5,2,12)-(5,2,13); other jigsaw parts hang off it
	public static StructureConfig.Builder hakurei() {
		return StructureConfig.builder()
				.rooms(List.of(
						new BoundingBox(3, 2, 8, 15, 5, 13),
						new BoundingBox(5, 2, 6, 13, 5, 7)
				)).house(1, 2, 1)
				.primary(GLStructureTagGen.REIMU_PRIMARY)
				.wouldFix(GLStructureTagGen.REIMU_FIX);
	}

	// morichika_shop template is 33x18x33 with bed at local y=8
	public static StructureConfig.Builder morichika() {
		return StructureConfig.builder()
				.rooms(List.of(
						new BoundingBox(3, 2, 15, 13, 7, 21),
						new BoundingBox(14, 2, 16, 23, 5, 20),
						new BoundingBox(24, 2, 14, 29, 10, 25)
				)).house(1, 2, 1)
				.primary(GLStructureTagGen.MORICHIKA_PRIMARY)
				.wouldFix(GLStructureTagGen.MORICHIKA_FIX);
	}

}

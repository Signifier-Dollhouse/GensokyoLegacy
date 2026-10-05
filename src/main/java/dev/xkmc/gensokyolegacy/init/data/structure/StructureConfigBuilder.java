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
 * Repair tags per structure, precalculated in-room boxes, and the integrity
 * snapshot box. Room boxes (youkai wander/interact area) are hardcoded here
 * in template-local coords (root only for jigsaw) from the verified
 * {@code RoomBoxScanner} output: every box holds only in-room air and
 * wall/solid blocks, never outdoor or under-roof air. Regenerate with the
 * {@code StructureSpaceVerifier} report after editing templates.
 * The house boxes (integrity snapshot) are template-local boxes fitted to
 * actual building geometry: walls, floors, roof and foundation, but never
 * outdoor ground, yard soil or margin foliage. Only cells inside a house
 * box are scanned and repaired; the raster covers their union.
 * Marisa's room bound comes from her interior rooms; her legacy room boxes
 * are deleted.
 * Primary blocks are the load-bearing shell restored first;
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
	// Upper boxes stay clear of the roof-stair intrusions at the y8 edges
	// (z=14/22 rows, x=3/18/26 columns); graph connectivity is unaffected
	// as it runs on node declarations, not box overlap.
	// West and east wings sharing one y level, roof included: side porch
	// roof and ridge run are building. North garden, south awning, ground
	// trims and the ridge tip stay outside with the yard soil.
	public static StructureConfig.Builder marisa() {
		return StructureConfig.builder()
				.interior(marisaInterior())
				.house(List.of(
						new BoundingBox(1, 1, 12, 15, 12, 24),
						new BoundingBox(16, 1, 7, 28, 12, 24)
				))
				.primary(GLStructureTagGen.MARISA_PRIMARY)
				.wouldFix(GLStructureTagGen.MARISA_FIX);
	}

	private static StructureInterior marisaInterior() {
		var rooms = new ArrayList<>(List.of(
				new InteriorRoom("west_ground_west", new BoundingBox(3, 2, 14, 9, 6, 22)),
				new InteriorRoom("west_ground_east", new BoundingBox(10, 2, 14, 17, 6, 22)),
				new InteriorRoom("west_upper_bedroom", new BoundingBox(3, 7, 15, 14, 8, 21)),
				new InteriorRoom("upper_hall", new BoundingBox(15, 7, 15, 25, 8, 21)),
				new InteriorRoom("east_ground", new BoundingBox(18, 2, 9, 26, 6, 21)),
				new InteriorRoom("east_upper_platform", new BoundingBox(19, 7, 9, 25, 8, 14))
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
	// Single box lifted by one to exclude the ground plate. The template
	// holds no yard soil at all, so the box spans the full footprint.
	public static StructureConfig.Builder hakurei() {
		return StructureConfig.builder()
				.rooms(List.of(
						new BoundingBox(3, 2, 8, 15, 5, 13),
						new BoundingBox(5, 2, 6, 13, 5, 7)
				)).house(List.of(
						new BoundingBox(0, 1, 0, 18, 13, 18)
				))
				.primary(GLStructureTagGen.REIMU_PRIMARY)
				.wouldFix(GLStructureTagGen.REIMU_FIX);
	}

	// morichika_shop template is 33x18x33 with bed at local y=8
	// Lower complex (west hall plus mid block, eaves and roof tiers) and
	// east tower with facade trim. Boxes are disjoint. The ground layer,
	// south tree and margin soil stay outside.
	public static StructureConfig.Builder morichika() {
		return StructureConfig.builder()
				.rooms(List.of(
						new BoundingBox(3, 2, 15, 13, 7, 21),
						new BoundingBox(14, 2, 16, 23, 5, 20),
						new BoundingBox(24, 2, 14, 29, 10, 25)
				)).house(List.of(
						new BoundingBox(0, 1, 12, 21, 12, 24),
						new BoundingBox(22, 1, 12, 31, 17, 27)
				))
				.primary(GLStructureTagGen.MORICHIKA_PRIMARY)
				.wouldFix(GLStructureTagGen.MORICHIKA_FIX);
	}

	// alice_house root is 29x23x18 with bed at local (12,6,6)-(12,6,7);
	// the other five jigsaw parts (gate, garden, path, tree, backyard) are
	// pure yard and hold no building, so they need no boxes.
	// House box lifted by one to exclude the ground plate, spanning the
	// whole shell: west tower x1-10 y1-17 plus its hip roof up to y22, east
	// wing x11-25 under a gable roof up to y13, the iron awning at z1-5 and
	// the entry porch out to x28. Only the roof eave overhangs past it
	// (x=0 and z=17, both at y18) and the yard soil stay outside.
	// Room boxes are disjoint and hold in-room air, shell blocks and
	// furniture only. The tower hall is a single open shaft: no intermediate
	// floors, so it runs from the y0 plank floor up to the hip roof at y18.
	// The x10-11 arcade is walled off from the wing above y4 (x11 turns
	// solid from y5 on) and dead-ends there, so the tower side and the wing
	// ground floor are its only links. The wing splits at the y5 plank
	// floor: ground y1-4, upper y5-8 with Alice's bed in its northwest
	// corner. Indoor air left out on purpose: the wing attic under the gable
	// (y9-13, ridge z10) and the tower roof interior (y18-21).
	public static StructureConfig.Builder alice() {
		return StructureConfig.builder()
				.interior(aliceInterior())
				.house(List.of(
						new BoundingBox(0, 0, 0, 18, 22, 17)
				))
				.primary(GLStructureTagGen.ALICE_PRIMARY)
				.wouldFix(GLStructureTagGen.ALICE_FIX);
	}

	// Ground level runs tower -> arcade -> wing in a straight line along
	// z9-13 at y1, both doorways being wall-less gaps. The stair node is the
	// only way up; its two staging cells are flat ground just clear of the
	// stair run, on the ground floor before the lowest step and on the upper
	// floor past the top one, as for marisa's east shaft. Both exterior
	// doors are entries: the north door into the ground floor and the
	// balcony door out of the upper floor at x26.
	private static StructureInterior aliceInterior() {
		var rooms = new ArrayList<>(List.of(
				new InteriorRoom("tower_hall", new BoundingBox(2, 1, 7, 9, 17, 15)),
				new InteriorRoom("arcade", new BoundingBox(10, 1, 9, 11, 9, 13)),
				new InteriorRoom("wing_ground", new BoundingBox(12, 1, 6, 25, 4, 14)),
				new InteriorRoom("wing_upper", new BoundingBox(12, 5, 6, 25, 8, 14))
		));
		var nodes = new ArrayList<>(List.of(
				new InteriorNode(new BlockPos(9, 1, 11), new BlockPos(9, 1, 11), 0, 1),
				new InteriorNode(new BlockPos(11, 1, 11), new BlockPos(11, 1, 11), 1, 2),
				new InteriorNode(new BlockPos(24, 1, 8), new BlockPos(24, 6, 14), 2, 3),
				new InteriorNode(new BlockPos(18, 1, 6), new BlockPos(18, 1, 6), 2, -1),
				new InteriorNode(new BlockPos(25, 6, 7), new BlockPos(25, 6, 7), 3, -1)
		));
		return new StructureInterior(rooms, nodes);
	}

}

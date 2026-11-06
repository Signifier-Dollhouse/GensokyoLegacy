package dev.xkmc.gensokyolegacy.init.data.structure;

import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.InteriorNode;
import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.InteriorRoom;
import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.StructureConfig;
import dev.xkmc.gensokyolegacy.content.attachment.home.datamap.StructureInterior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Repair tags, in-room boxes and the integrity snapshot box, all template-local; verify with tools/checkconfig.py.
 * Room boxes (the youkai wander area) hold only in-room air and solid blocks, from verified {@code RoomBoxScanner}
 * output; house boxes fit building geometry. Primary first, wouldFix second, neither cleared to air.
 */
public class StructureConfigBuilder {

	/**
	 * cirno_nest is one 13x8x13 dome, bed at (10,1,7)-(10,1,8), spruce door at (6,1,1) sealing the z0-1 gap: a
	 * single sealed chamber of 185 air cells, no interior graph, so plain vanilla pathing works. The two room boxes
	 * split it at the pinch, are disjoint in y and spill 18 corner gaps; the door leaf at z1 is a wall.
	 */
	public static StructureConfig.Builder cirno() {
		return StructureConfig.builder()
				.rooms(List.of(
						new BoundingBox(2, 1, 3, 3, 2, 9),
						new BoundingBox(4, 1, 2, 8, 2, 10),
						new BoundingBox(9, 1, 3, 10, 2, 9)
				)).house(List.of(
						new BoundingBox(1, 0, 1, 11, 6, 11)
				))
				.primary(GLStructureTagGen.CIRNO_PRIMARY)
				.wouldFix(GLStructureTagGen.CIRNO_FIX);
	}

	/**
	 * marisa_house is 30x13x25 with bed at local y=6 (ground feet y=2, upper y=7); west ground splits at the
	 * x=9 wall, west upper at x=14, wings join at the x=17/18 archway, east stair shaft at x=23-25. Room boxes
	 * are disjoint and a doorway node sits on the air cell inside roomA; wings share a y level, roof included.
	 */
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
				new InteriorNode(new BlockPos(8, 2, 16), new BlockPos(8, 2, 16), 0, 1),
				new InteriorNode(new BlockPos(13, 7, 18), new BlockPos(13, 7, 18), 2, 3),
				new InteriorNode(new BlockPos(17, 2, 18), new BlockPos(17, 2, 18), 1, 4),
				new InteriorNode(new BlockPos(21, 7, 13), new BlockPos(21, 7, 13), 5, 3),
				new InteriorNode(new BlockPos(24, 2, 13), new BlockPos(24, 7, 19), 4, 3),
				new InteriorNode(new BlockPos(3, 2, 17), new BlockPos(3, 2, 17), 0, -1),
				new InteriorNode(new BlockPos(21, 2, 9), new BlockPos(21, 2, 9), 4, -1)
		));
		return new StructureInterior(rooms, nodes);
	}

	/**
	 * hakurei_shrine root is 19x14x19 with bed at local (5,2,12)-(5,2,13); other jigsaw parts hang off it.
	 * Single box lifted by one to exclude the ground plate; the template holds no yard soil, so the box spans
	 * the full footprint.
	 */
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

	/**
	 * morichika_shop template is 33x18x33 with bed at local y=8. Lower complex (west hall plus mid block, eaves
	 * and roof tiers) and east tower with facade trim. Boxes are disjoint; the ground layer, south tree and
	 * margin soil stay outside.
	 */
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

	/**
	 * alice_house root is 30x23x20, bed upstairs at (21,6,7)-(22,6,8); the other five jigsaw parts are pure yard.
	 * House box lifted to y1 and inset so ground plate, yard soil and margin foliage stay out. Rooms are
	 * disjoint - west tower, the light slot x10-11 open to the sky at z5-7/z15-17, the wing split at y5.
	 */
	public static StructureConfig.Builder alice() {
		return StructureConfig.builder()
				.interior(aliceInterior())
				.house(List.of(
						new BoundingBox(1, 1, 1, 29, 22, 18)
				))
				.primary(GLStructureTagGen.ALICE_PRIMARY)
				.wouldFix(GLStructureTagGen.ALICE_FIX);
	}

	/**
	 * Every room must be wired: {@code findRoute} follows declared nodes only, so an unwired room is unreachable.
	 * corridor_up is the exception - wired to the wing, not the tower, so the balcony is no exit onto the drop.
	 * The stair node stages clear of the run at (19,1,15) and (15,5,15); stair_hall meets landing at y4/y5.
	 */
	private static StructureInterior aliceInterior() {
		var rooms = new ArrayList<>(List.of(
				new InteriorRoom("tower_hall", new BoundingBox(2, 1, 7, 9, 17, 15)),
				new InteriorRoom("corridor_gnd", new BoundingBox(10, 1, 10, 11, 4, 12)),
				new InteriorRoom("hall_ground", new BoundingBox(12, 1, 6, 21, 4, 14)),
				new InteriorRoom("stair_hall", new BoundingBox(13, 1, 15, 21, 4, 16)),
				new InteriorRoom("east_room", new BoundingBox(22, 1, 6, 26, 4, 16)),
				new InteriorRoom("wing_upper", new BoundingBox(12, 5, 7, 26, 8, 14)),
				new InteriorRoom("landing", new BoundingBox(13, 5, 15, 21, 8, 16)),
				new InteriorRoom("cabinet_nook", new BoundingBox(23, 5, 15, 25, 8, 15)),
				new InteriorRoom("corridor_up", new BoundingBox(10, 5, 10, 11, 8, 12))
		));
		var nodes = new ArrayList<>(List.of(
				new InteriorNode(new BlockPos(9, 2, 11), new BlockPos(9, 2, 11), 0, 1),
				new InteriorNode(new BlockPos(12, 1, 11), new BlockPos(12, 1, 11), 2, 1),
				new InteriorNode(new BlockPos(21, 1, 12), new BlockPos(21, 1, 12), 2, 4),
				new InteriorNode(new BlockPos(21, 1, 15), new BlockPos(21, 1, 15), 3, 2),
				new InteriorNode(new BlockPos(20, 1, 15), new BlockPos(15, 6, 15), 3, 6),
				new InteriorNode(new BlockPos(14, 6, 15), new BlockPos(14, 6, 15), 6, 5),
				new InteriorNode(new BlockPos(23, 6, 15), new BlockPos(23, 6, 15), 7, 5),
				new InteriorNode(new BlockPos(11, 6, 11), new BlockPos(11, 6, 11), 8, 5),
				new InteriorNode(new BlockPos(20, 1, 6), new BlockPos(20, 1, 6), 2, -1),
				new InteriorNode(new BlockPos(26, 6, 8), new BlockPos(26, 6, 8), 5, -1)
		));
		return new StructureInterior(rooms, nodes);
	}

}

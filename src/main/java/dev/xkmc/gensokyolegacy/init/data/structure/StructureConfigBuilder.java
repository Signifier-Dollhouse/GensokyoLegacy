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
 * {@code StructureSpaceVerifier} report after editing templates, then check
 * with {@code python3 tools/checkconfig.py <structure> <template.nbt>}, which
 * reads the generated datamap and validates it against the shipped template
 * (see {@code doc/tools.md}).
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

	/**
	 * marisa_house template is 30x13x25 with bed at local y=6
	 * ground floor feet at y=2 (ceiling slabs at y=6), upper floor feet at
	 * y=7 (roof above y=8). West ground splits at the x=9 wall (door z=16),
	 * west upper splits at the x=14 wall (door z=18); wings join at the
	 * x=17/18 archway (z=15-21) on the ground floor and the open hall
	 * (z=16-20) on the upper floor. East stair shaft at x=23-25, z=14-18,
	 * alchemy platform at z=9-13 behind the z=13 railing gap.
	 * Room boxes are disjoint: shared walls belong to roomA of the doorway
	 * node, the neighbor room is reduced on that side. Stair staging cells
	 * stand clear of the stair run, on ground and upper floors respectively.
	 * Upper boxes stay clear of the roof-stair intrusions at the y8 edges
	 * (z=14/22 rows, x=3/18/26 columns); graph connectivity is unaffected
	 * as it runs on node declarations, not box overlap.
	 * A doorway node sits on the air cell inside roomA, one block in from the
	 * door leaf, so a shared waypoint is always standable; the leaf itself is
	 * a solid block and would aim the path at a wall.
	 * West and east wings sharing one y level, roof included: side porch
	 * roof and ridge run are building. North garden, south awning, ground
	 * trims and the ridge tip stay outside with the yard soil.
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
	 * hakurei_shrine root is 19x14x19 with bed at local (5,2,12)-(5,2,13);
	 * other jigsaw parts hang off it.
	 * Single box lifted by one to exclude the ground plate. The template
	 * holds no yard soil at all, so the box spans the full footprint.
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
	 * morichika_shop template is 33x18x33 with bed at local y=8
	 * Lower complex (west hall plus mid block, eaves and roof tiers) and
	 * east tower with facade trim. Boxes are disjoint. The ground layer,
	 * south tree and margin soil stay outside.
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
	 * alice_house root is 30x23x20 with the double bed at local
	 * (21,6,7)-(22,6,8), i.e. upstairs now; the other five jigsaw parts
	 * (gate, garden, path, tree, backyard) are pure yard and hold no
	 * building, so they need no boxes.
	 * House box is lifted to y1 and inset to x1/z1/z18 so the ground plate,
	 * the yard soil and the margin foliage stay outside, spanning the whole
	 * shell otherwise: the west tower and its hip roof up to y22, the east
	 * wing under a gable roof to y18, the porch and iron awning out to x29
	 * and the roof eaves at z18. Only x=0 (one eave block at y18 plus leaves
	 * at y1) and the z0/z19 strips fall outside.
	 * Room boxes are disjoint and hold in-room air, shell blocks and
	 * furniture only. The plan is a west tower, a two-block light slot at
	 * x10-11 and the wing east of it, with the slot open to the sky at the
	 * z5-7 and z15-17 ends, so no room may span it at those ends. The tower
	 * is a single open shaft with no intermediate floors, y1-17, and its box
	 * runs z7-15 so it takes in the loom bench on the z7 platform, the alcove
	 * in front of the south window at z15, and the shallow niches along the
	 * west wall at x2, while stopping short of the two 2x3 window panes
	 * themselves at z6 and z16: those are wall, not standing space, and x2
	 * has no wall at either z, so reaching them would admit outdoor air.
	 * corridor_gnd is the slot at z10-12 on the ground floor, a real walkway
	 * between the tower and the wing, so it carries nodes on both sides. The
	 * same slot on the upper floor is corridor_up, an inner balcony over the
	 * 17-block drop, railed off from the tower at x10 by wrought iron bars.
	 * It is a room in its own right and joins the wing, but it deliberately
	 * has no node to the tower: touching tower_hall is fine, routing the
	 * youkai out onto the balcony above the drop is not, and the rail is what
	 * makes standing on it safe. The wing splits at the y5 plank floor:
	 * hall_ground y1-4 and wing_upper y5-8, the latter holding the bed. The
	 * stair hall and its landing are a separate two-cell-deep shaft at z15-16
	 * behind the z14 door row, walled off from the upper floor too, so the
	 * landing reaches the wing only through the x13-14 doors at z14.
	 * cabinet_nook is the two-block-tall alcove under the x23-25 door
	 * cabinets, open to the wing at z14 and walled from the landing at x22,
	 * so it is its own room rather than an extension of the landing. The east
	 * room is walled off at x22-23 and holds both the z6-13 room and the
	 * z15-16 corner behind one door. Indoor air left out on purpose: the wing
	 * loft and the tower roof interior (y19-21), neither of which is standing
	 * space.
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
	 * The slot is the only way between the tower and the wing, so corridor_gnd
	 * carries a node on each side of it. Every room must be wired to the rest
	 * this way: {@code findRoute} only follows declared nodes, so an unwired
	 * room is unreachable no matter how well its box fits. corridor_up is the
	 * one deliberate exception on the far side of that rule: it is wired to the
	 * wing so the balcony is usable, and left unwired from the tower so it is
	 * not an exit onto the drop. The remaining interior doors are closed
	 * leaves, so their shared position is the air cell just inside the room
	 * that owns the node. The stair node is the only way up: its two staging
	 * cells are one block clear of the run, on the ground floor east of the
	 * lowest step at (19,1,15) and on the landing above the top one at
	 * (15,5,15); stair_hall and landing touch at y4/y5 over the whole shaft
	 * so the pair stages continuously. Both exterior doors are entries: the
	 * north door into the ground floor and the east balcony door out of the
	 * upper floor at x27.
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

package dev.xkmc.gensokyolegacy.init.data.structure;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

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
	public static StructureConfig.Builder marisa() {
		return StructureConfig.builder()
				.rooms(List.of(
						new BoundingBox(18, 2, 9, 26, 8, 21),
						new BoundingBox(3, 2, 14, 17, 8, 22)
				)).house(1, 2, 1)
				.primary(GLStructureTagGen.MARISA_PRIMARY)
				.wouldFix(GLStructureTagGen.MARISA_FIX);
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

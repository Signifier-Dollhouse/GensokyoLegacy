package dev.xkmc.gensokyolegacy.init.data.structure;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.StructureConfig;

/**
 * Room/house shrink + repair tags per structure.
 * Room bound (youkai wander/interact area) excludes outer walls, roof and ground fill;
 * house bound (integrity snapshot) includes the shell but excludes surrounding terrain.
 * Primary blocks are the load-bearing shell restored first; wouldFix blocks are
 * furniture/containers restored second. Template blocks in neither tag are cleared
 * to air when fixing (foliage, grass, soil, small deco) instead of being restored.
 */
public class StructureConfigBuilder {

	// marisa_house template is 30x13x25 with bed at local y=6
	public static StructureConfig.Builder marisa() {
		return StructureConfig.builder()
				.room(2, 3, 1).house(1, 2, 1)
				.primary(GLStructureTagGen.MARISA_PRIMARY)
				.wouldFix(GLStructureTagGen.MARISA_FIX);
	}

	// hakurei_shrine root is 19x14x19 with bed at local (5,2,12)-(5,2,13); other jigsaw parts hang off it
	public static StructureConfig.Builder hakurei() {
		return StructureConfig.builder()
				.room(2, 4, 1).house(1, 2, 1)
				.primary(GLStructureTagGen.REIMU_PRIMARY)
				.wouldFix(GLStructureTagGen.REIMU_FIX);
	}

	// morichika_shop template is 33x18x33 with bed at local y=8
	public static StructureConfig.Builder morichika() {
		return StructureConfig.builder()
				.room(2, 4, 1).house(1, 2, 1)
				.primary(GLStructureTagGen.MORICHIKA_PRIMARY)
				.wouldFix(GLStructureTagGen.MORICHIKA_FIX);
	}

}

package dev.xkmc.gensokyolegacy.content.attachment.datamap;

import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * One walkable volume inside a structure, in template-local coords.
 * Boxes may include walls and furniture, but never outdoor air.
 */
public record InteriorRoom(
		String name,
		BoundingBox bound
) {
}

package dev.xkmc.gensokyolegacy.content.attachment.home.datamap;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * One walkable volume inside a structure, in template-local coords.
 * Boxes may include walls and furniture, but never outdoor air.
 *
 * @param owner the only character allowed to pick this room as a destination, or null when public;
 *              routing through it stays allowed either way.
 */
public record InteriorRoom(
		String name,
		BoundingBox bound,
		@Nullable EntityType<?> owner
) {

	public InteriorRoom(String name, BoundingBox bound) {
		this(name, bound, null);
	}

}

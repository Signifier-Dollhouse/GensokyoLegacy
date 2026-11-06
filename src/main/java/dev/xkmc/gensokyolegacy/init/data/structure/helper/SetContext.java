package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * What a structure needs from the set that places it: the set itself plus this member's index within the set and the
 * member count. Every region deterministically picks exactly one member, so members of one set never share a region;
 * unassigned indices generate nothing, which reserves room for future structures without shifting existing region
 * picks. Spacing, spread type, salt and attempts live on the set's placement alone.
 */
public record SetContext(ResourceLocation set, int setIndex, int setCount) {

	public static SetContext of(StructStructure self, List<StructStructure> members) {
		return of(self, members, members.size());
	}

	public static SetContext of(StructStructure self, List<StructStructure> members, int total) {
		if (total < members.size()) {
			throw new IllegalStateException("set total " + total + " smaller than member count " + members.size());
		}
		return new SetContext(self.set(), members.indexOf(self), total);
	}

}

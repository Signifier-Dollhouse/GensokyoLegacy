package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import java.util.List;

/**
 * Shared-set membership for one structure: the set salt (shared placement
 * grid) plus this member's index within the set and the member count.
 * Every region deterministically picks exactly one member, so members of
 * one set can never share a region. Unassigned indices (when the total
 * exceeds the registered member count) generate nothing, which reserves
 * room for future structures without shifting existing region picks.
 */
public record SetContext(int salt, int setIndex, int setCount) {

	public static SetContext of(StructStructure self, List<StructStructure> members) {
		return of(self, members, members.size());
	}

	public static SetContext of(StructStructure self, List<StructStructure> members, int total) {
		if (total < members.size()) {
			throw new IllegalStateException("set total " + total + " smaller than member count " + members.size());
		}
		return new SetContext(self.salt(), members.indexOf(self), total);
	}

}

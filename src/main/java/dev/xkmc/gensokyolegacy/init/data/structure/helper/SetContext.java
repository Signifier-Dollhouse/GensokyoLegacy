package dev.xkmc.gensokyolegacy.init.data.structure.helper;

import java.util.List;

/**
 * Shared-set membership for one structure: the set salt (shared placement
 * grid) plus this member's index within the set and the member count.
 * Every region deterministically picks exactly one member, so members of
 * one set can never share a region.
 */
public record SetContext(int salt, int setIndex, int setCount) {

	public static SetContext of(StructStructure self, List<StructStructure> members) {
		return new SetContext(self.salt(), members.indexOf(self), members.size());
	}

}

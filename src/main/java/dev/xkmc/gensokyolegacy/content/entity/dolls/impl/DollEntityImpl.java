package dev.xkmc.gensokyolegacy.content.entity.dolls.impl;

import dev.xkmc.gensokyolegacy.content.entity.dolls.DollEntity;

/**
 * The typed handle for the impl interfaces that only the loadout-capable doll implements: the
 * loadout, the status, the shield and the rest all read a {@link DollEntity}, so they inherit the
 * narrowing here instead of casting at each call (entity.md §9.1).
 *
 * @see "doc/design/doll/entity.md"
 */
public interface DollEntityImpl extends DollBaseImpl {

	@Override
	default DollEntity asDoll() {
		return (DollEntity) this;
	}

}
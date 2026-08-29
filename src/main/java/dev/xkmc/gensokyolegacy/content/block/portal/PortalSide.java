package dev.xkmc.gensokyolegacy.content.block.portal;

public enum PortalSide {
	ENTRY,
	EXIT;

	public PortalSide other() {
		return this == ENTRY ? EXIT : ENTRY;
	}
}

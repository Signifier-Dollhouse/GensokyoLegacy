package dev.xkmc.gensokyolegacy.content.entity.dolls.ghost;

/**
 * The variant accessor both ghost entity types share, so the renderers and the bone flute never
 * care which of the two they hold.
 */
public interface GhostVariantHolder {

	GhostVariant getVariant();

	void setVariant(GhostVariant variant);

}

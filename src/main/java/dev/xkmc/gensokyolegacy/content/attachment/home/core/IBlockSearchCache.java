package dev.xkmc.gensokyolegacy.content.attachment.home.core;

/**
 * Per-kind block search cache.
 * Implemented by home data holders so the search logic stays in one place.
 */
public interface IBlockSearchCache {

	BlockSearchCache cache(HomeBlockKind kind);

}

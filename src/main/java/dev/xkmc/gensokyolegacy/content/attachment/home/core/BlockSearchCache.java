package dev.xkmc.gensokyolegacy.content.attachment.home.core;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;

/**
 * Cached search result for one {@link HomeBlockKind}: known positions plus
 * the game time of the last search. A full re-search is skipped while
 * {@code now < lastSearch + pos.size() * 100}.
 */
@SerialClass
public class BlockSearchCache {

	@SerialField
	public final ArrayList<BlockPos> pos = new ArrayList<>();

	@SerialField
	public long lastSearch = 0;

}

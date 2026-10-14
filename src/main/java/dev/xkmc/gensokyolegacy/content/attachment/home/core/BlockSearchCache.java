package dev.xkmc.gensokyolegacy.content.attachment.home.core;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;

/**
 * Cached search per {@link HomeBlockKind}: positions and last search time; re-search waits
 * {@code pos.size() * 100} ticks.
 */
@SerialClass
public class BlockSearchCache {

	@SerialField
	public final ArrayList<BlockPos> pos = new ArrayList<>();

	@SerialField
	public long lastSearch = 0;

}

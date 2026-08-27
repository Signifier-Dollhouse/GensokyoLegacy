package dev.xkmc.gensokyolegacy.content.attachment.area;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ClientAreaEffectTracker {

	private static final Map<UUID, AreaEffectEntry> TRACKED = new LinkedHashMap<>();

	private ClientAreaEffectTracker() {
	}

	public static void onSync(AreaEffectSyncPacket packet) {
		switch (packet.action()) {
			case ADD, UPDATE -> {
				if (packet.entry() != null) TRACKED.put(packet.id(), packet.entry());
			}
			case REMOVE -> TRACKED.remove(packet.id());
		}
	}

	public static Collection<AreaEffectEntry> getTracked() {
		return TRACKED.values();
	}

	public static List<AreaEffectEntry> getAffecting(BlockPos pos) {
		ChunkPos cpos = new ChunkPos(pos);
		return TRACKED.values().stream().filter(e -> e.range.contains(cpos)).toList();
	}

	public static void clear() {
		TRACKED.clear();
	}
}

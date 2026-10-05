package dev.xkmc.gensokyolegacy.content.attachment.index;

import dev.xkmc.gensokyolegacy.content.attachment.datamap.BedData;
import dev.xkmc.gensokyolegacy.content.attachment.datamap.CharacterConfig;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IHomeHolder;
import dev.xkmc.gensokyolegacy.content.block.deco.bed.YoukaiBedBlockEntity;
import dev.xkmc.gensokyolegacy.content.entity.visit.VisitScheduler;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@SerialClass
public class StructureRefData {

	@SerialField
	private long lastBlockTickedTime = 0;
	@SerialField
	private long structureTick = 0;
	@SerialField
	private final Map<EntityType<?>, BedRefData> entities = new LinkedHashMap<>();

	public void blockTick(ServerLevel sl, BedData data, YoukaiBedBlockEntity be, StructureKey key) {
		long time = sl.getGameTime();
		if (time != lastBlockTickedTime) {
			structureTick++;
			lastBlockTickedTime = time;
			// one call per game time regardless of how many beds the structure
			// has, so this doubles as the structure's heartbeat for visits
			var home = IHomeHolder.of(sl, key);
			if (home != null) VisitScheduler.tick(sl, key, structureTick, home);
		}
		var config = CharacterConfig.of(data.type());
		if (config != null) {
			var ref = entities.computeIfAbsent(data.type(), k -> new BedRefData());
			ref.blockTick(data, config, sl, be, key);
		}
	}

	/**
	 * How many game times this structure has been ticked. Only advances while the
	 * structure is loaded, which is what makes it usable as a visit heartbeat.
	 */
	public long structureTick() {
		return structureTick;
	}

	@Nullable
	public BedRefData getEntityRef(EntityType<?> type) {
		return entities.get(type);
	}

}

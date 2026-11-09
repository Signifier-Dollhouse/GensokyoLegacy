package dev.xkmc.gensokyolegacy.content.attachment.gap;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.compat.l2lib.BaseSavedData;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.UUID;

@SerialClass
public class GapMappingData extends BaseSavedData<GapMappingData> {

	private static final String ID = GensokyoLegacy.MODID + "_gap_mapping";
	private static GapMappingData load(CompoundTag tag) {
		return TagCodec.fromTag(tag, GapMappingData.class, new GapMappingData(), f -> true);
	}

	public static GapMappingData get(ServerLevel level) {
		var ans = level.getServer().overworld().getDataStorage().computeIfAbsent(GapMappingData::load, GapMappingData::new, ID);
		ans.level = level;
		return ans;
	}

	private ServerLevel level;

	@SerialField
	public final LinkedHashMap<UUID, GapMapping> mapping = new LinkedHashMap<>();

	public GapMappingData() {
		super(GapMappingData.class);
	}

	public @Nullable GapMapping get(UUID id) {
		return mapping.get(id);
	}

	public void set(UUID id, GapMapping data) {
		var prev = mapping.put(id, data);
	}

	public void remove(UUID key) {
		mapping.remove(key);
	}

}

package dev.xkmc.gensokyolegacy.content.attachment.index;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.compat.l2lib.BaseSavedData;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import dev.xkmc.l2serial.serialization.SerialClass;
import dev.xkmc.l2serial.serialization.SerialClass.SerialField;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashMap;

@SerialClass
public class IndexStorage extends BaseSavedData<IndexStorage> {

	private static final String ID = GensokyoLegacy.MODID + "_reference_index";
	private static IndexStorage load(CompoundTag tag) {
		return TagCodec.fromTag(tag, IndexStorage.class, new IndexStorage(), f -> true);
	}

	public static IndexStorage get(ServerLevel level) {
		var ans = level.getServer().overworld().getDataStorage().computeIfAbsent(IndexStorage::load, IndexStorage::new, ID);
		ans.level = level;
		return ans;
	}

	private ServerLevel level;

	@SerialField
	private final LinkedHashMap<StructureKey, StructureRefData> structureData = new LinkedHashMap<>();

	public IndexStorage() {
		super(IndexStorage.class);
	}

	public StructureRef getOrCreate(StructureKey key) {
		return new StructureRef(this, key, structureData.computeIfAbsent(key, k -> new StructureRefData()));
	}

	public void remove(StructureKey key) {
		structureData.remove(key);
	}

}

package dev.xkmc.gensokyolegacy.content.attachment.index;

import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.l2core.capability.level.BaseSavedData;
import dev.xkmc.l2serial.serialization.codec.TagCodec;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@SerialClass
public class IndexStorage extends BaseSavedData<IndexStorage> {

	private static final String ID = GensokyoLegacy.MODID + "_reference_index";
	private static final Factory<IndexStorage> FACTORY = new Factory<>(IndexStorage::new, IndexStorage::new);

	public static IndexStorage get(ServerLevel level) {
		var ans = level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
		ans.level = level;
		return ans;
	}

	private ServerLevel level;

	@SerialField
	private final LinkedHashMap<StructureKey, StructureRefData> structureData = new LinkedHashMap<>();

	@SerialField
	private final LinkedHashMap<UUID, MissingTowerState> missingTowers = new LinkedHashMap<>();

	public IndexStorage() {
		super(IndexStorage.class);
	}

	private IndexStorage(CompoundTag tag, HolderLookup.Provider pvd) {
		super(IndexStorage.class);
		new TagCodec(pvd).fromTag(tag, IndexStorage.class, this);
	}

	public StructureRef getOrCreate(StructureKey key) {
		return new StructureRef(this, key, structureData.computeIfAbsent(key, k -> new StructureRefData()));
	}

	public void remove(StructureKey key) {
		structureData.remove(key);
	}

	public Map<UUID, MissingTowerState> towers() {
		return Collections.unmodifiableMap(missingTowers);
	}

	@Nullable
	public MissingTowerState getTower(UUID id) {
		return missingTowers.get(id);
	}

	/**
	 * Rolls a tower for this player, or returns the existing one unchanged.
	 */
	public MissingTowerState createTower(UUID id, ResourceLocation quest, ServerLevel level, BlockPos origin) {
		return missingTowers.computeIfAbsent(id, k -> MissingTowerManager.create(quest, level, origin));
	}

	public void putTower(UUID id, MissingTowerState state) {
		missingTowers.put(id, state);
	}

	public void removeTower(UUID id) {
		missingTowers.remove(id);
	}

}

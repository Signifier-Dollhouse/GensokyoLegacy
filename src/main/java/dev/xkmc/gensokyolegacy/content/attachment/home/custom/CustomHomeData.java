package dev.xkmc.gensokyolegacy.content.attachment.home.custom;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.BlockSearchCache;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.IBlockSearchCache;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.MultiStructureBound;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureInfoUpdateToClient;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

@SerialClass
public class CustomHomeData implements IBlockSearchCache {

	@SerialField
	private BlockPos rootPos;
	@SerialField
	protected RoomData room;

	@SerialField
	private final LinkedHashMap<HomeBlockKind, BlockSearchCache> blocks = new LinkedHashMap<>();

	public boolean checkInit(CustomHomeHolder holder) {
		return rootPos != null && room != null;
	}

	public void setData(BlockPos pos, RoomData box) {
		rootPos = pos;
		room = box;
	}

	public void tick(CustomHomeHolder holder) {
		if (rootPos == null || room == null) {
		}
	}

	public BlockPos getRoot() {
		return rootPos;
	}

	public BoundingBox getRoomBound() {
		return room.bound;
	}

	public BoundingBox getHouseBound() {
		return room.bound.inflatedBy(1);
	}

	public BoundingBox getTotalBound() {
		return room.bound.inflatedBy(1);
	}

	public boolean isOutside(Level level, BlockPos ans) {
		return level.canSeeSky(ans);
	}

	@Override
	public BlockSearchCache cache(HomeBlockKind kind) {
		return blocks.computeIfAbsent(kind, k -> new BlockSearchCache());
	}

	@Nullable
	public BlockPos getBlockAround(HomeBlockKind kind, CustomHomeHolder holder, BlockPos center) {
		return HomeSearchUtil.searchKind(this, kind,
				MultiStructureBound.of(getRoomBound()), holder.level(), center);
	}

	public StructureInfoUpdateToClient getAbnormality(StructureKey key) {
		return new StructureInfoUpdateToClient(key, -1, -1, -1);
	}

}

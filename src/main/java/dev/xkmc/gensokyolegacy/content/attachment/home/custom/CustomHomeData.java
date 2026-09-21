package dev.xkmc.gensokyolegacy.content.attachment.home.custom;

import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeBlockKind;
import dev.xkmc.gensokyolegacy.content.attachment.home.core.HomeSearchUtil;
import dev.xkmc.gensokyolegacy.content.attachment.index.StructureKey;
import dev.xkmc.gensokyolegacy.content.client.structure.StructureInfoUpdateToClient;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@SerialClass
public class CustomHomeData {

	@SerialField
	private BlockPos rootPos;
	@SerialField
	protected RoomData room;

	@SerialField
	private final LinkedHashMap<HomeBlockKind, ArrayList<BlockPos>> blocks = new LinkedHashMap<>();

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

	public List<BlockPos> cache(HomeBlockKind kind) {
		return blocks.computeIfAbsent(kind, k -> new ArrayList<>());
	}

	@Nullable
	public BlockPos getBlockAround(HomeBlockKind kind, CustomHomeHolder holder, BlockPos center, int rxz, int ry, int trail) {
		return HomeSearchUtil.searchBlock(cache(kind), kind.validator(),
				getRoomBound(), holder.level(), center, rxz, ry, trail);
	}

	@Nullable
	public BlockPos getContainerAround(CustomHomeHolder holder, BlockPos center, int rxz, int ry, int trail) {
		return getBlockAround(HomeBlockKind.CONTAINER, holder, center, rxz, ry, trail);
	}

	@Nullable
	public BlockPos getChairAround(CustomHomeHolder holder, BlockPos center, int rxz, int ry, int trail) {
		return getBlockAround(HomeBlockKind.CHAIR, holder, center, rxz, ry, trail);
	}

	@Nullable
	public BlockPos getShelfAround(CustomHomeHolder holder, BlockPos center, int rxz, int ry, int trail) {
		return getBlockAround(HomeBlockKind.SHELF, holder, center, rxz, ry, trail);
	}

	public StructureInfoUpdateToClient getAbnormality(StructureKey key) {
		return new StructureInfoUpdateToClient(key, -1, -1, -1);
	}

}

package dev.xkmc.gensokyolegacy.content.attachment.index;

import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One player's missing tower. The entity is only a view of this record.
 *
 * @see "doc/design/quest/missing_tower.md"
 */
@SerialClass
public class MissingTowerState {

	@SerialField
	private ResourceLocation quest;
	@SerialField
	private ResourceLocation dim;
	@SerialField
	private BlockPos pos = BlockPos.ZERO;
	@SerialField
	private BlockPos origin = BlockPos.ZERO;
	@SerialField
	private boolean carried;
	@SerialField
	@Nullable
	private UUID entity;
	@SerialField
	private long updated;

	public MissingTowerState() {
	}

	MissingTowerState(ResourceLocation quest, ResourceLocation dim, BlockPos pos, BlockPos origin) {
		this.quest = quest;
		this.dim = dim;
		this.pos = pos;
		this.origin = origin;
	}

	public ResourceLocation quest() {
		return quest;
	}

	public ResourceLocation dim() {
		return dim;
	}

	public BlockPos pos() {
		return pos;
	}

	public BlockPos origin() {
		return origin;
	}

	public boolean carried() {
		return carried;
	}

	@Nullable
	public UUID entity() {
		return entity;
	}

	public long updated() {
		return updated;
	}

	void setQuest(ResourceLocation quest) {
		this.quest = quest;
	}

	void setGround(BlockPos pos, ResourceLocation dim, long time) {
		this.pos = pos;
		this.dim = dim;
		this.carried = false;
		this.entity = null;
		this.updated = time;
	}

	void setCarried(ResourceLocation dim, BlockPos pos, long time) {
		this.dim = dim;
		this.pos = pos;
		this.carried = true;
		this.updated = time;
	}

	void setEntity(@Nullable UUID entity) {
		this.entity = entity;
	}

	void setPos(BlockPos pos) {
		this.pos = pos;
	}

}

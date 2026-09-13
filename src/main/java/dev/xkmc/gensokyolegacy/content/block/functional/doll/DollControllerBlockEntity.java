package dev.xkmc.gensokyolegacy.content.block.functional.doll;

import dev.xkmc.gensokyolegacy.content.attachment.doll.DollData;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollHost;
import dev.xkmc.gensokyolegacy.content.attachment.doll.DollState;
import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.l2core.base.tile.BaseBlockEntity;
import dev.xkmc.l2modularblock.core.BlockTemplates;
import dev.xkmc.l2modularblock.tile_api.TickableBlockEntity;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A doll controller block — a block that hosts its own resident {@link DollData} and acts as the
 * {@link DollHost} for deployed {@link BaseDollEntity} instances (doc/design/doll/controller.md).
 * <p>
 * Pairing is identical to the player ledger (§8.2, §5.8): each DollData key is the deployed
 * entity's own game uuid (fresh on every materialization, never stored on item or entity), the
 * entry is inserted BEFORE {@code addFreshEntity}, and the entity's inverse check resolves its
 * host through {@link BaseDollEntity#getHost()} — a player-owned doll looks up the player
 * capability, a block-hosted doll looks up this block entity.
 * <p>
 * Unlike the player ledger the block keeps an ordered <b>list</b> of resident dolls (up to
 * {@link #MAX_DOLLS}); installed dolls are STORED (no uuid yet), deployed ones are keyed by the
 * live entity's uuid and SUMMONED:
 * <ul>
 *   <li>STORED — the doll rests inside the block (installed from a doll item, or recalled).</li>
 *   <li>SUMMONED — the doll is materialized and hovers in front of the block; its values are
 *       continuously written back into its {@link DollData}, and a vanished entity is respawned
 *       from the recorded values (a dead or invalid one reverts to STORED).</li>
 * </ul>
 * The block is never "owned" by a player: the deployed doll has no owner, so the follow/look goals
 * stay dormant (§13.4). Future work tasks (e.g. mining) will attach worker goals to the deployed
 * entity via {@link #getDeployedDoll()}.
 */
@SerialClass
public class DollControllerBlockEntity extends BaseBlockEntity implements TickableBlockEntity, DollHost {

	/** Max resident dolls a single controller block can hold. */
	public static final int MAX_DOLLS = 4;

	@SerialField
	public final List<DollData> dolls = new ArrayList<>();

	public DollControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	// ---------- host queries (called by the deployed entity and the player interact) ----------

	@Override
	@Nullable
	public DollData findSummoned(UUID uuid) {
		if (uuid == null) return null;
		for (var data : dolls) {
			if (data != null && data.isSummoned() && uuid.equals(data.uuid)) return data;
		}
		return null;
	}

	@Override
	@Nullable
	public DollData detach(UUID uuid) {
		DollData data = findSummoned(uuid);
		if (data == null) return null;
		dolls.remove(data);
		return data;
	}

	@Override
	public void update(BaseDollEntity entity) {
		if (!entity.isHomedTo(worldPosition)) return;
		DollData data = findSummoned(entity.getUUID());
		if (data == null) return;
		entity.writeValuesTo(data);
		setChanged();
	}

	// ---------- resident management (called by the block methods) ----------

	public boolean hasDoll() {
		return !dolls.isEmpty();
	}

	public boolean hasSpace() {
		return dolls.size() < MAX_DOLLS;
	}

	public boolean isSummoned() {
		for (var data : dolls) {
			if (data != null && data.isSummoned()) return true;
		}
		return false;
	}

	/**
	 * Installs a doll item into a controller with free space. The item is consumed; the doll
	 * becomes one of the block's residents (STORED). Item -> stored data conversion mirrors
	 * {@code DollAttachment.summon} (via {@link DollData#fromItemData}): significant values
	 * only, no uuid. The initial spot is the center of the block directly above the controller,
	 * so a deployed doll naturally hovers right above the block.
	 */
	public boolean install(ItemStack stack) {
		if (!hasSpace()) return false;
		DollData data = DollData.fromItemData(stack, worldPosition.above().getCenter(), level.dimension().location(), 0);
		if (data.combat.amount() <= 0) return false;
		data.state = DollState.STORED;
		dolls.add(data);
		setChanged();
		sync();
		return true;
	}

	/**
	 * Deploy the next stored resident (STORED -> SUMMONED). Creates a fresh entity of the
	 * recorded type, keys the doll entry by its new game uuid BEFORE {@code addFreshEntity}, and
	 * homes the entity at this block so its inverse check finds this host (§8.2).
	 */
	public boolean deploy() {
		if (!(level instanceof ServerLevel)) return false;
		DollData found = null;
		for (var d : dolls) {
			if (d != null && d.state == DollState.STORED) {
				found = d;
				break;
			}
		}
		if (found == null || found.type == null || found.getHealth() <= 0) return false;
		found.yRot = getFacing().toYRot();
		if (!spawnFromData(found, getDeployPos())) return false;
		setChanged();
		sync();
		return true;
	}

	/**
	 * Materializes a resident doll entry as a live entity homed to this block and re-keys the
	 * entry to the entity's fresh game uuid BEFORE {@code addFreshEntity}, so the join-level
	 * inverse check finds its SUMMONED entry (§8.2). The block's own sync is left to the caller.
	 */
	private boolean spawnFromData(DollData data, Vec3 pos) {
		if (!(level instanceof ServerLevel sl) || data.type == null) return false;
		EntityType<?> type = sl.registryAccess().registry(Registries.ENTITY_TYPE)
				.flatMap(r -> r.getOptional(ResourceKey.create(Registries.ENTITY_TYPE, data.type))).orElse(null);
		if (type == null) return false;
		Entity ent = type.create(sl);
		if (!(ent instanceof BaseDollEntity be)) {
			if (ent != null) ent.discard();
			return false;
		}
		data.uuid = be.getUUID();
		data.state = DollState.SUMMONED;
		data.dimension = sl.dimension().location();
		data.position = pos;
		be.setHome(worldPosition);
		be.readValuesFrom(data);
		sl.addFreshEntity(be);
		return true;
	}

	/**
	 * Recall one deployed doll into the block (SUMMONED -> STORED). Values are written back before
	 * the entity is discarded, so the resident data always tracks the last materialized form.
	 */
	public boolean recall() {
		if (!(level instanceof ServerLevel sl)) return false;
		DollData data = null;
		for (var d : dolls) {
			if (d != null && d.isSummoned() && d.uuid != null) {
				data = d;
				break;
			}
		}
		if (data == null) return false;
		if (sl.getEntity(data.uuid) instanceof BaseDollEntity be) {
			be.writeValuesTo(data);
			be.discard();
		}
		data.state = DollState.STORED;
		setChanged();
		sync();
		return true;
	}

	/**
	 * Remove one resident doll from the block and hand its item to the player. A deployed doll is
	 * recalled first (values written back, entity discarded); otherwise the first installed one is
	 * reclaimed. Deploys nothing.
	 */
	public boolean eject(ServerPlayer player) {
		DollData data = null;
		for (var d : dolls) {
			if (d != null && d.isSummoned() && d.uuid != null) {
				data = d;
				break;
			}
		}
		if (data == null) {
			for (var d : dolls) {
				if (d != null && d.state == DollState.STORED) {
					data = d;
					break;
				}
			}
		}
		if (data == null || data.type == null) return false;
		if (data.isSummoned() && level instanceof ServerLevel sl && data.uuid != null) {
			if (sl.getEntity(data.uuid) instanceof BaseDollEntity be) {
				be.writeValuesTo(data);
				be.discard();
			}
			data.state = DollState.STORED;
		}
		ItemStack stack = DollItem.makeItem(data);
		dolls.remove(data);
		player.getInventory().placeItemBackInInventory(stack);
		setChanged();
		sync();
		return true;
	}

	/**
	 * The block was broken: never lose a doll. A deployed doll's entity is discarded and its item
	 * dropped where the doll currently is; a summoned entry with no live entity is reclaimed at
	 * its last recorded spot if known, and every stored doll drops at the block position.
	 */
	public void onDestroy() {
		if (level == null || level.isClientSide() || dolls.isEmpty()) return;
		ServerLevel sl = level instanceof ServerLevel s ? s : null;
		for (var data : dolls) {
			if (data == null) continue;
			Vec3 dropPos = worldPosition.getCenter();
			if (data.isSummoned()) {
				if (data.uuid != null && sl != null && sl.getEntity(data.uuid) instanceof BaseDollEntity be) {
					be.writeValuesTo(data);
					be.discard();
					dropPos = be.position();
				} else if (data.position != null) {
					dropPos = data.position;
				}
				data.state = DollState.STORED;
			}
			if (data.type != null) {
				ItemStack stack = DollItem.makeItem(data);
				if (!stack.isEmpty()) {
					Block.popResource(level, BlockPos.containing(dropPos), stack);
				}
			}
		}
		dolls.clear();
	}

	// ---------- deployment config (the seam for future tasks such as mining) ----------

	/**
	 * The first currently materialized doll, if any. Future task goals (mining etc.) attach their
	 * logic here.
	 */
	@Nullable
	public BaseDollEntity getDeployedDoll() {
		if (!(level instanceof ServerLevel sl)) return null;
		for (var data : dolls) {
			if (data == null || !data.isSummoned() || data.uuid == null) continue;
			Entity e = sl.getEntity(data.uuid);
			if (e instanceof BaseDollEntity be && be.isHomedTo(worldPosition)) return be;
		}
		return null;
	}

	private Direction getFacing() {
		return getBlockState().getValue(BlockTemplates.HORIZONTAL_FACING);
	}

	private Vec3 getDeployPos() {
		Direction dir = getFacing();
		return worldPosition.getCenter().add(dir.getStepX() * 1.5, 0.5, dir.getStepZ() * 1.5);
	}

	// ---------- tick ----------

	@Override
	public void tick() {
		if (level == null || level.isClientSide()) return;
		// dolls are never chunk-serialized (§5.8): keep deployed entries in sync or respawn a
		// vanished summoned doll from its recorded values. Only an unrespawnable entry (bad type
		// / dead) reverts to STORED.
		if (level.getGameTime() % 20 != 0) return;
		ServerLevel sl = (ServerLevel) level;
		boolean changed = false;
		for (var data : dolls) {
			if (data == null || !data.isSummoned() || data.uuid == null) continue;
			Entity e = sl.getEntity(data.uuid);
			if (e instanceof BaseDollEntity be && be.isHomedTo(worldPosition)) {
				be.writeValuesTo(data);
				changed = true;
			} else {
				if (e != null) e.discard();
				Vec3 pos = data.position != null ? data.position : getDeployPos();
				if (data.type != null && data.getHealth() > 0 && spawnFromData(data, pos)) {
					changed = true;
				} else {
					data.state = DollState.STORED;
					changed = true;
				}
			}
		}
		if (changed) {
			setChanged();
			sync();
		}
	}

	// ---------- lifecycle ----------

	@Override
	public void onLoad() {
		super.onLoad();
		if (level == null || level.isClientSide() || dolls.isEmpty()) return;
		// the deployed entity is never saved: on reload a SUMMONED entry has no live entity,
		// revert it to STORED and let the owner redeploy.
		boolean changed = false;
		for (var data : dolls) {
			if (data != null && data.isSummoned()) {
				data.state = DollState.STORED;
				changed = true;
			}
		}
		if (changed) setChanged();
	}

}
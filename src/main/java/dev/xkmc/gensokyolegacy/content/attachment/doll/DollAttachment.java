package dev.xkmc.gensokyolegacy.content.attachment.doll;

import dev.xkmc.gensokyolegacy.content.entity.dolls.BaseDollEntity;
import dev.xkmc.gensokyolegacy.content.item.doll.DollItem;
import dev.xkmc.gensokyolegacy.init.GensokyoLegacy;
import dev.xkmc.gensokyolegacy.init.data.GLLang;
import dev.xkmc.l2core.capability.player.PlayerCapabilityTemplate;
import dev.xkmc.l2serial.serialization.marker.SerialClass;
import dev.xkmc.l2serial.serialization.marker.SerialField;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SerialClass
public class DollAttachment extends PlayerCapabilityTemplate<DollAttachment> implements DollLedger {

	/**
	 * A recorded restore position is trusted only while within this many blocks of the owner;
	 * beyond that (stale, tampered, recorded before a long run) the doll is restored near the
	 * owner instead of at the recorded spot.
	 */
	private static final double RESTORE_TRUST_RADIUS = 10.0;

	/**
	 * Maximum summoned dolls per player. Stored dolls (data, items) are uncapped —
	 * only materialized ones count.
	 */
	public static final int MAX_SUMMONED = 8;

	/**
	 * How close a stray must be for the glove recall to reach it (control.md
	 * §5.4) — the glove's target range, so it matches what the holder can
	 * actually see. Strays further out are left to the idle rejoin.
	 */
	public static final double STRAY_RECALL_RANGE = 48.0;

	@SerialField
	private final Map<UUID, DollData> dolls = new LinkedHashMap<>();

	/**
	 * Player-facing command logic: volley / one-time / stop, iterative handoff,
	 * auto-heal, heal marks. Server-only command state lives here, never persisted.
	 */
	private final DollCommander commander = new DollCommander(this);

	@Override
	public DollCommander commands() {
		return commander;
	}

	/**
	 * Sidebar roster (summoned dolls in ledger order) mirrored to the owner's
	 * client via {@code DollRosterToClient}. Never serialized (no
	 * {@code SerialField}). Dual role, split by side — server and client
	 * instances are separate objects, so one field serves both: on the server
	 * it is the last-pushed roster (diff baseline for {@code maybePushRoster});
	 * on the client it is the last-received roster (read by the sidebar). The
	 * client instance dies with the client player, so no clearing is needed.
	 */
	private List<DollRosterToClient.Entry> roster = List.of();

	public List<DollRosterToClient.Entry> getRoster() {
		return roster;
	}

	public void setRoster(List<DollRosterToClient.Entry> roster) {
		this.roster = List.copyOf(roster);
	}

	@Override
	public Collection<DollData> dolls() {
		return dolls.values();
	}

	// ---------- queries ----------

	public DollData get(UUID uuid) {
		return dolls.get(uuid);
	}

	/**
	 * Summoned entries in ledger order. Server-side only: clients read the
	 * sidebar roster from the client {@code DollAttachment} instance instead.
	 */
	public List<DollData> summonedDolls() {
		List<DollData> out = new ArrayList<>();
		for (DollData data : dolls.values()) {
			if (data.isSummoned()) out.add(data);
		}
		return out;
	}

	@Nullable
	public DollData findSummoned(UUID uuid) {
		if (uuid == null) return null;
		DollData data = dolls.get(uuid);
		return data != null && data.isSummoned() ? data : null;
	}

	public void update(BaseDollEntity doll) {
		UUID uuid = doll.getUUID();
		DollData data = dolls.get(uuid);
		if (data == null || !data.isSummoned()) return;
		doll.writeValuesTo(data);
	}

	@Override
	@Nullable
	public DollData detach(UUID uuid) {
		if (uuid == null) return null;
		DollData data = dolls.get(uuid);
		if (data == null || !data.isSummoned()) return null;
		dolls.remove(uuid);
		return data;
	}

	// ---------- item -> entity ----------
	// The ledger key is the entity's own game UUID: the game mints a fresh one for
	// every materialization, it is never stored on the item, and it survives
	// dimension changes. A cloned spare item therefore summons an independent doll
	// (fresh uuid), and the entity is always findable via level().getEntity(uuid).

	/** Summoned entries on this ledger. */
	public int summonedCount() {
		int n = 0;
		for (DollData data : dolls.values()) {
			if (data.isSummoned()) n++;
		}
		return n;
	}

	public boolean summonCapped() {
		return summonedCount() >= MAX_SUMMONED;
	}

	/**
	 * Item -> entity. Broken (0-health) dolls stay items — repair them in an anvil
	 * with wool first. Respects the per-player summoned cap.
	 */
	public boolean summon(ServerPlayer player, ItemStack stack, Vec3 pos) {
		DollData data = DollData.fromItemData(stack, pos, player.level().dimension().location(), player.getYRot());
		if (data.getHealth() <= 0 || summonCapped()) return false;
		data.state = DollState.SUMMONED;
		return doSummon(player, data, null);
	}

	// ---------- entity -> item (atomic) ----------

	public boolean itemize(ServerPlayer player, BaseDollEntity doll) {
		UUID uuid = doll.getUUID();
		DollData data = dolls.get(uuid);
		if (data == null || !data.isSummoned()) return false;
		if (!hasSlotFor(player)) {
			player.displayClientMessage(GLLang.Doll.NO_SPACE.get(), true);
			return false;
		}
		doll.writeValuesTo(data);
		ItemStack stack = DollItem.makeItem(data);
		dolls.remove(uuid);
		if (player.getMainHandItem().isEmpty())
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		else player.getInventory().placeItemBackInInventory(stack);
		doll.discard();
		return true;
	}

	// ---------- entity -> parked data ----------

	public boolean park(ServerPlayer player, BaseDollEntity doll, DollState intent) {
		if (intent == DollState.SUMMONED) return false;
		UUID uuid = doll.getUUID();
		DollData data = dolls.get(uuid);
		if (data == null || !data.isSummoned()) return false;
		doll.writeValuesTo(data);
		data.state = intent;
		if (doll.level() instanceof ServerLevel sl) data.lastUpdate = sl.getGameTime();
		doll.discard();
		return true;
	}

	/**
	 * Stray rejoin (control.md §5.4): a suicide dive that aborted before
	 * detonation is not a death, so its detached entry goes back on the ledger
	 * parked TEMP — the next tick's TEMP pass resummons the doll near its owner,
	 * health and gear untouched. The stray entity is discarded instead of killed,
	 * so the death drop never fires. False when the doll is not stray, already
	 * handed its entry to the death drop, is dying, or its key is taken — the
	 * caller then leaves it a stray.
	 */
	public boolean rejoin(BaseDollEntity doll) {
		StrayHost stray = doll.strayHost();
		if (stray == null || doll.isDeadOrDying()) return false;
		DollData data = stray.data();
		if (data == null || data.uuid == null || data.type == null) return false;
		if (dolls.containsKey(data.uuid)) return false;
		stray.take();
		doll.setStrayHost(null);
		doll.writeValuesTo(data);
		data.state = DollState.TEMP;
		dolls.put(data.uuid, data);
		doll.discard();
		return true;
	}

	// ---------- glove mass operations ----------

	/** Any doll currently summoned on this ledger. */
	public boolean hasSummoned() {
		for (var data : dolls.values()) {
			if (data.isSummoned()) return true;
		}
		return false;
	}

	/**
	 * Glove summon-all: materialize every parked entry (STORED entries are
	 * promoted to TEMP first — both are just parked data) plus every doll item
	 * in the player's inventory, in a ring around the owner. There is no ledger
	 * cap; as much as possible means everything. Returns dolls summoned.
	 */
	public int summonAll(ServerPlayer player) {
		int n = 0;
		for (var data : new ArrayList<>(dolls.values())) {
			if (summonCapped()) break;
			if (data.isSummoned() || data.type == null) continue;
			if (data.state == DollState.STORED) data.state = DollState.TEMP;
			if (data.state == DollState.TEMP && trySummon(player, data)) n++;
		}
		var inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			if (summonCapped()) break;
			ItemStack stack = inv.getItem(i);
			if (stack.isEmpty() || !(stack.getItem() instanceof DollItem)) continue;
			double a = (i * 2 * Math.PI) / inv.getContainerSize();
			Vec3 pos = player.position().add(Math.cos(a) * 1.5, 1, Math.sin(a) * 1.5);
			if (summon(player, stack, pos)) {
				stack.shrink(1);
				inv.setItem(i, stack);
				n++;
			}
		}
		return n;
	}

	/**
	 * Glove recall for strays (control.md §5.4): itemize every stray of this
	 * ledger within {@code range} blocks of the owner, same result as recalling
	 * a summoned one — the item form lands in the player's inventory. A stray
	 * holds no ledger entry, so this scans loaded entities for this player's own
	 * stray dolls rather than walking the map. Stops at the first full-inventory
	 * refusal like {@link #itemize} does; strays past {@code range} are left
	 * alone, and the idle rejoin ({@code DollStray}) is the fallback for those.
	 * Returns dolls itemized.
	 */
	public int recallStrays(ServerPlayer player, double range) {
		AABB box = player.getBoundingBox().inflate(range);
		List<Entity> strays = player.level().getEntities(player, box,
				e -> e instanceof BaseDollEntity doll && doll.isStray() && doll.isOwner(player));
		int n = 0;
		for (var e : strays) {
			if (!hasSlotFor(player)) {
				player.displayClientMessage(GLLang.Doll.NO_SPACE.get(), true);
				break;
			}
			if (itemizeStray(player, (BaseDollEntity) e)) n++;
		}
		return n;
	}

	/**
	 * Stray counterpart of {@link #itemize}: the detached entry rides a fresh
	 * doll item into the player's inventory and the stray entity is discarded.
	 * The slot is the caller's gate — {@link #placeItemBackInInventory} drops
	 * whatever does not fit, and the doll must not be lost to a full inventory.
	 */
	private boolean itemizeStray(ServerPlayer player, BaseDollEntity doll) {
		StrayHost stray = doll.strayHost();
		if (stray == null || doll.isDeadOrDying()) return false;
		DollData data = stray.take();
		if (data == null) return false;
		doll.setStrayHost(null);
		doll.writeValuesTo(data);
		player.getInventory().placeItemBackInInventory(DollItem.makeItem(data));
		doll.discard();
		return true;
	}

	/**
	 * Glove recall-all: itemize every reachable summoned doll, parking to STORED
	 * when the inventory is full; then consolidate all TEMP into STORED, so
	 * everything parked ends up as data. Strays within {@link #STRAY_RECALL_RANGE}
	 * blocks ride the same sweep as itemized dolls (§5.4). Player-ledger only —
	 * block-hosted dolls never reach here. Returns {itemized, parked}.
	 */
	public int[] recallAll(ServerPlayer player) {
		int itemized = recallStrays(player, STRAY_RECALL_RANGE), parked = 0;
		for (var data : new ArrayList<>(dolls.values())) {
			if (!data.isSummoned() || data.uuid == null) continue;
			ServerLevel level = getLevel(player, data);
			if (!(level != null && level.getEntity(data.uuid) instanceof BaseDollEntity doll)) continue;
			if (itemize(player, doll)) itemized++;
			else if (park(player, doll, DollState.STORED)) parked++;
		}
		for (var data : dolls.values()) {
			if (data.state == DollState.TEMP) data.state = DollState.STORED;
		}
		return new int[]{itemized, parked};
	}

	// ---------- deferred transitions ----------

	public boolean tryItemize(ServerPlayer player, DollData data) {
		if (data.state != DollState.STORED || data.type == null) return false;
		ItemStack stack = DollItem.makeItem(data);
		if (!hasSlotFor(player)) return false;
		dolls.remove(data.uuid);
		player.getInventory().placeItemBackInInventory(stack);
		return true;
	}

	public boolean trySummon(ServerPlayer player, DollData data) {
		if (data.state != DollState.TEMP || data.type == null) return false;
		if (summonCapped()) return false;
		if (data.getHealth() <= 0) {
			data.state = DollState.STORED;
			return tryItemize(player, data);
		}
		Vec3 target = getSpawnTarget(player, data);
		if (target == null) return false;
		data.position = target;
		data.state = DollState.SUMMONED;
		if (!doSummon(player, data, data.uuid)) {
			data.state = DollState.TEMP;
			return false;
		}
		return true;
	}

	// ---------- lifecycle ----------

	public void restore(ServerPlayer player) {
		for (var data : new ArrayList<>(dolls.values())) {
			if (data.state == DollState.STORED) tryItemize(player, data);
			else if (data.state == DollState.TEMP) trySummon(player, data);
		}
	}

	public void onLogout(ServerPlayer player) {
		parkAll(player);
	}

	public void onPlayerDeath(ServerPlayer player) {
		parkAll(player);
	}

	// ---------- tick ----------

	@Override
	public void tick(Player player) {
		if (player.level().isClientSide()) return;
		if (!(player instanceof ServerPlayer sp)) return;
		for (var data : new ArrayList<>(dolls.values())) {
			switch (data.state) {
				case STORED -> tryItemize(sp, data);
				case TEMP -> trySummon(sp, data);
				case SUMMONED -> tickSummoned(sp, data);
			}
		}
		commander.tick(sp);
		maybePushRoster(sp);
	}

	/**
	 * Sidebar roster push (glove.md §2c): rebuild the summoned roster in ledger
	 * order and send {@code DollRosterToClient} only when it changed. The
	 * capability itself syncs nothing to clients (all {@code DollData} fields
	 * are {@code toClient = false}); per-doll display state rides the doll
	 * entity's synced data instead.
	 *
	 * The roster pass also restamps each live entry's formation slot, which the
	 * follow goal reads instead of scanning the ledger.
	 */
	private void maybePushRoster(ServerPlayer player) {
		var built = commander.roster(player);
		ArrayList<DollRosterToClient.Entry> next = new ArrayList<>(built.size());
		for (DollCommander.RosterEntry entry : built) {
			next.add(new DollRosterToClient.Entry(entry.entityId(), entry.uuid()));
		}
		if (!next.equals(roster)) {
			setRoster(next);
			GensokyoLegacy.HANDLER.toClientPlayer(new DollRosterToClient(next), player);
		}
	}

	// ---------- internals ----------

	private void parkAll(ServerPlayer player) {
		for (var data : new ArrayList<>(dolls.values())) {
			if (!data.isSummoned()) continue;
			ServerLevel level = getLevel(player, data);
			if (level != null && level.getEntity(data.uuid) instanceof BaseDollEntity doll) {
				doll.writeValuesTo(data);
				doll.discard();
			}
			// entity not reachable: dolls are never chunk-serialized (§5.8), so an unloaded
			// doll is gone, not pending on disk — flip to TEMP safely.
			data.state = DollState.TEMP;
		}
	}

	/**
	 * SUMMONED per-tick watchdog. A dead entry (no health) is STORED, never
	 * re-summoned. A doll whose entry no longer matches reality (wrong type,
	 * vanished entity) or that drifted past {@link #PULLBACK_DISTANCE} from its
	 * owner is discarded and parked TEMP; the next tick's TEMP branch summons it
	 * immediately, near the owner unless the recorded spot is still inside the
	 * trust circle.
	 */
	private void tickSummoned(ServerPlayer player, DollData data) {
		ServerLevel level = getLevel(player, data);
		if (level == null) {
			data.state = DollState.TEMP;
			return;
		}
		if (data.getHealth() <= 0) {
			data.state = DollState.STORED;
			if (level.getEntity(data.uuid) instanceof BaseDollEntity corpse) {
				corpse.discard();
			}
			return;
		}
		Entity e = level.getEntity(data.uuid);
		if (e instanceof BaseDollEntity doll) {
			if (!matches(doll, player, data)) {
				GensokyoLegacy.LOGGER.warn(GLLang.Doll.RESYNC_TAMPERED.get().getString());
				doll.discard();
				data.state = DollState.TEMP;
			} else if (doll.level() == player.level() && doll.distanceTo(player) >= PULLBACK_DISTANCE) {
				doll.writeValuesTo(data);
				doll.discard();
				data.state = DollState.TEMP;
			}
		} else {
			GensokyoLegacy.LOGGER.warn(GLLang.Doll.RESYNC_MISSING.get().getString());
			data.state = DollState.TEMP;
		}
	}

	private boolean matches(BaseDollEntity doll, Player owner, DollData data) {
		return data.type != null && data.type.equals(doll.getDollTypeId()) && doll.isOwner(owner);
	}

	private boolean hasSlotFor(ServerPlayer player) {
		return player.getAbilities().instabuild || player.getInventory().getFreeSlot() >= 0;
	}

	@Nullable
	ServerLevel getLevel(ServerPlayer player, DollData data) {
		if (data.dimension == null) return null;
		return player.serverLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, data.dimension));
	}

	@Nullable
	private Vec3 getSpawnTarget(ServerPlayer player, DollData data) {
		if (data.dimension != null && data.dimension.equals(player.level().dimension().location()) && data.position != null) {
			BlockPos pos = BlockPos.containing(data.position);
			// the recorded position is trustable only inside the owner's 10-block circle;
			// farther than that it is stale or tampered with, so summon near the owner.
			if (player.serverLevel().isLoaded(pos) && data.position.distanceTo(player.position()) <= RESTORE_TRUST_RADIUS) {
				return data.position;
			}
		}
		return player.position().add(0, 1, 0);
	}

	// ---- spawn + register: the DollData entry is inserted BEFORE addFreshEntity,
	// so the entity join-level inverse check finds its SUMMONED entry (§5.8, §8.2).
	// oldKey != null re-keys a TEMP entry to the new entity's fresh uuid (§6.5).

	private boolean doSummon(ServerPlayer player, DollData data, @Nullable UUID oldKey) {
		BaseDollEntity doll = DollSpawn.materialize(player.serverLevel(), player, data);
		if (doll == null) return false;
		if (oldKey != null) dolls.remove(oldKey);
		dolls.put(data.uuid, data);
		player.serverLevel().addFreshEntity(doll);
		return true;
	}

}
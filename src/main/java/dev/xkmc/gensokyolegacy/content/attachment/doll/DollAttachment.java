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

/**
 * Player capability ledger: a doll is always exactly one of {entity + {@link DollData}, item, parked data}.
 * Entries whose {@link DollNature} forbids an item form are never converted, recalled or loaded out —
 * they are dropped instead (timed_summon.md §3).
 *
 * @see "doc/design/doll/pairing.md"
 */
@SerialClass
public class DollAttachment extends PlayerCapabilityTemplate<DollAttachment> implements DollLedger {

	/**
	 * A recorded restore position is trusted only inside this radius of the owner; beyond it the
	 * doll is restored near the owner instead (stale, tampered, or recorded before a long run).
	 */
	private static final double RESTORE_TRUST_RADIUS = 10.0;

	/**
	 * Maximum summoned dolls per player. Stored dolls (data, items) are uncapped —
	 * only materialized ones count.
	 */
	public static final int MAX_SUMMONED = 8;

	/**
	 * How close a stray must be for the glove recall to reach it: the glove's own target range, so
	 * the sweep matches what the holder can see. Strays further out wait for the idle rejoin.
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

	/** Sidebar roster in ledger order, pushed via {@code DollRosterToClient}, never serialized.
	 * One field, both sides: the server's diff baseline and the client's last roster. */
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

	@Override
	@Nullable
	public DollData drop(UUID uuid) {
		return uuid == null ? null : dolls.remove(uuid);
	}

	// ---------- item -> entity ----------
	// The ledger key is the entity's own game UUID, minted fresh per materialization: a cloned
	// spare summons an independent doll, findable via level().getEntity(uuid) (pairing.md §2.2).

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
		// a timed summon's kind has no item form, so there is nothing here to convert it into
		if (!data.getNature().itemizable()) return false;
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
		// STORED means "wants to be an item", which a timed summon never does; TEMP still does,
		// since it only means "wants to be an entity again"
		if (intent == DollState.STORED && !data.getNature().itemizable()) return false;
		doll.writeValuesTo(data);
		data.state = intent;
		if (doll.level() instanceof ServerLevel sl) data.lastUpdate = sl.getGameTime();
		doll.discard();
		return true;
	}

	/** Stray rejoin (control.md §5.4a): an aborted suicide dive is not a death, so the detached
	 * entry goes back parked TEMP, health and gear untouched, and the stray is discarded rather
	 * than killed so no death drop fires. False unless stray, alive, and its key is free. */
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

	/** Glove summon-all: every parked entry (STORED promoted to TEMP) plus every doll item in the
	 * inventory, in a ring around the owner. No ledger cap: as much as possible means everything.
	 * Returns the dolls summoned. */
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

	/** Glove recall for strays (control.md §5.4a): itemize every stray within {@code range} blocks,
	 * as recalling a summoned one does. A stray holds no entry, so this scans loaded entities; a
	 * full inventory stops the sweep. Returns the dolls itemized. */
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

	/** Stray counterpart of {@link #itemize}: the detached entry rides a fresh doll item into the
	 * inventory and the stray is discarded; the caller's slot check is the only gate. */
	private boolean itemizeStray(ServerPlayer player, BaseDollEntity doll) {
		StrayHost stray = doll.strayHost();
		if (stray == null || doll.isDeadOrDying()) return false;
		DollData data = stray.data();
		// asked before the one-shot take: a refused sweep must not spend a timed summon's entry
		if (data == null || !data.getNature().itemizable()) return false;
		stray.take();
		doll.setStrayHost(null);
		doll.writeValuesTo(data);
		player.getInventory().placeItemBackInInventory(DollItem.makeItem(data));
		doll.discard();
		return true;
	}

	/** Glove recall-all: itemize every reachable summoned doll, parking to STORED when the inventory
	 * is full, then fold all TEMP into STORED. Strays within {@link #STRAY_RECALL_RANGE} ride the
	 * same sweep. Player-ledger only. Entries whose nature forbids recall are skipped whole — they
	 * are neither itemized nor parked, and their TEMP stays TEMP. Returns {itemized, parked}. */
	public int[] recallAll(ServerPlayer player) {
		int itemized = recallStrays(player, STRAY_RECALL_RANGE), parked = 0;
		for (var data : new ArrayList<>(dolls.values())) {
			if (!data.isSummoned() || data.uuid == null || !data.getNature().recallable()) continue;
			ServerLevel level = getLevel(player, data);
			if (!(level != null && level.getEntity(data.uuid) instanceof BaseDollEntity doll)) continue;
			if (itemize(player, doll)) itemized++;
			else if (park(player, doll, DollState.STORED)) parked++;
		}
		for (var data : dolls.values()) {
			if (data.state == DollState.TEMP && data.getNature().itemizable()) data.state = DollState.STORED;
		}
		return new int[]{itemized, parked};
	}

	// ---------- deferred transitions ----------

	public boolean tryItemize(ServerPlayer player, DollData data) {
		if (data.state != DollState.STORED || data.type == null) return false;
		// nothing to become: a timed summon parked STORED is dropped rather than retried forever,
		// so the invariant holds here and not only at the two producers that set STORED
		if (!data.getNature().itemizable()) return drop(data.uuid) != null;
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

	/** Sidebar roster push (glove.md §2c): rebuild in ledger order, send {@code DollRosterToClient}
	 * only on a change, and restamp each live entry's formation slot for the follow goal. */
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
			// entity not reachable: dolls are never chunk-serialized (entity.md §5), so an
			// unloaded doll is gone, not pending on disk — flip to TEMP safely.
			data.state = DollState.TEMP;
		}
	}

	/** SUMMONED watchdog: a dead entry is STORED and never re-summoned, or dropped outright when it
	 * has no item form to repair; an entry that no longer matches reality, or a doll past
	 * {@link #PULLBACK_DISTANCE} from its owner, is parked TEMP and resummoned near the owner on
	 * the next tick. */
	private void tickSummoned(ServerPlayer player, DollData data) {
		ServerLevel level = getLevel(player, data);
		if (level == null) {
			data.state = DollState.TEMP;
			return;
		}
		if (data.getHealth() <= 0) {
			// a timed summon that dies is simply gone: there is no broken item to hand back
			if (data.getNature().itemizable()) data.state = DollState.STORED;
			else dolls.remove(data.uuid);
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
			// trusted only inside the owner's circle; farther out it is stale or tampered
			if (player.serverLevel().isLoaded(pos) && data.position.distanceTo(player.position()) <= RESTORE_TRUST_RADIUS) {
				return data.position;
			}
		}
		return player.position().add(0, 1, 0);
	}

	// ---- spawn + register: the entry is inserted BEFORE addFreshEntity, so the entity's join-level
	// inverse check finds its SUMMONED entry; oldKey, when set, re-keys to the new entity's uuid.

	private boolean doSummon(ServerPlayer player, DollData data, @Nullable UUID oldKey) {
		BaseDollEntity doll = DollSpawn.materialize(player.serverLevel(), player, data);
		if (doll == null) return false;
		if (oldKey != null) dolls.remove(oldKey);
		dolls.put(data.uuid, data);
		player.serverLevel().addFreshEntity(doll);
		return true;
	}

}
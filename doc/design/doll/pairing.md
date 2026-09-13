# Doll Pairing — identity, ledger, state machine

The pairing layer keeps a doll as exactly one of {entity + `DollData`, item, parked `DollData`} at all times. It covers the identity model, the data model, the state machine that moves a doll between forms, the lifecycle events, and the reconciliation backup that keeps the whole thing honest.

## 1. Reference: ModularGolems

Borrowed ideas (from `../../mods_l2/ModularGolems-1.21`):

| Idea | Location in ModularGolems | How we use it |
|---|---|---|
| Item-driven summon | `GolemHolder.summon()` (`content/item/golem/GolemHolder.java:296`) | `DollItem.useOn()` restores a `DollEntity` from the item's significant values (not full NBT) |
| Bare-hand interact → convert back to item | `AbstractGolemEntity.mobInteractImpl()` (`content/entity/common/AbstractGolemEntity.java:236`) | `DollEntity.mobInteract()` with empty main hand |
| Position/facing free-space check on summon | `GolemHolder.setPos()` (`GolemHolder.java:283`) | Reuse for entity placement |
| Owner/doll uuid as synced data + tag fields | `AbstractGolemEntity` OWNER_ID / `setOwnerUUID` | Same pattern on `BaseDollEntity` |
| Track last dim/pos/hp per owner | `GolemTracker.TrackedData` (`content/capability/GolemTracker.java:60`) | Concept reused, but in the **player capability** (survives death/logout) and with significant-values only |
| Track position lazily via `setPosRaw` | `AbstractGolemEntity.trackPos()` (`AbstractGolemEntity.java:1160`) | Lightweight positional update into `DollData` (no NBT) |
| "Not seen as enemy" | `AbstractGolemEntity.canBeSeenAsEnemy()` (`AbstractGolemEntity.java:570`) | `BaseDollEntity.canBeSeenAsEnemy()` returns `false` |

**Key difference from ModularGolems:**

1. MG stores the whole golem in the holder item as full entity NBT (`GolemItems.ENTITY`). We store **only significant values** (see §2.2) to avoid per-operation serialization cost, on both the item and the capability.
2. MG's tracker is per-level `SavedData`. Ours is the **player capability**: copyOnDeath, serialized with the player file on logout, invisible to other players.

## 2. Data model

### 2.1 `DollState` (enum) — three states

```java
public enum DollState {
    SUMMONED, // an entity with this uuid is in a ServerLevel, actively tracked. No item.
    STORED,   // no entity, no item. DollData kept; it wants to BECOME AN ITEM — when the
              // player inventory has space, remove the data and convert to item form.
    TEMP,     // no entity, no item. DollData kept; the doll wants to BE AN ENTITY again —
              // it will be placed in the level as soon as possible (entity lost, logout,
              // death, dimension unloaded).
}
```

These are *intents*, not just *observations*:

- `SUMMONED` — the doll is an entity right now; the reconciliation keeps it honest (§5).
- `STORED` — parked and waiting for a free inventory slot; the moment space exists, `tick()` itemizes it (data removed, item produced). This is the "dead doll / tool-collected / no space" case.
- `TEMP` — parked and waiting for a place to exist; the moment it can (player online, dimension ready, free spot), `tick()` respawns the entity and it flips to `SUMMONED`. This is the "logout / death / entity accidentally gone" case.

There is **no ITEMIZED state**: once an item is produced the `DollData` entry is **deleted** — the item itself is the sole authoritative copy.

### 2.2 `DollData` (per doll) — significant values only

`content/attachment/doll/DollData.java`, `@SerialClass`. The entity is **not** saved into a `CompoundTag` snapshot; only the fields needed to reproduce it are stored, and they are updated lazily (every ~20 ticks, on damage, on teleport — never a full entity NBT save):

```java
@SerialClass
public class DollData {
    @SerialField public UUID uuid;               // == the CURRENT materialized entity's game uuid.
                                                 // Minted fresh per summon/re-summon, never on the item.
    @SerialField public ResourceLocation type;   // entity type id (future-proof: per-character dolls)
    @SerialField public DollState state;         // SUMMONED | STORED | TEMP

    // significant restore values (in-memory; persisted only with the player attachment)
    @SerialField public ResourceLocation dimension; // last known dim
    @SerialField public Vec3 position;              // last known block pos (fallback restore point)
    @SerialField public float yRot;                 // facing on restore
    @SerialField public CombatData combat = initialCombat(); // authoritative combat state (deferred pipeline),
                                                    // never null; fresh entries start at the doll's
                                                    // default max health. Max health is always the default attribute
    @SerialField(toClient = false) public Component customName; // null == none
    @SerialField(toClient = false) public long lastUpdate;   // game time of last value sync

    // loadout (planned, see loadout.md): four held item slots
    // @SerialField public DollInventory inventory = new DollInventory();
}
```

- `position` is kept fresh by the periodic lazy sync in `BaseDollEntity.tick()` (`DollHost.update`) — no NBT and no per-tick full-capture.
- `combat` is the **single authority** for the doll's health. The damage pipeline is deferred through it: `BaseDollEntity` intercepts `takeDamage` (damage) and the `onHeal` hook (healing — the parent `heal()` keeps its event/clamp logic and invokes `onHeal` right before applying) and routes every change into `DollData.combat` (via `CombatData.set`), then mirrors the result into the entity's own `CombatData` with `applyData` — instead of letting `setCombatProgress` mutate the entity's copy directly. Intercepting at these entry points means no re-entrancy guard is needed. Records are serialized by l2serial's `RecordCodec`. The doll **never writes its combat back** into `DollData` — every change already lands in `DollData.combat` at the moment it happens, so `writeValuesTo` only carries position/facing/type/name; the item is built from `DollData.combat` directly (§3.4).
- `customName` is a `net.minecraft.network.chat.Component`; l2serial has a built-in `CodecHandler<Component>` (via `ComponentSerialization`) so it round-trips through the capability storage directly.
- Values are written into `DollData` from the entity via `BaseDollEntity.writeValuesTo(DollData)` and applied back via `BaseDollEntity.readValuesFrom(DollData)` — a few scalar assignments, never `saveWithoutId`.
- Each summon materializes a fresh entity with a fresh game uuid (§3.1), so two entity instances never share a `DollData.uuid`. A stale entity's uuid quickly finds no `SUMMONED` entry (a new doll was summoned with a new uuid) → self-discard (§4.8, §5.2). Identity = **uuid + owner + type**. No separate "doll uuid" field exists: `level().getEntity(data.uuid)` must resolve to the real entity, and that only works when the ledger key *is* the entity's uuid.

### 2.3 `DollAttachment` (player capability)

`content/attachment/doll/DollAttachment.java`, `@SerialClass`, extends `PlayerCapabilityTemplate<DollAttachment>` (pattern: `CharacterAttachment`).

```java
@SerialClass
public class DollAttachment extends PlayerCapabilityTemplate<DollAttachment> {
    @SerialField
    private final LinkedHashMap<UUID, DollData> dolls = new LinkedHashMap<>();

    // ---- item → entity ----
    public boolean summon(ServerPlayer player, ItemStack stack, Vec3 pos);
    //   Spawns the entity first (fresh game uuid), keys a SUMMONED DollData to it from
    //   the item's values, and registers it BEFORE the entity joins the level (§3.1).
    //   No entry can pre-exist: item and DollData never coexist.

    // ---- entity → item (atomic) ----
    public boolean itemize(ServerPlayer player, BaseDollEntity doll);
    //   Removes this uuid's DollData from `dolls`, and only then builds+gives the item.
    //   No data removal → no item, doll stays SUMMONED. No inventory space:
    //   abort, stays SUMMONED with a "no space" message.

    // ---- entity → parked data (no item, keep a restore intent) ----
    public void park(ServerPlayer player, BaseDollEntity doll, DollState intent); // SUMMONED → STORED | TEMP

    // ---- per-intent deferred transitions, driven by tick() ----
    public boolean tryItemize(ServerPlayer player, DollData data); // STORED → item when a slot exists
    public boolean trySummon(ServerPlayer player, DollData data);  // TEMP → SUMMONED when level allows;
    //   mints a fresh uuid, re-keys the entry to it, spawns the entity with it (§3.5).

    // ---- restore / lifecycle ----
    public void restore(ServerPlayer player);                        // login/respawn: kick try* for parked dolls
    public void onPlayerDeath(ServerPlayer player);                  // park all SUMMONED → TEMP (data survives via copyOnDeath)
    public void onLogout(ServerPlayer player);                       // park all SUMMONED → TEMP

    // ---- queries / sync ----
    public DollData get(UUID uuid);
    public void trackPos(UUID uuid, double x, double y, double z);
    @Override public void tick(Player player);                       // server-only, §3.5 + §5
}
```

Registered in `GLMeta`:

```java
public static final AttVal.PlayerVal<DollAttachment> DOLL =
        ATT.player("doll_data", DollAttachment.class, DollAttachment::new, PlayerCapabilityNetworkHandler::new);
```

Access everywhere via `GLMeta.DOLL.type().getOrCreate(player)`. The capability is:
- **copyOnDeath** by default in `PlayerCapabilityHolder` (`GeneralCapabilityHolder` → `AttachmentType.copyOnDeath`), so parked dolls survive death.
- **persisted on logout** automatically (NeoForge player attachment saved with the player file).

## 3. Core interaction flows (state machine)

```
   ITEM ──summon (consume item, create DollData)──▶ SUMMONED
   SUMMONED ──itemize (empty hand / forced, space)──▶ ITEM (data removed)
   SUMMONED ──forced itemize, NO space──▶ STORED  (→ auto-itemize when a slot frees)
   SUMMONED ──logout / death / entity lost──▶ TEMP (→ auto-summon when possible)   [player-initiated intent]
   STORED ──slot free / restore──▶ ITEM (data removed)
   TEMP ──level ready / restore──▶ SUMMONED (entity spawned)
```

### 3.1 Summon (item → entity + DollData) — fresh uuid every time

1. `DollItem.useOn` reads `DOLL_DATA` from the stack.
2. Create the `DollEntity` (type is fixed, `DollItem.TYPE`) — the game mints a fresh uuid for it. Build a `DollData` (SUMMONED) keyed by **that entity uuid** from the item's restore values. No lookup or conflict handling is needed: an *item* never coexists with a `DollData` entry, so there is nothing to reuse or collide with — a cloned spare item simply summons an independent doll.
3. Free-space check like `GolemHolder.setPos`, set owner, apply the values (`readValuesFrom`: combat, position, facing, color, name).
4. **Register the entry BEFORE `addFreshEntity`**, so the entity's join-level inverse check finds its `SUMMONED` entry (§4.8, §5.2). Then spawn, mark `state = SUMMONED`, `lastUpdate`, clear the stack. The entry lands in the **summoner's** ledger: a traded doll has no owner/identity of its own, so the entry lives where the summon happens.

Order matters: **DollData is registered before the entity joins the level**, so an entity can never be in the world without a ledger entry (primary guard), and the item is consumed in the same call (one item in ⇔ one entry out).

### 3.2 Itemize (entity → item) — the atomic rule

The operation the whole anti-dupe design hinges on:

```java
public boolean itemize(ServerPlayer player, BaseDollEntity doll) {
    UUID uuid = doll.getUUID();
    DollData data = dolls.get(uuid);
    if (data == null || data.state != SUMMONED) return false;      // nothing to itemize
    if (!hasSlotFor(player, doll)) {                                 // interactive only
        // abort, doll stays SUMMONED, message "no space" (never silently parks)
        return false;
    }
    // 1) REMOVE DATA FIRST — the item must not be produced if this can't happen
    dolls.remove(uuid);
    // 2) only now build + give the item — refresh the significant values into the ledger entry,
    // build the item straight from it (combat comes from the authoritative DollData.combat)
    doll.writeValuesTo(data);
    ItemStack stack = DollItem.makeItem(data);                      // combat/color/name ->
    giveOrDrop(player, stack);                                      // slot pre-checked above
    // 3) discard the entity last, after the item is secured
    doll.discard();
    return true;
}
```

- **Data is removed before the item exists.** If removal were skipped/failed, itemization aborts and the doll stays SUMMONED — item and data never coexist.
- **The entity is discarded last**, after the item is secured, so item and entity never coexist either.
- `hasSlotFor` pre-checks so the give step cannot fail after removal; ground-drop is the fallback if it still races.
- **Sequence-safe**: doll death and owner death (or two `onRemove`s) happen in a defined tick order, not simultaneously. Whichever fires first wins; both `itemize` and `park` only act on a `SUMMONED` entry (`data == null` or `state != SUMMONED` → `false`), so the second call is a no-op and the same uuid can never be itemized twice. The only divergence is negligible: if the owner-death park is processed first, the doll is discarded while still full-health and skips the fatal hit.

### 3.3 Park (SUMMONED → STORED / TEMP, no item)

Used where the entity must go but the normal transition isn't possible right now:

- **→ STORED**: a doll that cannot come back as an entity (e.g. `trySummon` while health ≤ 0) or was otherwise tool-collected. Wants to become an item; `tick()` converts it the moment a slot frees (§3.5).
- **→ TEMP**: logout / death / dimension-left-behind / entity accidentally gone. Wants to become an entity again; `tick()` re-summons it as soon as the level allows (§3.5).

`park` keeps `DollData` (updates `state`, `dimension`, `position`), discards the entity, and marks `lastUpdate`. There is no removal hook or "parked" flag on the entity — removal is lazy (no-op if the entry is already parked, `state != SUMMONED`).

### 3.4 Restore (login / respawn)

On `PlayerLoggedInEvent` and `PlayerRespawnEvent`: call `restore()`, which kicks `try*` for every parked entry:

- `STORED` → `tryItemize` (needs a free slot now; otherwise it waits for `tick`).
- `TEMP` → `trySummon` (needs the player's current dimension loaded and a free spot; otherwise it waits for `tick`).

No forced choice: the *intent* stored in `DollState` decides the outcome. A config toggle (default: TEMP) can flip how logout/death parks dolls for players who prefer item form.

### 3.5 Deferred transitions (in `tick`, server only)

Until now these ran "sometimes"; here they run every tick cheaply:

- **STORED**: if `player.addItem(item)` can succeed → **capture the significant values from `data` first** → remove the entry → build the item from the captured values → insert. (Same atomic rule as §3.2; the snapshot is taken *before* the removal so the item is always built from a valid `DollData`, not a nulled one.)
- **TEMP**: if the player is online in a loaded dimension and a free spot exists (near player, or at `DollData.position` when same dim) → **create a fresh entity (new game uuid), re-key the entry to it, register the entry before `addFreshEntity`**, and spawn from the significant values → `SUMMONED`. Retry next tick otherwise. If a stale entity for the *old* uuid still lingers (park/discard not yet flushed, chunk leak), it has no `SUMMONED` entry anymore and self-discards via §5.2 — same end state in either order. A TEMP is retried **every tick**, never throttled. The recorded `position` is only trusted while it lies within the owner's 10-block circle; farther than that it is stale, tampered, or records a long run, and the doll restores near the player instead (anti-tamper).

Both are O(#parked) small operations per tick; state only *moves forward* once a precondition holds.

## 4. Lifecycle events

`event/DollEventHandlers.java` (`@EventBusSubscriber`), or `MiscEventHandlers`:

| Event | Action |
|---|---|
| `PlayerLoggedOutEvent` | `onLogout`: park all SUMMONED → **TEMP**. DollData persists in the player savefile. |
| `LivingDeathEvent` (ServerPlayer) | `onPlayerDeath`: park all SUMMONED → **TEMP**. Data survives via copyOnDeath. Respawn restores via `PlayerRespawnEvent` (→ `SUMMONED`). |
| `PlayerEvent.Clone` | No-op (capability auto-copied, entries already TEMP). Optional `onClone` sanitize hook. |
| `PlayerRespawnEvent` | `restore(player)` (§3.4). |
| `EntityJoinLevelEvent` (ServerLevel) | Belt-and-braces: freshly-spawned / loaded-from-disk doll → immediate inverse check against `findSummoned(uuid)` (§4.8, §5.2). |

Dimension changes need no handler: when the owner leaves the dimension the doll's `getHost()` resolves to null (owner not in that level), so the doll self-discards (§5.2) and its entry reconciles to TEMP, then `trySummon` respawns a fresh doll near the player in the new dimension — uniformly, regardless of chunk load state.

## 5. Integrity: reconciliation backup + anti-dupe

The **primary** pairing is uuid identity enforced at every state transition (§3): item ⇔ DollData ⇔ entity never coexist. Reconciliation below is the backup safety net (log a warning, never silently drop).

### 5.1 Forward check (capability → world), in `DollAttachment.tick`

Every tick, server only. For each entry, by state:

- **STORED** → nothing to validate (no entity expected); `tryItemize` runs every tick (§3.5).
- **TEMP** → nothing to validate; `trySummon` runs every tick, immediately when possible (§3.5).
- **SUMMONED**:
  0. **No health** (`combat.amount() <= 0`): dead — discard any lingering entity and flip to **STORED** (never re-summoned; `tryItemize` delivers the item). No warning: death is routine.
  1. Resolve the `ServerLevel` from `DollData.dimension`. If that dim is gone, the entity is effectively unreachable (paired dolls are never chunk-serialized, entity.md §5): flip to **TEMP** so `trySummon` re-materializes it near the player.
  2. Else **entity missing** (`level.getEntity(data.uuid)` → null): removed, `/kill @e`, or a mod bug. Flip to **TEMP** (it wants to come back) → **warn log** (`doll.resync.missing`). Safe: `trySummon` spawns only a fresh-entity with a fresh-uuid, so it can never mint a duplicate of anything (§3.5).
  3. **Entity present but does not match** (wrong type/owner for the uuid — tampered/foreign copy): `discard()` the entity and flip to **TEMP** → **warn log** (`doll.resync.tampered`). (A same-uuid dupe can't otherwise exist: every summon materializes a fresh entity/uuid and the doll is never chunk-serialized, entity.md §5.)
  4. **Entity too far** (same dimension, ≥ `PULLBACK_DISTANCE` = 48 blocks from the owner): write the values back, `discard()`, flip to **TEMP** — the next tick's `trySummon` restores it near the owner (the recorded spot is >10 blocks away, so the trust circle is bypassed).

`matches(entity)`: `entity instanceof DollEntity`, the ledger key **is** the entity's real uuid (looked up by it, trivially equal), `getType()` matches `DollData.type`, and `doll.isOwner(player)`. Health/position are volatile and deliberately **not** part of identity.

### 5.2 Inverse check (world → capability), in `BaseDollEntity.tick`

Every 20 ticks the doll asks its (owner's) host: *"is my uuid `SUMMONED`, and do I match?"*

- **No `SUMMONED` entry for my uuid** → self-discard → **warn log** (`doll.resync.orphan`). This is the regular fate of an outdated doll: a new doll was summoned with a **fresh uuid**, so the ledger's `DollData` now names a different uuid and this entity's uuid has no entry — it discards. The same rule covers parked entries (STORED/TEMP) and mid-transition strays, and this one-shot check runs on spawn/load too (the on-disk leak self-check, entity.md §5).
- **Order vs `trySummon` doesn't matter** (they run on different ticks): if the inverse check fires first, the stale stray discards and `trySummon` re-summons a fresh entity later; if `trySummon` fires first, the new entity carries the new uuid and passes the check. Either order converges to SUMMONED with exactly one entity.

No exemption is needed: itemize/park remove the entry and then `discard()` the entity synchronously, so the entity never ticks again — there is no orphan window to suppress.

### 5.3 Summon/itemize guards (first line)

- `summon` registers the minted-uuid `DollData` before the entity exists, so an entity never appears without a ledger entry (nothing to reject — every summon is a fresh identity).
- `itemize` refuses when removal of the `DollData` cannot complete before producing the item.
- `mobInteract` gates on the owner (creative bypass allowed — ledger updates regardless).

Result at all times: a doll exists as **exactly one** of {entity + DollData, item, parked DollData (STORED/TEMP)} — never two forms sharing an identity. A uuid names at most one live entity, and no item carries one.

## 6. Mob targeting: "not seen as enemy, but still takes damage"

```java
@Override
public boolean canBeSeenAsEnemy() {
    return false;
}
```

- Vanilla hostile targeting (`NearestAttackableTargetGoal`), `Mob.setTarget`/`canAttack` guards, and `BeingHitTargetGoal` gate on `canBeSeenAsEnemy` — with `false`, hostile mobs never *choose* the doll.
- Damage is unaffected: AoE, projectiles, already-engaged mobs, and direct player attacks still hurt via the untouched `DamageRefactorEntity` `hurt`/`actuallyHurt` path.
- Reference: `AbstractGolemEntity.canBeSeenAsEnemy()` (`AbstractGolemEntity.java:570`). Since dolls never attack, mobs need no `HurtByTargetGoal` retaliation.

## 7. Edge cases

- **STORED vs TEMP are intents, not outcomes**: STORED is *"wants to be an item"* (auto-itemizes on free slot), TEMP is *"wants to be an entity"* (auto-summons when the level is ready). Invariants (§5) only ever need to check SUMMONED.
- **Config for park intent**: logout/death park as TEMP by default (follow theme). A toggle can make them park as STORED (item form preferred). STORED can also be reached directly from TEMP by a player-facing "turn my dolls into items" action — all transitions are intent-driven, so the UI/config is just choosing the target state.
- **Item destroyed while itemized** (player throws it in lava): the doll data is already gone by design — a player-caused loss of a normal item, and the tradeoff the "no data after itemize" rule accepts. System-driven loss (bugs, logout races, full inventory, death) is still prevented by the ledger.
- **Tradeable dolls**: item form carries no owner-id and no uuid, so dolls can be dropped/given/traded freely — it's just an item. The item is the sole authoritative copy while in item form; whoever summons it mints a fresh entry in *their* ledger. The owner-gate (creative bypass) still exists at `mobInteract` — a traded doll is recalled to item form by its *current* summoner.
- **Player offline / other dim when a SUMMONED doll mutates**: reconciliation resolves through `DollData.dimension`; if that dim is unloaded it **does nothing** (can't inspect, could be alive). Once loaded: entity gone → TEMP (re-summon later); entity present → `matches()` check. Never force-spawns while the truth is unknowable.
- **No double-spawn / no on-disk leftover**: every summon mints a fresh uuid and the doll is never chunk-serialized (entity.md §5) — "two entities for one uuid" cannot occur. If a stale entity ever lingers (chunk leak, mid-park), its uuid has no `SUMMONED` entry anymore (new doll, new uuid) and §5.2 discards it. And §5.1 never flips an entry to TEMP for a chunk it can't inspect, so an unloaded-but-alive moment is never misread as a death.
- **Event ordering**: lifecycle events (death, logout, dimension change) run in a sequence, not simultaneously. Whichever transition fires first wins; later ones short-circuit on `state` and no-op. The end state is the same regardless of order, with one negligible exception: if owner-death is processed before the doll's own death, the doll is parked to TEMP first and skips taking the lethal hit (§3.2).
- **Cloning / two players (creative-clone exploit)**: a cloned spare *item* is just another doll — it summons an independent doll with its **own fresh uuid** (§3.1). No two summons can collide, the capability is per-player, and no chunk serialization exists (entity.md §5). Cloning only ever duplicates *items*, never entities.
- **Client sync is now cheap**: `DollData` is a small scalar set (no NBT snapshot), so full `PlayerCapToClient` sync is fine; only `customName`/`lastUpdate` are marked `toClient = false`.
- **Double `onRemove` / item-only-discard races**: guarded by the `itemized`/`parked` flags set in §3.2/§3.3.
- **Health restore without NBT**: `readValuesFrom` seeds the entity's `CombatData` from `DollData.combat` via `applyData`; the internal `CombatData` is reconciled from the vanilla Health setter by `validateCombatData()` in `tick`.
- **Future doll kinds**: `DollData.type` (set to `DollItem.TYPE` at summon) already supports per-character dolls under the same ledger; extra per-kind significant values are added as a few more scalar fields (or new data components on the item for item-carried values), never a full NBT snapshot.
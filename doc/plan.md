# Plan: Level + Chunk Capability System for Chunk-Granular Area Effects

## 1. Overview & Goal

Implement a two-layer capability system using the existing `l2core` attachment framework (`AttReg` / `AttVal` / `AttachmentDef`):

* **Level attachment** — authoritative store of all active area-effect data. Each entry owns a `UUID`, a chunk-granularity range, and an **owner block** (`BlockPos`).
* **Chunk attachment** — lightweight index attached to every `LevelChunk`. Stores only `Set<UUID>` that claim to affect that chunk. Can resolve to full data on demand via the level attachment, lazily pruning stale UUIDs and caching per-tick.

Desired properties:
* Add/remove at level scope, effect at chunk scope.
* `add` fans out UUID to affected loaded chunks eagerly; unloaded chunks are tracked in a serializable **pending `Map<String, List<UUID>>`** (hex `ChunkPos` key) on the level attachment.
* Pending entries are delivered synchronously when the chunk loads (via `LevelChunk` load / `ChunkEvent.Load`), then removed from the pending map. No `CompletableFuture` needed.
* `remove` touches only the level store — lazy deletion for both pending (skipped at flush) and already-delivered chunk entries.
* `chunk.getAffecting()` resolves UUIDs → objects, removes dead entries, and is cached transiently for the current tick (fields have no `@SerialField`, no `transient` keyword needed).
* Each entry is anchored to an owner block; the level attachment validates owner liveness spread over 5 s (100 ticks, `O(M/100)` per tick) **only when the owner chunk is loaded** and auto-removes invalid entries.

This is the generic infrastructure for future systems that need "large range of effect" (auras, corruption, difficulty modifiers, youkai territory, gap destabilization, etc.) without per-block storage or per-tick global scans. Typical `R ≤ 8` (`17×17 = 289` chunks max per add); larger ranges use a different system.

---

## 2. Non-Goals

* No per-block or per-entity storage directly — chunk is the smallest unit.
* No client/server sync in v1 (server authoritative). Client sync can be added later via `PacketHandler` if needed.
* No datapack registry integration in v1 (payload codec can be added later if data-driven effects are needed).

---

## 3. Context: Current Capability System in This Repo

| Pattern | Example | Location |
|---|---|---|
| Player capability | `CharacterAttachment extends PlayerCapabilityTemplate` | `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/character/CharacterAttachment.java:15` |
| Chunk capability | `StructureAttachment extends GeneralCapabilityTemplate<LevelChunk, …>` | `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/home/core/StructureAttachment.java:16` |
| Entity capability | `FrogGodCapability extends GeneralCapabilityTemplate<Frog, …>` | `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/misc/FrogGodCapability.java:24` |
| Level SavedData | `GapMappingData extends BaseSavedData` (overworld `DataStorage`) | `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/gap/GapMappingData.java:19` |
| Registration | `GLMeta.ATT = AttReg.of(GensokyoLegacy.REG)` + `ATT.entity(...)` / `ATT.player(...)` | `src/main/java/dev/xkmc/gensokyolegacy/init/registrate/GLMeta.java:35` |
| Holder impl | `GeneralCapabilityHolder<E,T>` — predicate-guarded, `getOrCreate(e)` / `getExisting(e)` | `libs/l2core-3.0.8+16-sources.jar: dev/xkmc/l2core/capability/attachment/GeneralCapabilityHolder.java` |
| Serialization | `AttachmentDef` serializes via `TagCodec(provider).toTag/fromTag` | `libs/l2core-3.0.8+16-sources.jar: dev/xkmc/l2core/capability/attachment/AttachmentDef.java` |
| Player tick | `BaseCapabilityEvents.onPlayerTick` iterates `PlayerCapabilityHolder.INTERNAL_MAP` | `libs/l2core-3.0.8+16-sources.jar: dev/xkmc/l2core/events/BaseCapabilityEvents.java` |

Key takeaway: `AttReg.entity(id, DataClass.class, DataClass::new, HolderClass.class, pred)` works for **any** `IAttachmentHolder` — `LevelChunk`, `ServerLevel`, `Level`, `Frog`, etc. Currently there is **no** `Level` attachment in `GLMeta.java:43`; adding one follows the same pattern. `TagCodec` only serializes `@SerialField`; absence of annotation means not persisted (Java `transient` keyword unnecessary).

Existing `BaseSavedData` level stores (`IndexStorage`, `GapMappingData`) are singleton per `ServerLevel` via `getDataStorage().computeIfAbsent(FACTORY, ID)`. For the new system we intentionally use an **attachment** instead to stay consistent with the capability system, get automatic per-dimension storage, and avoid global overworld indirection.

---

## 4. Design

### 4.1 Data Model

```java
// src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/AreaEffectEntry.java
@SerialClass
public class AreaEffectEntry {
    @SerialField UUID id;                          // generated on add
    @SerialField BlockPos ownerPos;                 // anchor block (immutable after creation)
    @SerialField ChunkPosRange range;               // chunk-granularity extent
    @SerialField EffectData data;                  // base class @SerialClass, subclasses via TagCodec inheritance
    @SerialField long createdGameTime;
    @SerialField @Nullable Long expiresAt;

    public boolean isOwnerValid(ServerLevel level) {
        return data.isOwnerStillValid(level, ownerPos, level.getBlockState(ownerPos));
    }
}

// Minimal range representation — cheaper than Set<ChunkPos> for large areas
@SerialClass
public class ChunkPosRange {
    @SerialField int minCX, minCZ, maxCX, maxCZ;
    public boolean contains(ChunkPos pos) { … }
    public Stream<ChunkPos> stream() { … }
    public static ChunkPosRange ofBlocks(BlockPos ownerPos, int radiusChunks) { … }
}
```

Base payload with inheritance (`TagCodec` supports it when the field handle class is `@SerialClass`):

```java
@SerialClass
public abstract class EffectData {
    public boolean isOwnerStillValid(ServerLevel level, BlockPos ownerPos, BlockState state) {
        return !state.isAir(); // default; subclasses override (e.g., check BlockEntity)
    }
}
```

Subclasses are `@SerialClass` (e.g., `BeaconEffectData` checks `level.getBlockEntity(ownerPos) instanceof FooBlockEntity`). No `Handlers`/`CodecHandler` registry needed for v1.

*Owner block*: immutable anchor. Every entry **must** have an owner. Range is typically `ChunkPosRange.ofOwner(ownerPos, R)` but can be explicit. When owner block is broken / replaced, the level tick cleans the entry.

*Granularity*: range is always expanded to chunk boundaries on add: `blockBox → chunkBox via (x>>4)`. Owner `BlockPos` validation gates on `ChunkPos(ownerPos)` loaded state.

### 4.2 Level Attachment

```java
// src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/LevelEffectAttachment.java
@SerialClass
public class LevelEffectAttachment
        extends GeneralCapabilityTemplate<Level, LevelEffectAttachment> {

    @SerialField
    private final Map<UUID, AreaEffectEntry> byId = new LinkedHashMap<>();

    // Pending fan-out for chunks not loaded at add time.
    // Key = hex string of chunk pos long (Long.toHexString(ChunkPos.toLong(x,z))), value = list of UUIDs.
    // String key matches NBT CompoundTag key type; hex is compact. List discarded as unit on load.
    @SerialField
    private final Map<String, List<UUID>> pending = new LinkedHashMap<>();

    // not @SerialField → not serialized (no `transient` needed)
    private long lastPendingFlushTick = Long.MIN_VALUE;

    public UUID add(AreaEffectEntry entry);
    public boolean remove(UUID id); // O(1), stale pending skipped at flush
    @Nullable public AreaEffectEntry get(UUID id);
    public Collection<AreaEffectEntry> getAll();
    public Map<UUID, AreaEffectEntry> view();

    public void tickValidation(ServerLevel level);   // O(M/100) per tick, see 5.5
    public void tickPendingFlush(ServerLevel level); // gated P>10 every 5s, see 5.4
}
```

*Attached to*: `Level` (or `ServerLevel`), predicate `!isClientSide` for server-only. `ServerLevel` preferred for `getChunkSource` / `isLoaded`.

*Pending semantics*: source of truth for unloaded fan-out. Populated for every unloaded `ChunkPos` in range, persisted to NBT (survives restarts), drained synchronously on chunk load. On `remove` pending is **not** scanned; stale `UUID`s are skipped at flush via `byId.containsKey(id)` then whole list discarded. Short-lived: drains within 1 tick of natural load, or within 5 s via bulge flush when `P>10`.

*Validation*: spread `O(M/100)` per tick via `floorMod(id.hashCode(),100)==tick%100` (see 5.5), owner chunk must be loaded to validate.

### 4.3 Chunk Attachment

```java
// src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/ChunkEffectAttachment.java
@SerialClass
public class ChunkEffectAttachment
        extends GeneralCapabilityTemplate<LevelChunk, ChunkEffectAttachment> {

    @SerialField
    private final Set<UUID> effectIds = new LinkedHashSet<>();

    // not @SerialField → not serialized (no `transient` needed)
    private long cachedTick = Long.MIN_VALUE;
    private List<AreaEffectEntry> cachedResolved = List.of();
    private boolean cacheValid = false;

    void addId(UUID id);
    public List<AreaEffectEntry> getAffecting(Level level);
    public List<AreaEffectEntry> getAffecting(ServerLevel level, ChunkPos selfPos);
}
```

*Attached to*: `LevelChunk` (same as `StructureAttachment` `GLMeta.java:43`). Only `effectIds` persisted; cache is per-tick transient.

### 4.4 Registration (`GLMeta`)

```java
// src/main/java/dev/xkmc/gensokyolegacy/init/registrate/GLMeta.java:35
public static final AttVal.CapVal<Level, LevelEffectAttachment> LEVEL_EFFECT =
        ATT.entity("level_effect", LevelEffectAttachment.class,
                   LevelEffectAttachment::new, Level.class, e -> true);

public static final AttVal.CapVal<LevelChunk, ChunkEffectAttachment> CHUNK_EFFECT =
        ATT.entity("chunk_effect", ChunkEffectAttachment.class,
                   ChunkEffectAttachment::new, LevelChunk.class, e -> true);
```

`GLMeta.register()` is no-op; registration at static init before registry freeze (`GensokyoLegacy.java:116`).

### 4.5 Manager / Facade

```java
public final class AreaEffectManager {
    public static UUID add(ServerLevel level, BlockPos ownerPos, ChunkPosRange range, EffectData data);
    public static UUID add(ServerLevel level, AreaEffectEntry entry);
    public static boolean remove(ServerLevel level, UUID id);
    @Nullable public static AreaEffectEntry get(ServerLevel level, UUID id);

    public static List<AreaEffectEntry> getAffecting(LevelChunk chunk);
    public static List<AreaEffectEntry> getAffecting(Level level, ChunkPos pos);
    public static List<AreaEffectEntry> getAffecting(Level level, BlockPos pos);

    public static void tickValidation(ServerLevel level);
    public static void tickPendingFlush(ServerLevel level);
}
```

`add` enforces `ownerPos`; `ChunkPosRange.ofOwner(ownerPos, R)` for convenience.

---

## 5. Workflows

### 5.1 Add (with owner + pending)

```
caller -> AreaEffectManager.add(level, ownerPos, range, data)
  0. require level.isLoaded(ownerPos) || warn; require data != null && ownerPos != null
  1. uuid = UUID.randomUUID()
  2. entry = new AreaEffectEntry(uuid, ownerPos, range, data, level.getGameTime())
  3. levelAtt = level.getData(GLMeta.LEVEL_EFFECT.get())
  4. levelAtt.byId.put(uuid, entry)
  5. fanOut + pending:
       for (ChunkPos cpos : range.stream()) {
           LevelChunk chunk = level.getChunkSource().getChunk(cpos.x, cpos.z, false) // only if loaded
           if (chunk != null) {
               chunk.getData(GLMeta.CHUNK_EFFECT.get()).addId(uuid);
               chunk.setUnsaved(true);
           } else {
               String key = Long.toHexString(cpos.toLong()); // hex matches NBT string key
               levelAtt.pending.computeIfAbsent(key, k -> new ArrayList<>()).add(uuid);
           }
       }
  6. return uuid
```

*Complexity*: `O(N)` where `N=chunks_in_range`. Bound `R≤8` → `N≤289` → <1 ms server thread. Enforce `MAX_CHUNKS_PER_ADD=1024` warning. Larger ranges use a different system. No force-load; pending handles unloaded.

### 5.2 Remove (O(1), pending lazily cleaned)

```
caller -> AreaEffectManager.remove(level, uuid)
  1. levelAtt = level.getData(GLMeta.LEVEL_EFFECT.get())
  2. removed = levelAtt.byId.remove(uuid) != null
  3. // no pending scan; stale UUIDs in pending skipped on flush (byId.containsKey), whole list discarded
     // no loaded-chunk iteration; stale effectIds pruned lazily on next getAffecting()
  4. return removed
```

`O(1)`. Trade-off: `pending` may hold dead `UUID`s until next flush (≤5 s when `P>10`, or next natural `ChunkEvent.Load`), negligible vs `O(P)` scan per remove.

### 5.3 Fetch (Chunk Query)

```
chunk.getData(GLMeta.CHUNK_EFFECT.get()).getAffecting(level)
  1. if (level.getGameTime() == cachedTick && cacheValid) return cachedResolved  // O(1) hit
  2. levelAtt = level.getData(GLMeta.LEVEL_EFFECT.get())
  3. resolved = []; toRemove = []
     for (UUID id : effectIds) {
         entry = levelAtt.get(id)
         if (entry != null) resolved.add(entry)
         else toRemove.add(id)
     }
  4. if (!toRemove.isEmpty()) { effectIds.removeAll(toRemove); chunk.setUnsaved(true); }
  5. cachedResolved = List.copyOf(resolved); cachedTick = level.getGameTime(); cacheValid = true
  6. return cachedResolved
```

`O(k)` miss (`k=effectIds.size()` typically 0-3), `O(1)` hit via `gameTime` equality. `GeneralCapabilityTemplate.tick` for `LevelChunk` not auto-dispatched; `gameTime` check is robust.

### 5.4 Pending Flush for Unloaded Chunks (sync on chunk load)

Unloaded chunks from 5.1 are tracked in `LevelEffectAttachment.pending: Map<String, List<UUID>>` (hex key, survives restarts).

#### 5.4.1 Data Structure

* Key: hex `String` of `ChunkPos.toLong(x,z)` — matches NBT `CompoundTag` string keys directly, compact (≤16 chars).
* Value: `List<UUID>` — all UUIDs for that chunk, coalesced. Discarded as unit on load.

#### 5.4.2 Flush Trigger

Two complementary triggers, both server thread, no `CompletableFuture`:

1. **Chunk load** (primary) — `ChunkEvent.Load` or `LevelChunk` mixin:

   ```java
   @SubscribeEvent
   public static void onChunkLoad(ChunkEvent.Load event) {
       if (!(event.getChunk() instanceof LevelChunk chunk)) return;
       if (!(chunk.getLevel() instanceof ServerLevel sl)) return;
       String key = Long.toHexString(chunk.getPos().toLong());
       var att = sl.getData(GLMeta.LEVEL_EFFECT.get());
       var pendingIds = att.pending.remove(key);
       if (pendingIds == null || pendingIds.isEmpty()) return;
       var cap = chunk.getData(GLMeta.CHUNK_EFFECT.get());
       boolean changed = false;
       for (UUID id : pendingIds) {
           if (!att.byId.containsKey(id)) continue; // removed before flush
           if (cap.effectIds.add(id)) changed = true;
       }
       if (changed) chunk.setUnsaved(true);
   }
   ```

2. **Level tick flush** (fallback for bulge) — `EffectLevelTickHandler` → `tickPendingFlush`:

   ```
   void tickPendingFlush(ServerLevel level)
     1. if (level.getGameTime() - lastPendingFlushTick < 100) return; // every 5 s
        lastPendingFlushTick = level.getGameTime();
     2. if (pending.size() <= 10) return;
     3. var snapshot = new ArrayList<>(pending.keySet());
        for (String key : snapshot) {
            long posLong = Long.parseUnsignedLong(key, 16);
            var cpos = new ChunkPos(posLong);
            if (level.getChunkSource().isChunkLoaded(cpos.x, cpos.z)) continue;
            // on main thread, last arg `false` schedules offthread load per ServerChunkCache; forcing generation is expected
            level.getChunkSource().getChunk(cpos.x, cpos.z, ChunkStatus.FULL, false);
            // delivery happens in ChunkEvent.Load above; no manual future
        }
   ```

   For `P≤10` (expected `R≤8`) tick does nothing; `ChunkEvent.Load` already drains within 1 tick. Bulge `P>10` offthread-loads missing chunks without stalling tick. `ServerChunkCache` dedupes repeated `getChunk` for same key — only one load event fires.

### 5.5 Owner Validation Tick (spread O(M/100) per tick)

Wiring — `GeneralCapabilityTemplate.tick` not auto-dispatched for `Level`; subscribe explicitly:

```java
@EventBusSubscriber(modid = GensokyoLegacy.MODID, bus = Bus.GAME)
public class EffectLevelTickHandler {
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        AreaEffectManager.tickValidation(sl);
        AreaEffectManager.tickPendingFlush(sl); // gated P>10 inside
    }
}
```

Work — `O(M/100)` per tick via bucket:

```
LevelEffectAttachment.tickValidation(ServerLevel level)
  1. long tick = level.getGameTime(); int tickBucket = (int)(tick % 100);
     toRemove = []
     for (entry : byId.values()) {
         int bucket = Math.floorMod(entry.id.hashCode(), 100); // UUID hash uniform vs spatial clustering
         if (bucket != tickBucket) continue;
         ChunkPos ownerCP = new ChunkPos(entry.ownerPos)
         if (!level.getChunkSource().isChunkLoaded(ownerCP.x, ownerCP.z)) continue;
         if (!entry.isOwnerValid(level)) toRemove.add(entry.id)
     }
  2. for (uuid : toRemove) byId.remove(uuid)
```

Each entry checked once per 100 ticks (≤5 s latency), per-tick cost `~M/100` (e.g., `M=50` → 0.5 checks/tick). `floorMod` handles negative hashes. Unloaded owners skipped until loaded; `onRemove`/`setRemoved` can eagerly call `remove` for zero-latency path.

---

## 6. Edge Cases & Considerations

### 6.1 Unloaded Chunks at Add Time

Handled by eager + pending + sync flush on `ChunkEvent.Load` (primary) and `tickPendingFlush` bulge every 5 s (`P>10`) via offthread `getChunk(...,false)`. No per-query scan.

### 6.2 Dimensionality

`LevelEffectAttachment` per-`Level`; separate entries per dimension.

### 6.3 Client/Server

Mutations on `ServerLevel` only. Client attachments empty unless synced via `LevelEffectSyncToClient` if needed.

### 6.4 Persistence & Versioning

Both attachments + `pending` use `TagCodec` + `@SerialClass`. `pending` `Map<String,List<UUID>>` hex key matches NBT `CompoundTag` strings directly. `LinkedHashMap` preserves order. Crash between add and flush skips dead `UUID`s via `byId.containsKey` on next flush.

### 6.5 Performance (with design constraints)

* **Add:** `O(N)` `R≤8` → `N≤289` <1 ms. `pending` coalesced list append.
* **Remove:** `O(1)` (`byId.remove`); `pending` stale skipped at flush.
* **Fetch:** `O(k)` miss (`k` 0-3), `O(1)` hit via `cachedTick`; hot path.
* **Pending flush:** `ChunkEvent.Load` `O(L)` per loaded chunk; tick bulge `O(P)` every 5 s only when `P>10` → offthread `getChunk(...,false)` then sync drain. No periodic NBT bloat (short-lived).
* **Owner validation:** `O(M/100)` per tick via bucket (`M=50` → 0.5/tick).
* **Memory:** `effectIds` small; `pending` short-lived, hex `String` ≤16 chars, negligible.

### 6.6 Concurrency / Thread Safety

Hot path (`ChunkEvent.Load`, `add`/`remove`, validation) server thread only. Bulge flush `getChunk(...,false)` on main thread schedules offthread load per `ServerChunkCache`; mutation still sync in `ChunkEvent.Load`, no manual threading.

### 6.7 Granularity Alignment

Helpers: `ChunkPosRange.ofBlocks`, `ofChunks`, `ofRadius(center, radiusChunks)`, `ofBoundingBox`.

### 6.8 Chunk Invalidation

After `effectIds` mutation, always `chunk.setUnsaved(true)`.

---

## 7. Implementation Steps

### 7.1 New Files

* `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/ChunkPosRange.java`
* `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/EffectData.java` — base `@SerialClass`
* `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/AreaEffectEntry.java`
* `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/LevelEffectAttachment.java`
* `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/ChunkEffectAttachment.java`
* `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/AreaEffectManager.java`
* `src/main/java/dev/xkmc/gensokyolegacy/content/attachment/effect/EffectLevelTickHandler.java`

### 7.2 Modified Files

* `src/main/java/dev/xkmc/gensokyolegacy/init/registrate/GLMeta.java:35` — add `LEVEL_EFFECT` and `CHUNK_EFFECT`.
* `src/main/java/dev/xkmc/gensokyolegacy/init/GensokyoLegacy.java:116` — already calls `GLMeta.register()`.

### 7.3 Event Wiring

* `EffectLevelTickHandler` — 5 s owner validation (spread) + 5 s bulge flush (`P>10`).
* `EffectChunkLoadHandler` — `ChunkEvent.Load` sync drain (primary).
* Owner-block `onRemove`/`setRemoved` → `AreaEffectManager.remove` for instant path.

### 7.4 Tests / Verification

1. Add 3×3 → loaded chunks contain `UUID`, `ownerPos` persisted.
2. `getAffecting()` twice same tick → cache hit `O(1)`.
3. Next tick → cache miss, recompute.
4. Remove → `byId` null, `pending` stale until flush (skipped), `effectIds` pruned on next fetch.
5. Unloaded pending: add 1 loaded +1 unloaded → `pending` 1 entry; natural load → sync drain; bulge `P>10` → 5 s offthread drain.
6. Remove before flush → pending stale skipped, chunk never receives dead `UUID`.
7. Restart with pending → NBT round-trip, load still delivers.
8. Restart → `byId`/`effectIds`/`pending` round-trip.
9. Owner break → ≤100 ticks via bucket, chunk pruned on next fetch.
10. Owner chunk unloaded → skipped until reload, then validated.

---

## 8. Alternatives Considered

| Alternative | Pros | Cons | Why not chosen |
|---|---|---|---|
| `BaseSavedData` level store | Proven | Global not per-dimension | Capability system required |
| Level-only scan | No fan-out | `O(M)` per fetch | Chunk index `O(k)` faster |
| Eager remove sync | Strong consistency | `O(N)` per remove | Lazy prune cheaper |
| `SavedData` per chunk | Isolated | Harder lifecycle | Attachment serialization |

---

## 9. Open Questions

* Client sync for rendering? Add `AreaEffectSyncPacket` via `GensokyoLegacy.HANDLER:75` if needed.

`EffectData` typing, `R≤8` bound, and pending key encoding are closed (see 4.1, 6.5, 11.4).

---

## 10. Future Extensions

* Datapack-driven `EffectData` via `CodecRegistry:46` if needed.
* `expiresAt` checked in same bucket sweep as owner validation.
* `getAffecting(BlockPos)` helper.
* `/gensokyolegacy effect list` debug command.

---

## 11. Remaining Concerns (R ≤ 8, pending short-lived)

### 11.1 Async window
Bulge offthread `getChunk(...,false)` (5 s, `P>10` only) has 1-tick window before `ChunkEvent.Load`; rare and next tick delivers.

### 11.2 Deduping
`ServerChunkCache` dedupes `getChunk` for same key; only one load event fires. `pending.remove` before `addAll` idempotent.

### 11.3 Never-loaded leak
`P≤10` ungenerated ring persists until natural generation — single-digit `pending`, cheap `isChunkLoaded` check. Large bulge force-generates offthread as intended.

### 11.4 Hex key
Closed — hex `String` matches NBT, verify `toHexString` → `parseUnsignedLong(hex,16)` round-trip once.

### 11.5 Remove
`O(1)` closed — stale pending skipped at flush, whole list discarded.

---

## 12. Synchronization: Player-Tracked Effects

### 12.1 Requirement

> Track effects player should see in server. Player should see effects if any chunk with said effect is tracked by player. Maintain a map of effects on server. Maintain a transient player list per effect. When effect is tracked, added, modified, or removed, sync data to tracking players. Client side, player should maintain a list of tracked effects.

This aligns `ChunkEffectAttachment.effectIds` / `pending` chunk-granular indexing with vanilla chunk-tracking (what `ServerPlayer` has sent).

### 12.2 Server State

Reuse `LevelEffectAttachment.byId:110` as authoritative map (`Map<UUID,AreaEffectEntry>`). Add transient (no `@SerialField`) index:

```java
// in LevelEffectAttachment.java (transient, not serialized)
private final Map<UUID, Set<ServerPlayer>> trackingPlayers = new HashMap<>();
private final Map<UUID, AreaEffectEntry> byId; // already @SerialField
```

* `trackingPlayers.get(uuid)` = players that currently track **at least one** chunk of `range.contains(chunkPos)` for that entry and have been sent the effect. Empty set means no tracking player currently needs it.
* Alternative shape `Map<ServerPlayer, Set<UUID>> perPlayer` is derived from the above; primary is per-effect for `ADD/MODIFY/REMOVE` broadcast.

Per-effect set is transient; rebuilt on demand after restart (no player tracks until they re-track chunks).

### 12.3 When a Player Tracks an Effect

“Tracked” = player’s view distance includes a chunk whose `ChunkEffectAttachment` will contain the effect (or whose `pending` will). Equivalent to `range.contains(trackedChunkPos)` and entry still in `byId`.

Triggers — only `track`/`untrack` for player status per your note (no `PlayerLoggedOut`/`ChangedDimension` separate; range is immutable, so `MODIFY` is data-only):

*In this design `WATCH` = `track`*: NeoForge `ChunkWatchEvent.Watch` (player starts tracking chunk `C`) / `ChunkWatchEvent.UnWatch` (player stops tracking `C`). Renamed to `TRACK`/`UNTRACK` below. `WATCH` was just the NeoForge event name for `track`.

1. **Effect lifecycle** — `ADD / UPDATE / REMOVE` via `AreaEffectManager` (`range` immutable; range change = `REMOVE` + `ADD` only):
   * Compute `Set<ServerPlayer> players = union of chunk trackers for all chunks in range` (via `chunkMap.getPlayers(cpos,false)`; `pending` not needed — `byId` range check suffices):
     ```
     Set<ServerPlayer> players = new HashSet<>();
     for (ChunkPos cpos : entry.range.stream()) players.addAll(getTrackingPlayers(level, cpos));
     ```
     For `ADD`: `trackingPlayers.put(uuid, players)`, send `EffectSyncPacket{ADD, entry}` to each.
     For `UPDATE` (data-only, range unchanged — per your constraint): send `Action.UPDATE` to existing `trackingPlayers.get(uuid)` (no recompute of tracker set).
     For `REMOVE`: send `Action.REMOVE {uuid}` to `trackingPlayers.remove(uuid)`.

2. **Player chunk tracking** — `TRACK`/`UNTRACK` only:
   * `TRACK chunk C by player P` (`ChunkWatchEvent.Watch`):
     ```
     for (entry : levelAtt.byId.values()) {
         if (!entry.range.contains(C)) continue;
         var set = trackingPlayers.computeIfAbsent(entry.id, k->new HashSet<>());
         if (set.add(P)) HANDLER.toClientPlayer(new EffectSyncPacket(ADD, entry), P);
     }
     ```
   * `UNTRACK chunk C by player P` (`ChunkWatchEvent.UnWatch`):
     ```
     for (entry : levelAtt.byId.values()) {
         if (!entry.range.contains(C)) continue;
         boolean stillTracks = false;
         for (ChunkPos other : entry.range.stream()) {
             if (other.equals(C)) continue;
             if (isPlayerTracking(level, P, other)) { stillTracks = true; break; }
         }
         if (!stillTracks) {
             var set = trackingPlayers.get(entry.id);
             if (set != null && set.remove(P)) {
                 HANDLER.toClientPlayer(new EffectSyncPacket(REMOVE, entry.id), P);
                 if (set.isEmpty()) trackingPlayers.remove(entry.id);
             }
         }
     }
     ```
     `UNTRACK` is the `untrack` event you specified — handles logout/dimension change implicitly (those trigger `UNTRACK` for all their chunks), so no separate `PlayerLoggedOut` handling needed.

Entry’s transient player list is thus maintained incrementally; `ADD`/`TRACK` adds, `REMOVE`/`UNTRACK-last-chunk` removes.

### 12.4 Packets

Register via `GensokyoLegacy.HANDLER:75` ( `PacketHandler` / `SerialPacketBase` as `CharDataToClient:76`, `FrogSyncPacket:89`):

```java
@SerialClass
public record EffectSyncPacket(Action action, UUID id, @Nullable AreaEffectEntry entry) implements SerialPacketBase<EffectSyncPacket> {
    public enum Action { ADD, UPDATE, REMOVE }
    // id always present; entry present for ADD/UPDATE, null for REMOVE
    // EffectData is @SerialClass base class, so TagCodec inheritance works for entry.data
    @Override public void handle(Player player) { ClientEffectTracker.onSync(this); }
}
```

* `ADD`/`UPDATE` sends full `AreaEffectEntry` (id + ownerPos + range + data) — size ~ `BlockPos(12B) + Range(16B) + data` (typically <100B) + UUID. For `R≤8` and small `M` this is tiny.
* `REMOVE` sends only `UUID` (16B).
* Consider batch `EffectSyncBatchPacket(List<EffectSyncPacket>)` for initial chunk-watch burst (player logs in and watches ~625 chunks at view distance 12) — see 12.6.

### 12.5 Client State

```java
// ClientEffectTracker.java — client-only, in src/main/java/dev/xkmc/gensokyolegacy/content/client/effect/
public final class ClientEffectTracker {
    private static final Map<UUID, AreaEffectEntry> TRACKED = new LinkedHashMap<>();
    public static void onSync(EffectSyncPacket p) {
        switch(p.action()) {
            case ADD, UPDATE -> TRACKED.put(p.id(), p.entry());
            case REMOVE -> TRACKED.remove(p.id());
        }
    }
    public static Collection<AreaEffectEntry> getTracked() { return TRACKED.values(); }
    public static List<AreaEffectEntry> getAffecting(BlockPos pos) {
        var cpos = new ChunkPos(pos);
        return TRACKED.values().stream().filter(e -> e.range.contains(cpos)).toList();
    }
    public static void clearOnDisconnect() { TRACKED.clear(); }
}
```

Used for rendering/fog/particles/sound. No per-tick cache needed on client beyond `ChunkEffectAttachment` equivalent; `TRACKED` is authoritative view.

### 12.6 Performance & Network Evaluation

**Server CPU:**

* `ADD/UPDATE/REMOVE`: `O(N * T_chunk)` where `N≤289` (`R≤8`) and `T_chunk` = avg trackers per chunk (typically 1-3). Worst `O(N * players)` if all players cluster in same range, but `N` bounded. `UPDATE` is `O(T)` only (data-only, range immutable per your constraint; no tracker set recompute).
* `TRACK` (player moves, tracks ~1-4 new chunks/tick, burst ~100 on login/teleport): naive scan `O(M)` per chunk (`M` = total effects in dimension). With `M=50` and 100 chunks → 5k `range.contains` checks (4 int compares each) → trivial. If `M` 1000+, build secondary index `Map<String,List<UUID>> chunkIndex` (hex key as `pending:118`) so `TRACK` becomes `O(effectsForThisChunk)` (0-1) instead of `O(M)`.
* `UNTRACK`: similar `O(M)` scan but must also check if player still tracks any other chunk of same effect’s range → `O(N)` per affected effect. Optimize via per-player `watchedChunks` cache or per-effect refcount `Map<UUID,Map<Player,int>>` incremented on `TRACK`/`UNTRACK`. For `R≤8` small, linear scan `N≤289` per effect is acceptable.

**Server memory:**

* Transient `trackingPlayers`: `O(M * avgTrackers)`. Worst `M=100, players=20` → 2k entries. Each set small. Acceptable.
* No duplication of `byId` data; only `Set<ServerPlayer>` references.

**Network:**

* **Steady state:** 0 packets — effects only sync on `ADD`/`UPDATE`/`REMOVE` or on first `WATCH` for that effect. Not per-tick. `ChunkEffectAttachment.getAffecting` per-tick cache (`5.3`) does not generate traffic.
* **Per-effect cost:** `ADD`/`UPDATE` sends entry to `T` tracking players → `T * sizeof(entry)` bytes. With `sizeof(entry)` ~100-200B, `T=5`, `N=289` range still only one packet per player per effect (not per chunk). So large range does not multiply network cost — track by effect, not by chunk.
* **Player join / teleport burst:** Player tracks ~625 chunks; if `M=50` effects scattered, worst `50` `ADD` packets to that player on login. At 200B each → ~10KB burst, negligible. Batch `EffectSyncBatchPacket` can coalesce to 1 packet per tick’s `TRACK` burst.
* **Movement churn:** Player walking: ~4 `TRACK`/`UNTRACK` per second, each may trigger 0-1 effect sync as they cross range boundary. So 0-4 packets/sec per moving player near effect edge. Not a spam vector.
* **Stale/duplicate sync:** `trackingPlayers` set ensures each effect sent exactly once per player while they track any chunk of its range; repeated `TRACK` of other chunks in same range does not resend (guard `set.add(P)`). `UNTRACK` only sends `REMOVE` when last chunk of that range is untracked.
* **Concern if `M` large (hundreds) and many players:** `TRACK` scan `O(M)` per chunk could be done for many players simultaneously. Mitigate with chunk→effects index. Network burst scales with `M_visible` per player.
* **Pending interaction:** Effects in `pending` (unloaded chunks) are still in `byId`, so `getTrackingPlayers` range check includes them even before chunk load — player will receive effect as soon as they track any chunk in its range, regardless of flush state. No extra sync needed after `ChunkEvent.Load` drain beyond what `TRACK` already does.

**Client CPU/memory:**

* `TRACKED` map `O(M_visible)` (effects whose range overlaps view distance). For `M_visible` small, `getAffecting(BlockPos)` linear scan `O(M_visible)` per render/tick is fine; cache per tick if called frequently.
* Memory `M_visible * entry` small.

**Alternatives / mitigations:**

* If `TRACK` `O(M)` proves heavy, replace `byId.values()` scan with maintained `Map<String,List<UUID>> chunkIndex` (hex key as `pending:118`) updated on `ADD`/`REMOVE`; then `TRACK` is `O(effectsForChunk)`.
* Batch `TRACK` burst on login: collect `List<AreaEffectEntry> toAdd` for that player and send single batch packet instead of `M_visible` individual packets.
* On `UPDATE` (data-only, range immutable), send full `EffectData` or delta if large.


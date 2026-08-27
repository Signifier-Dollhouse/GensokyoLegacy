# Area Effect System — Implementation & Usage Guide

This document reflects the current implementation in `content/attachment/area` (`ServerLevel` + `LevelChunk` capabilities via `l2core` `AttReg`). Typical `R ≤ 8` (`17×17=289` chunks per effect); larger ranges use a different system.

## 1. Components

| Class | File | Role |
|---|---|---|
| `ChunkPosRange` | `area/ChunkPosRange.java` | `record(int minCX, minCZ, maxCX, maxCZ)` — no `@SerialClass` needed for records. `contains(ChunkPos)`, `stream()`, `forEach`, `chunkCount()`, factories `ofChunks/ofRadius/ofOwner/ofBlocks/ofBoundingBox`. |
| `EffectData` | `area/EffectData.java` | `@SerialClass abstract class` — `isOwnerStillValid(ServerLevel,BlockPos,BlockState)` default `!isAir()`. Subclasses `@SerialClass` (e.g., `BeaconEffectData`). `TagCodec` handles inheritance when field handle `EffectData` is `@SerialClass`. |
| `AreaEffectEntry` | `area/AreaEffectEntry.java` | `@SerialClass` — `@SerialField UUID id, BlockPos ownerPos, ChunkPosRange range, EffectData data, long createdGameTime`. Transient `Map<UUID,Integer> trackingCounts` (player `UUID` → tracked chunk count, not serialized) with `incrementTracking(ServerPlayer)→bool first`, `decrementTracking→bool last`, `getTrackingPlayers()`, `sync(ServerLevel)`, `cleanupPlayers(ServerLevel)`, `isOwnerValid(ServerLevel)`. |
| `LevelAreaAttachment` | `area/LevelAreaAttachment.java` | `GeneralCapabilityTemplate<Level>` — `@SerialField Map<UUID,AreaEffectEntry> byId` + `Map<String,List<UUID>> pending` (hex `Long.toHexString(ChunkPos.toLong())` → `List<UUID>`, coalesced per chunk, discarded as unit). Transient `lastPendingFlushTick`. `tickValidation` `O(M/100)` per tick via `floorMod(id.hashCode(),100)==tick%100`, owner `getChunkNow` check; `tickPendingFlush` every 5s if `P>10` offthread `getChunk(...,false)`. |
| `ChunkAreaAttachment` | `area/ChunkAreaAttachment.java` | `GeneralCapabilityTemplate<LevelChunk>` — `@SerialField Set<UUID> effectIds` + transient `cachedTick/Resolved/cacheValid` (no `transient` keyword). Pure data; logic lives in holder. |
| `AreaChunkHolder` | `area/AreaChunkHolder.java` | `record(ServerLevel level, ChunkPos pos, LevelChunk chunk, ChunkAreaAttachment attachment)` — **main interface**. Factories `of(ServerLevel,ChunkPos)` nullable (`getChunk(...,false)` → `null` if not loaded, via `GLMeta.CHUNK_EFFECT.type().getOrCreate`) and `of(ServerLevel,LevelChunk)`. `addId/removeId` (invalidate cache + `setUnsaved`), `getAffecting()` `O(k)` with per-tick cache and stale prune (`byId` check + `removeAll`). Always requires `ServerLevel`. |
| `AreaEffectManager` | `area/AreaEffectManager.java` | Facade — `add/remove/getAffecting/tickValidation/tickPendingFlush/onTrack/onUntrack`. Uses `GLMeta.LEVEL_EFFECT/CHUNK_EFFECT.type().getOrCreate` (not `chunk.getData`). `getAffecting` never forces load (`holder` nullable). |
| `AreaEffectSyncPacket` | `area/AreaEffectSyncPacket.java` | `record(Action,UUID,AreaEffectEntry) implements SerialPacketBase` — `ADD/UPDATE/REMOVE`, no `@SerialClass` (records handled directly). Via `GensokyoLegacy.HANDLER` `PLAY_TO_CLIENT`. |
| `ClientAreaEffectTracker` | `area/ClientAreaEffectTracker.java` | Client `Map<UUID,AreaEffectEntry> TRACKED`, `onSync`, `getTracked`, `getAffecting(BlockPos)` filter, `clear()`. |
| `AreaEffectEvents` | `area/AreaEffectEvents.java` | `LevelTickEvent.Post` → `tickValidation` + `tickPendingFlush`; `ChunkEvent.Load` → pending drain `holder.addId`; `ChunkWatchEvent.Watch/UnWatch` → `onTrack`/`onUntrack`. |
| `ClientAreaEffectEvents` | `area/ClientAreaEffectEvents.java` | `Dist.CLIENT` `ClientPlayerNetworkEvent.LoggingOut` + `LevelEvent.Unload` (client) → `clear()`. |
| `GLMeta` | `init/registrate/GLMeta.java:48` | `LEVEL_EFFECT: CapVal<Level, LevelAreaAttachment>` `level_area` + `CHUNK_EFFECT: CapVal<LevelChunk, ChunkAreaAttachment>` `chunk_area`. |

## 2. Workflows

### Add
```
AreaEffectManager.add(level, ownerPos, ChunkPosRange.ofOwner(ownerPos, R), data) -> UUID
  - create entry(UUID.randomUUID(), ownerPos, range, data, gameTime)
  - byId.put
  - fan-out N≤289: for each ChunkPos in range
      chunk = level.getChunkSource().getChunk(x,z,false) // no force
      if chunk != null AreaChunkHolder.of(level,chunk).addId(uuid)
      else pending.computeIfAbsent(hex,->new ArrayList).add(uuid)
  - for each ServerPlayer in level.players()
      count = #{ (x,z) in range | player.getChunkTrackingView().contains(x,z) } // O(1) cheap view check
      if count>0 { entry.trackingCounts.put(playerUUID,count); send ADD }
```
`O(P*N)` with cheap `contains`, `P` players, `N≤289`.

### Remove
```
AreaEffectManager.remove(level, uuid) -> bool
  entry = byId.remove(uuid); if null return false
  for (playerId : copy(entry.trackingPlayers)) { player = server.getPlayerList().getPlayer(playerId); if(player!=null) send REMOVE }
  entry.trackingCounts.clear()
  // no chunk/pending scan — pending stale skipped via byId.containsKey at flush, chunk stale pruned at next getAffecting
```
`O(T)` where `T=tracking players` (typically 0-3), `O(1)` plus lazy.

### Fetch (server, chunk-granular)
```
AreaChunkHolder.of(level, pos) // nullable, no force
  if null return List.of()
  holder.getAffecting() // O(k) miss (k=effects for this chunk 0-3) + stale prune, O(1) hit via cachedTick==gameTime
```
Always via holder; never `level.getChunk(pos)` that forces load.

### Pending flush
*Primary:* `ChunkEvent.Load` → `pending.remove(hex)` → `holder.addId` per `UUID` where `byId.containsKey` (stale skipped), whole list discarded.
*Fallback:* `tickPendingFlush` every 5s (`lastPendingFlushTick`) if `pending.size()>10`:
```
snapshot = copy(pending.keySet())
for key in snapshot
  posLong = parseUnsignedLong(key,16); cpos = new ChunkPos(posLong)
  if (getChunkNow(cpos) != null) { // already loaded but still pending (race)
      pendingIds = pending.remove(key); holder = of(level,cpos); for uid in pendingIds if(byId.containsKey) holder.addId(uid);
  } else {
      getChunk(cpos.x,cpos.z,ChunkStatus.FULL,false) // on main thread, false schedules offthread per ServerChunkCache, forcing generation expected
      // drain happens in future ChunkEvent.Load
  }
```

### Owner validation
Spread `O(M/100)` per tick: `tick = gameTime %100`, `bucket = floorMod(entry.id.hashCode(),100)`, only entries where `bucket==tickBucket` are checked. For each, `getChunkNow(ownerPos)` null → skip; else `entry.isOwnerValid` → collect toRemove → `byId.remove` + `REMOVE` to `trackingPlayers` + `clear`. Every 100 ticks also `entry.cleanupPlayers(level)` removes offline `UUID`s (`getPlayer==null`).

### Sync (track/untrack)
* `TRACK chunk C by P` (`ChunkWatchEvent.Watch`): `holder = of(level,C)` (`O(k)`); for `entry : holder.getAffecting()` if `incrementTracking(P)` (0→1) send `ADD`.
* `UNTRACK`: same holder, `decrementTracking(P)` → send `REMOVE` only when last chunk (`count 1→0`). Counter fixes “untracking any chunk untracks effect” bug; iterating only affecting data `O(k)` not all `M`.
* `UPDATE` data-only, range immutable (`REMOVE`+`ADD` for range change): `entry.data = newData; entry.sync(level)` → `UPDATE` to `trackingCounts.keySet()`.

## 3. Usage Guide

### Define a payload
```java
@SerialClass
public class MyAuraData extends EffectData {
    @SerialField public int strength;
    @SerialField public int color;
    public MyAuraData() {}
    public MyAuraData(int strength, int color) { this.strength = strength; this.color = color; }
    @Override public boolean isOwnerStillValid(ServerLevel level, BlockPos pos, BlockState state) {
        return state.is(MyBlocks.AURA_BLOCK.get()) && level.getBlockEntity(pos) instanceof AuraBlockEntity;
    }
}
```

### Add an effect (server, owner chunk must be loaded)
```java
ServerLevel level = (ServerLevel) player.level();
BlockPos ownerPos = blockEntity.getBlockPos(); // must be loaded
ChunkPosRange range = ChunkPosRange.ofOwner(ownerPos, 4); // R=4 → 9x9=81 chunks
EffectData data = new MyAuraData(5, 0xFF0000);
UUID id = AreaEffectManager.add(level, ownerPos, range, data);
// id can be stored in BlockEntity for later remove/update
```

### Query affecting effects (server, no force load)
```java
// via holder — preferred
AreaChunkHolder holder = AreaChunkHolder.of(level, new ChunkPos(pos));
if (holder != null) {
    List<AreaEffectEntry> affecting = holder.getAffecting();
    for (AreaEffectEntry e : affecting) { /* use e.data, e.ownerPos */ }
}
// or via manager
List<AreaEffectEntry> list = AreaEffectManager.getAffecting(level, new ChunkPos(pos));
List<AreaEffectEntry> list2 = AreaEffectManager.getAffecting(level, blockPos);
// client side
List<AreaEffectEntry> clientList = ClientAreaEffectTracker.getAffecting(blockPos);
```

### Update data (range immutable)
```java
AreaEffectEntry entry = AreaEffectManager.get(level, id);
if (entry != null) {
    entry.data = new MyAuraData(10, 0x00FF00);
    entry.sync(level); // sends UPDATE to tracking players via entry's UUID-keyed counts
}
```

### Remove
```java
AreaEffectManager.remove(level, id); // O(1), pending lazily skipped, chunk pruned on next getAffecting
// or on owner block break, let tickValidation auto-remove within ≤100 ticks (5s bucket) as fallback;
// for instant, call remove from Block.onRemove / BlockEntity.setRemoved
```

### Owner block removal fallback
Tick `O(M/100)` per tick handles missed `onRemove` (explosion, `/setblock`). No extra code needed, but for instant feedback call `remove` in your block:
```java
@Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
    if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) {
        // find and remove entries with this ownerPos — iterate byId
        var att = GLMeta.LEVEL_EFFECT.type().getOrCreate(sl);
        att.getById().values().removeIf(e -> e.ownerPos.equals(pos) && AreaEffectManager.remove(sl, e.id));
    }
    super.onRemove(state, level, pos, newState, moved);
}
```

## 4. Performance

* **Add** `O(P*N)` cheap `contains` checks, `N≤289`. **Remove** `O(T)`. **Fetch** `O(k)` miss / `O(1)` hit. **Validation** `O(M/100)`/tick, **pending** `ChunkEvent.Load` `O(L)` per chunk + 5s bulge `O(P)` only if `P>10`. **Track** `O(k)` per chunk via holder, `UNTRACK` `O(k)`. Memory `effectIds` small, `pending` short-lived hex strings, `trackingCounts` `O(M*players)` transient `UUID` keys (cleaned every 5s for offline).

## 5. Registration

`GLMeta.java:48` already registers `LEVEL_EFFECT`/`CHUNK_EFFECT`. Packets `AreaEffectSyncPacket` via `GensokyoLegacy.HANDLER` `PLAY_TO_CLIENT`. Events `AreaEffectEvents` (GAME bus) and `ClientAreaEffectEvents` (CLIENT `LoggingOut`/`Level.Unload`).


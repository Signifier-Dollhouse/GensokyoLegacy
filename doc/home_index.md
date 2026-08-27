# Home and Index System (Preset vs Custom) — Implementation & Usage Guide

This document reflects the current implementation in `content/attachment/home/`, `content/attachment/index/`, `content/attachment/datamap/`, `content/block/base/`, `content/client/structure/` and `init/registrate/GLMeta`.

## 1. Components

| Class | File | Role |
|---|---|---|
| `IHomeHolder` | `content/attachment/home/core/IHomeHolder.java:17` | Interface unifying preset/custom. `find(sl,pos)` (custom 3×3 chunk scan then worldgen `structureManager`), `of(sl,key)`, `of(sl,SmartYoukaiEntity)` via `StructureKey.of(entity)`. Methods `isValid/supportEntity/isInRoom/getRandomPosInRoom|Bound/getContainersAround/getChairsAround/getWanderCenter/getWanderBaseRadius/tick/toBoundPacket`. |
| `StructureAttachment` | `content/attachment/home/core/StructureAttachment.java:15` | `GeneralCapabilityTemplate<LevelChunk>` (`GLMeta.STRUCTURE` CapVal `structure`). `@SerialField Map<StructureKey,StructureHomeData> data` + `@SerialField Map<BlockPos,CustomHomeData> custom` (LinkedHashMap). |
| `StructureHomeHolder` | `content/attachment/home/structure/StructureHomeHolder.java` | `record(sl, chunk, key, config, attachment, data)` implements `IFixableHomeHolder`. Factory `of(sl,key)` via `sl.registryAccess holderOrThrow + chunk.getData(GLMeta.STRUCTURE)` + `computeIfAbsent`. Fallback synthetic `StructureConfig` with CIRNO if DataMap missing. |
| `StructureHomeData` | `content/attachment/home/structure/StructureHomeData.java:19` | Lazy `StructureStart start`, `StructurePiece piece`, `StructureCache.Builder cacheBuilder`, `StructureCache cache`, `IntegrityVerifier verifier`, `AbnormalCache abnormal`, `List<BlockPos> containers/chairs`. `checkInit/tick/getRoom|House|TotalBound/popFix/getAbnormality`. |
| `StructureCache` | `content/attachment/home/structure/StructureCache.java` | `BlockState[] palette` + `int[] raster` snapshot of `houseBound`. `Builder(level, houseBound)` incremental scan `SETUP_SCAN=256`/tick via `bound.resolve(i) → palette+raster`. |
| `IntegrityVerifier` | `content/attachment/home/structure/IntegrityVerifier.java` | Compares `level.getBlockState` vs `palette[raster[resolve(pos)]]`. Random `VERIFY_SCAN=500`/tick (only `VERIFY_FETCH=200` actually fetched via `resolve`). Categorises into `AbnormalCache`. |
| `AbnormalCache` | `content/attachment/home/structure/AbnormalCache.java` | 3 `IntOpenHashSet` `air/primary/secondary` (offsets inside `houseBound`). `pop(count,FixStage)` priority `air→primary→secondary` gated per stage. |
| `StructureBound` | `content/attachment/home/core/StructureBound.java` | `BoundingBox` helpers: `isInside(pos)`, `getSize()`, `resolve(pos/index)`, `getRandomPos`. |
| `CustomHomeHolder` | `content/attachment/home/custom/CustomHomeHolder.java` | `record(sl, chunk, key, att, data)` implements `IHomeHolder`. `of(sl, BlockPos anchor)` + `create(sl, BlockPos)` via `RoomVerifier`. `supportEntity→true`. |
| `CustomHomeData` | `content/attachment/home/custom/CustomHomeData.java` | `@SerialField BlockPos rootPos`, `@SerialField RoomData room`. `getTotalBound/isInRoom/getRandomPosInRoom|Bound` from `RoomData.bound`. `tick` noop. |
| `RoomVerifier` | `content/attachment/home/custom/RoomVerifier.java:15` | BFS flood-fill column scanner. Limits `MAX_HEIGHT 15`, `MAX_SIZE 48`. `visitColumn(pos)` vertical scan 2-phase (`toFloor/toRoof`), `step()` expands 4 horizontal dirs (needs `wall>1` continuous). Collects `IBlockConsumer` chairs/containers/beds. `run(pos)→RoomData`. |
| `RoomData` | `content/attachment/home/custom/RoomData.java` | `bound(6 ints)` + `ColumnData[x][z].list int[][2]` per-column Y intervals (merge via `addHeight`). `isInside(BlockPos)` via column check + `WALK` AABB. `acceptColumn`, `getColumn`. |
| `StructureKey` | `content/attachment/index/StructureKey.java:16` | `record(structure RL, dim RL, pos BlockPos)`. `CUSTOM` constant, `custom(dim,pos)`, `of(YoukaiEntity)`, `support(config)` `custom||!equals` (inverted), `getStructure()/getDim()`. |
| `IndexStorage` | `content/attachment/index/IndexStorage.java` | Global `@SerialClass BaseSavedData` (`gensokyolegacy_reference_index` in overworld `DataStorage`). `LinkedHashMap<StructureKey, StructureRefData> structureData`. `get(sl)` rebinds `level`, `getOrCreate(key)→StructureRef`. |
| `BedRefData` | `content/attachment/index/BedRefData.java:24` | Per-entity-type bed state machine (see structure_bed_entity.md). |
| `StructureWand` | `content/item/debug/StructureWand.java` | Creates/edits custom homes via `RoomVerifier.run(anchor.relative(face))`, requires ≥1 chair+container+bedHead, `setLink`, `chunk.setUnsaved`. |
| `PerformanceConstants` | `content/attachment/home/core/PerformanceConstants.java` | `SETUP_SCAN 256, VERIFY_SCAN 500, VERIFY_FETCH 200, COMMAND_PLACE_ONCE 128, COMMAND_PLACE_STEP 64`. |

## 2. Storage Model

```
LevelChunk ── GeneralCapabilityTemplate ── StructureAttachment (GLMeta.STRUCTURE)
 ├── data: Map<StructureKey, StructureHomeData>   // preset, key = (structure id, dim, piece locator)
 └── custom: Map<BlockPos, CustomHomeData>        // custom, key = anchor BlockPos (wand click)

Overworld DataStorage "gensokyolegacy_reference_index" ── IndexStorage (BaseSavedData, TagCodec)
 └── structureData: Map<StructureKey, StructureRefData>
      └── StructureRefData: Map<EntityType<?>, BedRefData>
           └── BedRefData: entityId, bedPos, lastEntityTickedTime, lostEntityTick

YoukaiEntity ── HomeModule.home:StructureKey (@SerialField entity NBT)
LocatedBlockEntity ── key:StructureKey (@SerialField BE NBT)
```

Chunk cap persists via `LevelChunk` NBT; `IndexStorage` persists via `level.dat`; both auto-dirtied via `chunk.setUnsaved(true)` / `setDirty()`. Structures and BEs are immovable, so neither map ever needs positional migration — keys are stable for the life of the chunk.

## 3. StructureKey

```java
record StructureKey(RL structure, RL dim, BlockPos pos)
  CUSTOM = gensokyolegacy:custom_structure
  custom(ResourceKey<Level> dim, BlockPos anchor) → new StructureKey(CUSTOM, dim.location(), anchor)
  new(ResourceKey<Structure> s, ResourceKey<Level> d, BlockPos locator) → (s.location(), d.location(), pos)
  of(YoukaiEntity e) → e.getModule(HomeModule.class).map(HomeModule::home)
  getStructure() → ResourceKey.create(Registries.STRUCTURE, structure)
  getDim()       → ResourceKey.create(Registries.DIMENSION, dim)
  support(CharacterConfig cfg) → structure.equals(CUSTOM) || !cfg.structure().equals(structure)
  isCustom()     → structure.equals(CUSTOM)
```

Used as unified identifier for both preset and custom homes; `equals/hashCode` from record guarantees map stability after reload.

## 4. Preset (Worldgen) vs Custom (Player-Built)

| Axis | Preset (`StructureHomeHolder` + `StructureHomeData`) | Custom (`CustomHomeHolder` + `CustomHomeData` + `RoomData`) |
|---|---|---|
| Source | Vanilla `StructureManager.getStructureWithPieceAt(pos, Structure)`; requires `GLStructureGen` worldgen definition | `StructureWand.useOn` → `RoomVerifier.run(anchor.relative(face))` flood-fill |
| Key | `(registry STRUCTURE id, dimension, piece.getLocatorPosition())` | `(CUSTOM, dimension, anchor BlockPos)` |
| Bounds | Derived from `StructurePiece.getBoundingBox()` shrunk by `StructureConfig`: `roomBound = box.shrink(xzRoom,topRoom,floorRoom)`, `houseBound = …shrink(xzHouse,topHouse,floorHouse)`, `totalBound = start.getBoundingBox().inflatedBy(-12)` | `RoomData.bound` from flooded columns (x0,y0,z0..x1,y1,z1, size capped 48); `roomBound = bound`, `house/total = bound.inflatedBy(1)` |
| Config | `StructureConfig` datamap per `Structure` (`entities Set`, `xzRoomShrink` etc., `outside/primary/wouldFix` tags) | No config; `supportEntity→true`, `isOutside = canSeeSky` only, no `primary/wouldFix` tags |
| `supportEntity` | `config.entities().contains(type)` (via `GLStructureGen` → `StructBed` → `config.addEntity`) | `true` for all types |
| Geometry cache | `StructureCache` palette+raster built lazily `256 blocks/tick` over `houseBound` | `RoomData.columns[x][z]` column intervals, `ClusterBitSet` for edge rendering |
| Integrity | `IntegrityVerifier` random sample `500/tick` (fetch `200`) → `AbnormalCache (air/primary/secondary)` → `popFix/doFix` per `FixStage` (`PATH→PRIMARY→SECONDARY→ALL`) via `BlockFix.fix()` | No integrity; `tick()` noop; `getAbnormality` always `(-1,-1,-1)`; `SCAN` re-runs verifier, `DELETE` removes `attachment.custom`+`IndexStorage` entry |
| Search helpers | `HomeSearchUtil.searchBlock(cache, isValidChest/Chair, roomBound, level, center, rxz3,ry3, trail32/12)` random linear trails | Same helper, roomBound from `RoomData` |
| Persistence | `StructureAttachment.data` map, chunk at locator | `StructureAttachment.custom` map, chunk at anchor |
| Sync packets | `S→C StructureBoundUpdateToClient(key,total,house,room)` | `S→C CustomStructureBoundUpdateToClient(key,RoomData)` |
| Limits | Throttled by `PerformanceConstants` (setup/verify counts, repair bulk `128/64`) | `RoomVerifier MAX_HEIGHT 15, MAX_SIZE 48`; fails `NO_FLOOR/NO_ROOF/TOO_THIN`; requires ≥1 chair, container, bed Head |
| Current worldgen | `GLStructureGen.initStructures()` returns `List.of()` → empty `structure_config`/`character_config` DataMaps; fallback synthetic `StructureConfig` (CIRNO) injected when `DataMap` missing (`StructureHomeHolder.TODO`) | Fully functional via wand |

## 5. Workflows

### 5.1 Lookup (`IHomeHolder`)

```java
// entity → home
IHomeHolder.of(sl, (SmartYoukaiEntity)youkai) // StructureKey.of(youkai) → StructureHomeHolder.of else CustomHomeHolder.of(pos)

// pos → home (LocatedBlockEntity + StructureWand hover)
IHomeHolder.find(sl, pos)
  // A) custom first (3×3 chunks, offsets ±16)
  for ix/iz in -1..1:
    chunk = sl.getChunkAt(pos.offset(ix*16,0,iz*16))
    att = chunk.getData(GLMeta.STRUCTURE.get())
    for (anchor,data : att.custom) if data.getTotalBound().isInside(pos)
        return new CustomHomeHolder(sl, chunk, StructureKey.custom(dim,anchor), att, data)

  // B) worldgen fallback
  map = sl.structureManager().getAllStructuresAt(pos)
  for holder : map.keySet():
    start = manager.getStructureWithPieceAt(pos, holder)
    if !valid || pieces.empty() → continue
    root = start.getPieces().getFirst().getLocatorPosition()
    // Validate via registry: reg.getHolder(reg.getId(start.getStructure()))
    return StructureHomeHolder.of(sl, new StructureKey(holderKey, dim, root))
  return null;

// key → home
IHomeHolder.of(sl, key) → StructureHomeHolder.of(sl,key) ?? CustomHomeHolder.of(sl,key.pos())
```

### 5.2 Custom Home Creation (`StructureWand.useOn`) — immovable

> Structures and BEs are immovable: custom homes are placed once via the wand and never translated. Deleting a bed or room destroys blocks in place; the paired bed-half invariant (destroying either half → other half `AIR` via `YoukaiBedMethods.updateShape` / `YoukaiBedBlock.playerWillDestroy`) guarantees no orphan half remains.

#### Steps

```java
ServerLevel sl = (ServerLevel)level;
BlockPos anchor = pos.relative(face); // face-clicked neighbour
IBlockConsumer consumer = new Collector(chairs, containers, beds); // visitBlock hook
RoomVerifier ver = new RoomVerifier(sl, player, consumer);
// 1) visitColumn(anchor): vertical scan BlockData.cull bits from collisionShape.bounds
//    Phase floor: walk DOWN until culling DOWN or solid/below→solid|UP-culling
//    Phase roof:  walk UP   until culling UP   or solid/above→solid|DOWN-culling
//    Fail TOO_THIN if floor+roof==0, NO_FLOOR/NO_ROOF if exceeds MAX_HEIGHT
// 2) BFS: queue valid columns; step() for 4 horizontal dirs skip Y,
//    skip if dir culled on current, door→register, next solid/door/opposite-culled→skip,
//    wall++ per non-culled cell; when wall>1 visitColumn(next)
// 3) run(): loop until queue empty; cap MAX_SIZE 48 on any axis else FATAL msg
// 4) consumer must have chairs>0 && containers>0 && beds(HEAD)>0 else msg
// 5) RoomData ans = new RoomData(x0,y0,z0,x1,y1,z1); for col: ans.acceptColumn(colPos, col.length)
// 6) CustomHomeHolder holder = CustomHomeHolder.create(sl, anchor); holder.data().setData(anchor, ans)
// 7) for bedPos in beds: be = sl.getBlockEntity(bedPos); be.setLink(holder.key()); be.sync()
// 8) chunk.getData(GLMeta.STRUCTURE.get()).custom.put(anchor, data); chunk.setUnsaved(true);
// 9) player.sendSystemMessage("Home registered successfully!")
```

RoomData storage: `bound` + `ColumnData[xSize][zSize].list int[][2]` merged intervals.

### 5.3 Preset Home Tick (`StructureHomeHolder.tick → StructureHomeData.tick`)

```java
if (piece==null) { // lazy init
  var st = sl.registryAccess().holderOrThrow(key.getStructure()).value();
  var start = sl.structureManager().getStructureWithPieceAt(key.pos(), st);
  if (start.getStructure()==st && !pieces.empty()) { this.start=start; piece=pieces.getFirst(); }
  return; // fallback synthetic config injected by holder if DataMap null
}
if (cache==null) {
  if (cacheBuilder==null) cacheBuilder=new StructureCache.Builder(sl, getHouseBound(config));
  cacheBuilder.tick(); // 256 blocks/tick: resolve(i)→pos→level.getBlockState→palette map + raster[i]=paletteIdx
  if (cacheBuilder.isDone()) { cache=cacheBuilder.build(); chunk.setUnsaved(true); }
} else {
  if (verifier==null) {
    verifier=new IntegrityVerifier(holder, houseBound, roomBound, cache, abnormal);
    if (!verifier.isValid()) { cache=null; cacheBuilder=null; verifier=null; return; } // size mismatch → reset
  }
  if (verifier.tick()) chunk.setUnsaved(true); // random 500 samples, fetch 200 via resolve, compare level state vs palette[raster[idx]] → process → abnormal.addAir/Primary/Secondary
}

// Bounds:
//   roomBound  = pieceBox.shrink(config.xzRoomShrink(), topRoom..., floor...)
//   houseBound = pieceBox.shrink(config.xzHouse..., topHouse..., floor...)
//   totalBound = start.getBoundingBox().inflatedBy(-12) (debug outer)
//   isOutside(level,pos) = canSeeSky || outsideBlock tag (preset) / canSeeSky only (custom)
```

Repair: `AbnormalCache.pop(count, stage)` priority `air→primary→secondary` gated by `FixStage` (PATH only air, PRIMARY stops after primary, SECONDARY after secondary, ALL all). Each offset → `BlockFix(pos, state)`; `fix()` handles `ICustomFixBlock.onFix`, `DoorBlock` halves, skips `YoukaiBedBlock`; bulk `doFix` `COMMAND_PLACE_ONCE 128` or looping `COMMAND_PLACE_STEP 64` when no player nearby.

### 5.4 Bed ↔ Home ↔ Entity Glue (see structure_bed_entity.md)

```java
// Bed side: LocatedBlockEntity once (immovable → no relocate) + YoukaiBedBlockEntity HEAD per-tick
// → IndexStorage.getOrCreate(key).blockTick → BedRefData (inRoom check, same-type dupe handling, respawn via CharacterConfig.create)
//   bed destruction: sl.removeBlock(HEAD) → updateShape makes FOOT AIR atomically, so no stale half persists to next tick
// → IHomeHolder.of(key).tick() (cache/verifier)
// Entity side: HomeModule.tickServer → BedRefData.entityTick → liveness/duplicate discard
// Sensor: YoukaiUpdateHomeSensor 80t → Memory HOME = GlobalPos(dim, bedPos) for YoukaiSleepTask/YoukaiGoHomeTask etc.
```

### 5.5 Networking

| Packet | Dir | Trigger |
|---|---|---|
| `StructureBoundUpdateToClient` | S→C | `StructureHomeHolder.toBoundPacket()` on wand hover / chunk load |
| `CustomStructureBoundUpdateToClient` | S→C | `CustomHomeHolder.toBoundPacket()` includes `RoomData` |
| `StructureInfoUpdateToClient` | S→C | response to `StructureInfoRequestToServer` (poll every 20t via `StructureInfoClientManager.tooltip` hover check `house Box.isInside(pos)`) + `RoomVerifier` alloc |
| `StructureInfoRequestToServer` | C→S | client hover request |
| `StructureRepairToServer(FixStage)` | C→S | `StructureRepairManager.openScreen` → `StructureFixScreen` (preset) buttons; handler `if ALL then loop doFix(64) else single doFix(128)` |
| `StructureEditToServer(Edit.SCAN/DELETE)` | C→S | `StructureCustomizeScreen` (custom) SCAN re-runs `RoomVerifier`, DELETE removes `att.custom` + `IndexStorage.remove(key)` |

All via `GensokyoLegacy.HANDLER` `PLAY_TO_CLIENT/SERVER` with l2serial codecs.

## 6. Usage Guide

### Register a preset structure

```java
// in GLStructureGen.initStructures() — currently empty; populate e.g.:
return List.of(new StructStructure(
    GensokyoLegacy.loc("cirno_nest"),
    TagKey.create(Registries.BIOME, GensokyoLegacy.loc("has_structure/cirno_nest")),
    24, 8, // spacing, separation
    StructureConfig.builder()
      .room(1,1,0).house(2,1,1)
      .outside(OUTSIDE_TAG).primary(PRIMARY_TAG).wouldFix(WOULD_FIX_TAG)
      .entities(Set.of(GLEntities.CIRNO.get())),
    List.of(new StructBed(GLEntities.CIRNO, CharacterConfig.forStructure(6000,12000,12,30), blockHolder)),
    new StructSimpleBuilding(List.of(new SetDataProcessor(...)), Map.of(EntityType.ZOMBIE, 2))
));
// GLStructureTagGen: add biome entries to has_structure/cirno_nest.json is handled via config
```

### Create a custom home via code

```java
RoomVerifier ver = new RoomVerifier(sl, null, null);
RoomData room = ver.run(anchorPos);
if (room != null) {
    var holder = CustomHomeHolder.create(sl, anchorPos); // transient
    holder.data().setData(anchorPos, room);
    var att = sl.getChunkAt(anchorPos).getData(GLMeta.STRUCTURE.get());
    att.custom.put(anchorPos, holder.data());
    sl.getChunkAt(anchorPos).setUnsaved(true);
}
```

### Query home from code

```java
IHomeHolder home = IHomeHolder.find(sl, bedPos);
if (home != null && home.supportEntity(GLEntities.REIMU.get())) {
    Vec3 wander = home.getWanderCenter();
    Vec3 randomInRoom = home.getRandomPosInRoom(youkai);
    BlockPos chest = home.getContainersAround(youkai.blockPosition());
    boolean inside = home.isInRoom(blockPos.above());
    SimplePacketBase boundPkt = home.toBoundPacket(); // for debug render
}
```

### Repair control

```java
// server side
IHomeHolder home = IHomeHolder.of(sl, key);
if (home instanceof IFixableHomeHolder fixable) {
    var fixes = fixable.popFix(1, FixStage.ALL); // peek 1
    if (!fixes.isEmpty()) fixes.getFirst().fix(sl);
    fixable.doFix(sl, FixStage.ALL, PerformanceConstants.COMMAND_PLACE_ONCE); // bulk 128
}
```

## 7. Performance & Limits

- **Immovability simplifies caching.** Because structures/BEs never move, `StructureAttachment.data/custom` keys are stable; no migration or invalidation is needed after placement. The one-shot `LocatedBlockEntity.located` bind is sound, and paired bed destruction (either half → other half `AIR` via `updateShape`) prevents stale-half leaks.
- Preset cache build spreads `SETUP_SCAN 256` blocks/tick over `houseBound` volume → multi-second spread for large houses; no main-thread hitch.
- Integrity sample is statistical not exhaustive: `500` random picks/tick, only `200` fetched, half in `houseBound` half in `roomBound` via `StructureBound.resolve`; TPS-safe but slow to detect full damage (need many ticks).
- Custom homes have no verifier; `RoomVerifier` runs synchronously on wand use — large rooms (48³) involve ~2k columns × height scan + queue; acceptable for rare manual action but not per-tick.
- `IHomeHolder.find` custom scan is `3×3=9` chunk `getData` per locate (bed HEAD once + hover poll 20t); cheap but uses `getChunkAt` (loads chunk if absent — custom lookup can force-load surrounding chunks).
- `StructureAttachment` is per-chunk cap; invalid structures with no matching `DataMap` inject synthetic CIRNO config (TODO) — masks missing registration errors silently.


# Structure-Bed-Entity Framework — Implementation & Usage Guide

This document reflects the current implementation in `content/block/bed/`, `content/block/base/`, `content/attachment/index/`, `content/attachment/datamap/`, `init/data/structure/`, and `content/entity/module/HomeModule`.

## 1. Components

| Class | File | Role |
|---|---|---|
| `YoukaiBedBlock` | `content/block/bed/YoukaiBedBlock.java` | `DelegateEntityBlockImpl` flat 2/16 double-bed (HEAD/FOOT via `YoukaiBedMethods`). Tag `beds`, `isBed=true`, bounce 0.66. Destroying either half auto-removes the other (updateShape → `AIR` + creative `playerWillDestroy`). |
| `YoukaiBedMethods` | `content/block/bed/YoukaiBedMethods.java` | `record` with `updateShape` (neighbour mismatch → `AIR`, so paired half vanishes) / `setPlacedBy` / `getStateForPlacement` for horizontal double-bed (`BedPart`). |
| `FlatBedShape` | `content/block/bed/FlatBedShape.java` | `VoxelShape 0,0,0-16,2,16` + datagen `bed_head/foot` models. |
| `LocatedBlockEntity` | `content/block/base/LocatedBlockEntity.java:16` | `@SerialClass BaseBlockEntity` + `Tickable`. `@SerialField StructureKey key`, `located` flag. One-shot `tick()` → `IHomeHolder.find` + `BedData` + `supportEntity` → `key`. Structures and this BE are immovable, so the one-shot binding never needs re-resolution. |
| `YoukaiBedBlockEntity` | `content/block/bed/YoukaiBedBlockEntity.java:25` | `@SerialClass` extends `LocatedBlockEntity`. Server `tick()` (HEAD only) → `IndexStorage.blockTick` + `IHomeHolder.tick`. Debug `onDebugClick`/`getDebugPacket`. |
| `BedData` | `content/attachment/datamap/BedData.java` | Datamap `Block → EntityType`. `record(EntityType<?> type)`, `of(Block)` via `GLMeta.BED_DATA`. |
| `CharacterConfig` | `content/attachment/datamap/CharacterConfig.java` | Datamap `EntityType → CharacterConfig(structure RL, discardTime, respawnTime, wanderRadius, noPlayerVanishTime)`. `of(EntityType)`, `create(type,sl,bedPos,key)` spawns youkai. |
| `StructureConfig` | `content/attachment/datamap/StructureConfig.java` | Datamap `Structure → StructureConfig(entities, room/house shrink, outside/primary/wouldFix tags)`. |
| `StructureKey` | `content/attachment/index/StructureKey.java:16` | `record(structure RL, dim RL, pos BlockPos)`. `CUSTOM="gensokyolegacy:custom_structure"` constant. `support(config)` / `isCustom()` / `of(YoukaiEntity)` via `HomeModule`. |
| `IndexStorage` | `content/attachment/index/IndexStorage.java` | `@SerialClass extends BaseSavedData` (`gensokyolegacy_reference_index` in overworld `DataStorage`). `LinkedHashMap<StructureKey, StructureRefData>`. `get(sl)` + `getOrCreate(key)`. |
| `StructureRefData` | `content/attachment/index/StructureRefData.java` | Per-structure multi-entity map `Map<EntityType, BedRefData>`. `blockTick()` once/tick delegates to typed `BedRefData`. |
| `BedRefData` | `content/attachment/index/BedRefData.java:24` | `@SerialClass` per-entity-type state: `UUID entityId`, `BlockPos bedPos`, `lastEntityTickedTime`, `lostEntityTick`. `blockTick`/`entityTick`/`onEntityDie`/`onDebugClick`. |
| `HomeModule` | `content/entity/module/HomeModule.java` | `@SerialClass AbstractYoukaiModule` `@SerialField StructureKey home`. `tickServer() → BedRefData.entityTick` else discard; `onKilled()`. |
| `StructStructure` | `init/data/structure/StructStructure.java` | Datagen descriptor `id, biomesTag, spacing, separation, StructureConfig.Builder, List<StructBed>, StructBuilding`. |
| `StructBuilding` | `init/data/structure/StructBuilding.java` | Sealed `StructSimpleBuilding` (processors, spawns) / `StructJigsawBuilding` (maxDepth, Part list, spawns) → `GLSinglePiece extends SinglePoolElement`. |
| `GLStructureGen` | `init/data/structure/GLStructureGen.java:19` | Bootstraps `PROCESSOR_LIST/TEMPLATE_POOL/STRUCTURE/STRUCTURE_SET` via `L2Registrate DataProviderInitializer`; fills 3 `DataMapReg` in `dataMap()`. Currently `initStructures()` empty. |

## 2. Relationships

```
Datagen: StructStructure(id, config, beds:List<StructBed>, building:Simple/Jigsaw)
  │ contains
  ├─▶ StructBed(entityHolder, CharacterConfig, Holder<Block>... beds)  ─┬─▶ GLMeta.BED_DATA  Block→BedData
  └─▶ StructBuilding ──▶ GLSinglePiece / processors / spawns            ├─▶ GLMeta.ENTITY_DATA EntityType→CharacterConfig(withId)
                                                                         └─▶ GLMeta.STRUCTURE_DATA Structure→StructureConfig
                                                                          → Registries PROCESSOR_LIST / TEMPLATE_POOL / STRUCTURE / STRUCTURE_SET

Runtime:
  YoukaiBedBlock (+ YoukaiBedMethods + FlatBedShape) ── Has BlockEntity YoukaiBedBlockEntity
                  └─ LocatedBlockEntity.key:StructureKey ──▶ IHomeHolder.find(pos)  (3×3 chunk custom scan, then structureManager)
  IndexStorage (global, overworld DataStorage) ──▶ StructureRefData ──▶ BedRefData (per EntityType)
                  ▲ blockTick via YoukaiBedBlockEntity.tick(HEAD)        ▲ entityTick via HomeModule.tickServer
                                                                         └─▶ CharacterConfig.create → new YoukaiEntity(HomeModule.home=key, restriction)

HomeModule (per-YoukaiEntity) ←── StructureKey.of(entity) ──▶ BedRefData.of(sl,key,type) ──▶ YoukaiUpdateHomeSensor → Brain HOME
```

## 3. Workflows

### 3.1 Registration (DataMap + Worldgen)

```java
// GLStructureGen.dataMap(pvd) — currently dormant (List.of())
for (StructStructure e : STRUCTURES) {
  for (StructBed bed : e.beds()) {
    for (Holder<Block> b : bed.bed()) bedReg.add(b, new BedData(bed.entity().value()));
    entityReg.add(bed.entity(), bed.data().withId(e.id()));
    config.addEntity(bed.entity().value());
  }
  structureReg.add(e.id(), config.build()); // StructureConfig with shrink + tags
}
// hard-coded active bindings (3 beds):
bedReg.add(BEDS[CIRNO], new BedData(CIRNO_ENTITY));
bedReg.add(BEDS[REIMU], new BedData(REIMU_ENTITY));
bedReg.add(BEDS[RUMIA], new BedData(RUMIA_ENTITY));

// GLStructureGen.init(init) bootstraps 4 registries from StructBuilding:
//   PROCESSOR_LIST: SetDataProcessor (marks data markers)
//   TEMPLATE_POOL: single pool with GLSinglePiece entries (isRoad flag)
//   STRUCTURE: JigsawStructure with biomes tag
//   STRUCTURE_SET: RandomSpread(spacing, separation, LINEAR, hash)
```

Structure tags written via `GLStructureTagGen` per `has_structure/<id>.json`.

### 3.2 Bed Placement and Paired Destruction

```java
// YoukaiBedMethods.setPlacedBy(level,pos,state,placer,stack)
//   dir = placer facing
//   HEAD at pos.relative(dir), FOOT at pos (or vice versa per BlockState PART)
//   both placed with PART property; isBed=true → vanilla bed mechanics
// Shape: FlatBedShape 16×2×16; isPathfindable false; getSeed uses HEAD pos

// YoukaiBedMethods.updateShape(block,current,state,dir,HORIZONTAL_FACINGState,level,currentPos,HORIZONTAL_FACINGPos)
//   if dir == getNeighbourDirection(PART, FACING)
//     return HORIZONTAL_FACINGState.is(block) && PART mismatched ? current : AIR
//   // breaking one half → neighbour updateShape on the other half returns AIR, so both halves vanish

// YoukaiBedBlock.playerWillDestroy(level,pos,state,player) — creative only:
//   if PART==FOOT, also setBlock(neighbour HEAD → AIR, flags 35) + levelEvent 2001
//   survival break already handled by updateShape AIR path above
// Invariant: structures and block entities are immovable (no pistons/structure-block moves),
// so bedPos never drifts and no relocate logic is needed.
```

### 3.3 Bed → Home Binding (LocatedBlockEntity)

```java
// LocatedBlockEntity.tick() — server, once (located flag)
if (!located) {
  located = true;
  var home = IHomeHolder.find(sl, getBlockPos());
  var bed  = BedData.of(getBlockState().getBlock()); // DataMap lookup
  if (home != null && bed != null && home.supportEntity(bed.type()))
      key = home.key(); // StructureKey(structure, dim, locatorPos or custom anchor)
}

// IHomeHolder.find(sl, pos):
//  1) custom: scan 3×3 chunks (offsets ±16), att = chunk.getData(GLMeta.STRUCTURE.get())
//       for (custom entry : att.custom) if entry.value.getTotalBound().isInside(pos) return CustomHomeHolder
//  2) preset: sl.structureManager().getAllStructuresAt(pos)
//       for each holder: start = getStructureWithPieceAt(pos, e)
//       if !valid || empty pieces → continue; root = pieces.getFirst().getLocatorPosition()
//       return StructureHomeHolder.of(sl, new StructureKey(holder.key(), dim, root))
```

`setLink(key)` via `StructureWand` directly assigns `LocatedBlockEntity.key` and `sync()` for custom homes. The key is write-once: BEs/structures are immovable, so no `relocate()` path is required (see §3.2 paired destruction).

### 3.4 Per-Tick Block Logic (YoukaiBedBlockEntity.tick)

```java
// Only HEAD part, server:
super.tick(); // ensures key located
if (key != null && state.get(PART)==HEAD) {
  var data = BedData.of(block); // EntityType tied to this bed item
  if (data != null) {
    var cfg = CharacterConfig.of(data.type()); // discard/respawn/wander
    if (cfg != null && key.support(cfg)) {     // CUSTOM always true; preset checks structure id
      IndexStorage.get(sl).getOrCreate(key)    // LinkedHashMap computeIfAbsent → StructureRefData
         .data().blockTick(sl, data, this, key); // per-entity-type typed BedRefData
    }
  }
  var home = IHomeHolder.of(sl, key);
  if (home != null) home.tick(); // drives StructureHomeData cache/verifier
}
```

### 3.5 Entity Lifecycle via BedRefData.blockTick

```java
// BedRefData.java:49 blockTick(bed, config, sl, be, key)
var home = IHomeHolder.of(sl, key);
if (home==null || !home.isInRoom(be.getBlockPos().above())) {
    sl.removeBlock(be.getBlockPos(), false); return; // bed outside shrunk room → destroy HEAD → updateShape destroys FOOT automatically
}
if (bedPos==null) bedPos = be.getBlockPos();
else if (!bedPos.equals(be.getBlockPos())) {
    // same EntityType cannot have 2 beds per structure (dupe from extra placement, not movement)
    // beds/structures are immovable, so this is only hit when a second bed of the same type is placed;
    // one of the pair is removed; paired-half destruction (updateShape) ensures no orphan remains
    if (sl.isLoaded(bedPos) && sl.getBlockState(bedPos).getBlock()!=be.getBlockState().getBlock()) {
        sl.removeBlock(bedPos,false); bedPos = be.getBlockPos(); // old destroyed (its FOOT auto-removed) → adopt new
    } else { sl.removeBlock(be.getBlockPos(),false); bedPos=null; return; } // new is dupe → destroy new HEAD (+ FOOT)
long time = sl.getGameTime();
if (time >= lastEntityTickedTime + lostEntityTick) lostEntityTick++; else lostEntityTick = time - lastEntityTickedTime;
if (lostEntityTick > config.discardTime()) entityId = NIL_UUID; // considered lost
if (entityId==NIL_UUID && time - lastEntityTickedTime > config.respawnTime()) {
    var e = config.create(bed.type(), sl, bedPos, key);
    // create(): type.create(sl) → HomeModule.setHome(key) → IHomeHolder.of(key).getWanderCenter()
    //           → setPos(center) → restrictTo(center, wanderBase+cfg.wanderRadius()) → initSpellCard()
    if (e != null) { sl.addFreshEntity(e); entityId = e.getUUID(); lastEntityTickedTime=time; }
}
```

### 3.6 Entity → Bed Heartbeat (HomeModule)

```java
// YoukaiEntity.customServerAiStep → modules.tickServer():
// HomeModule.tickServer():
var ref = BedRefData.of(sl, home, self.getType()); // IndexStorage.getOrCreate(home).bedOf(type)
if (ref != null) ref.entityTick(sl, self);
// BedRefData.entityTick:
if (entityId.equals(self.getUUID())) lastEntityTickedTime = sl.getGameTime();
else self.discard(); // impostor (duplicate UUID) — kill stray

// onKilled (YoukaiEntity.die):
modules.onKilled() → HomeModule.onKilled() → BedRefData.onEntityDie() → entityId = NIL_UUID (respawn timer starts)

// sensor side: YoukaiUpdateHomeSensor (interval 80, requires MemoryModuleType.HOME):
var home = StructureKey.of(entity); // HomeModule::home
var bed  = BedRefData.of(sl, home, entity.getType());
if (bed==null || !key.getDim().equals(level.dimension())) clear HOME else set HOME=GlobalPos(dim, bed.getBedPos())
// AI tasks read HOME (YoukaiSleepTask, YoukaiGoHomeTask …)
```

### 3.7 Persistence

| Data | Storage | Serialisation |
|---|---|---|
| `StructureKey key` (+ `located`) in `LocatedBlockEntity` | BlockEntity NBT, chunk save | `@SerialField StructureKey` via `TagCodec` + `BaseBlockEntity sync()` |
| `CharacterConfig` / `BedData` / `StructureConfig` | DataMaps `data/gensokyolegacy/data_maps/*/*.json` + in-memory `RegistryAccess` | `CodecAdaptor` via `GLMeta.*.reg()` |
| `IndexStorage.structureData` | Overworld `DataStorage` id `gensokyolegacy_reference_index` (`level.dat`) | `@SerialClass` `LinkedHashMap<StructureKey, StructureRefData>` via `TagCodec` |
| `HomeModule.home` | Entity NBT `YoukaiModules/<id>` | `@SerialField StructureKey` via `TagCodec` within `YoukaiEntity.addAdditionalSaveData` |
| Structures worldgen | Vanilla `StructureManager` + `StructureStart`/`StructurePiece` | Vanilla chunk NBT |

## 4. Usage Guide

### Add a new Youkai bed

```java
// 1) Define bed block (GLBlocks.java)
public enum Beds { CIRNO, REIMU, RUMIA, MY_BED }
public static final Map<Beds, Holder<Block>> BEDS = ... // YoukaiBedBlock via Registrate + FlatBedShape

// 2) Via preset structure (preferred, when GLStructureGen is populated):
StructBed myBed = new StructBed(
    GLEntities.MY_YOUKAI,                          // Holder<EntityType>
    CharacterConfig.forStructure(6000, 12000, 12, 30), // discard, respawn, wander, vanish
    bedHolder                                      // Holder<Block> for this bed
);
StructStructure myStruct = new StructStructure(
    GensokyoLegacy.loc("my_shrine"),
    TagKey.create(Registries.BIOME, GensokyoLegacy.loc("has_structure/my_shrine")),
    24, 8, StructureConfig.builder().room(...).house(...).outside(...).primary(...).wouldFix(...),
    List.of(myBed),
    new StructSimpleBuilding(...) // or Jigsaw
);

// 3) Or standalone (current pattern for non-worldgen testing):
// in GLStructureGen.dataMap:
bedReg.add(GLBlocks.BEDS[MY_BED.ordinal()], new BedData(GLEntities.MY_YOUKAI.get()), false);
// also need CharacterConfig entry somewhere via Datamap provider or manual DataMapReg
```

### Query bed-entity state

```java
ServerLevel sl = (ServerLevel) level;
StructureKey key = StructureKey.of(youkai).orElse(null);
if (key != null) {
    BedRefData ref = BedRefData.of(sl, key, youkai.getType());
    if (ref != null) {
        BlockPos bedPos = ref.getBedPos();
        boolean hasEntity = !ref.entityId.equals(Util.NIL_UUID) && sl.getEntity(ref.entityId) instanceof YoukaiEntity;
    }
    // via BedRefData.of(sl, youkai) optional variant
}

// from bed block entity:
YoukaiBedBlockEntity be = (YoukaiBedBlockEntity) level.getBlockEntity(pos);
boolean bound = be.linked(); // key != null
StructureKey k = be.key; // @SerialField
```

### Spawn manually (bypass bed timer)

```java
CharacterConfig cfg = CharacterConfig.of(GLEntities.MY_YOUKAI.get());
YoukaiEntity e = cfg.create(type, sl, bedPos, key); // sets HomeModule + restriction
if (e != null) sl.addFreshEntity(e); // BedRefData will adopt on next blockTick if entityId still NIL
// Prefer letting BedRefData handle it — manually set BedRefData.entityId if needed via NBT edit
```

### Debug

```java
// shift-right-click with StructureWand / debug stick routes to:
be.debugClick(player); // clears entityId, resets lastEntityTickedTime = now - respawnTime, sends MSG$RESET
BlockInfoToClient pkt = be.getDebugPacket(player); // respawn countdown or present pos
// Requires: key.support(config) && config.structure.equals(key.structure.location()) (preset) or CUSTOM
```

## 5. Invariants and Caveats

- **Immovable structures/BEs.** `Structure` worldgen pieces and `LocatedBlockEntity` are never moved by pistons/structure-blocks. `LocatedBlockEntity.tick()` therefore binds `key` once (`located` flag) and that is sufficient — no `relocate()` or move-tracking is needed or correct to add. Structure deletion means chunk unload/regeneration, not translation.
- **Paired destruction.** Destroying either half of a `YoukaiBedBlock` always removes the other half: survival via `YoukaiBedMethods.updateShape` (neighbour mismatch → `AIR`), creative FOOT via `YoukaiBedBlock.playerWillDestroy` (explicit `setBlock(HEAD, AIR)`). As a result a bed never leaves an orphan half; `BedRefData` dupe logic only handles a *second distinct bed of the same `EntityType`* in the same structure, not a dangling half. `YoukaiBedBlockEntity.tick()` running on `HEAD` only is therefore sufficient — there is no "destroy FOOT alone leaving stale HEAD" case.
- `GLStructureGen.initStructures()` currently returns `List.of()` — all `StructStructure`/`StructBuilding`/`GLSinglePiece` code is dead outside 3 hard-coded beds. Populating that list is the required step to create worldgen structures; otherwise DataMaps `entity_type/character_config.json` and `worldgen/structure/structure_config.json` remain empty.
- `StructureKey.support(config)` is `custom || !config.structure.equals(structure)`. For preset structures the `equals` check is inverted vs. intuitive `equals` (see `YoukaiBedBlockEntity.getDebugPacket` which correctly uses `equals` — inconsistent). Current runtime tick uses `support` so preset bed binding currently uses the negated comparison.


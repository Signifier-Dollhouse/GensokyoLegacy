# Interior Framework Rework — multi-resident homes, scattered buildings, private rooms

Design for the room-graph rework. Three requirements, with the decisions taken during planning:

| # | Requirement | Decision |
|---|---|---|
| 1 | Multiple characters in the same structure | Residents are **distinct entity types**; the type-keyed bed index stays. The real gap is room-blind destination sampling, not bed storage. |
| 2 | Pathing across disconnected interior rooms (scattered building) | The outdoors becomes a routing medium: composite routes (indoor legs + outdoor transfer) are **always cost-compared** against pure-indoor routes, not used as a last-resort fallback. |
| 3 | Rooms private to a character | **Authored** `owner` field on `InteriorRoom` (datamap level, keyed by `EntityType`). Enforced at destination selection only; pass-by via room routing stays allowed. |

Related: [home_index.md](home_index.md), [structure_bed_entity.md](structure_bed_entity.md), [character_visit.md](character_visit.md).

## 1. Prior shape

- `StructureInterior(rooms, nodes)` (`content/attachment/home/datamap/`), authored in `StructureConfigBuilder`, emitted to `structure_config.json`. `InteriorRoom(name, bound)`; `InteriorNode(posA, posB, roomA, roomB)` with shared (`posA==posB`) / dual-stair (`posA!=posB`) / entry (`roomB==-1`) semantics.
- `findRoute(from, to)` — BFS over non-entry edges, pure indoor. An unwired room returns `List.of()`, and `YoukaiMoveTask.stageViaInterior` then **silently falls back to a straight-line path**.
- Destination sampling: `IHomeHolder.getRandomPosInRoom` → `HomeSearchUtil.getRandomPos(data.getRoomBounds(config), e, pred)` samples uniformly over the **union of all room boxes** (`MultiStructureBound` prefix-sum weighting). No room is ever preferred, avoided, or owned.
- Binding: `StructureRefData.entities` is `Map<EntityType, BedRefData>` — one bed slot per entity type per structure; a second bed of the same type is destroyed (`BedRefData.blockTick`). "Its room" is only the vanilla `HOME` memory pointing at the bed.

## 2. Rework A — room-aware destination selection

The type-keyed bed index already holds one slot per resident type, and datagen (`List<StructBed>`) already supports several beds per structure. What breaks with two residents is that wander destinations ignore rooms entirely — sampling a private room is indistinguishable from sampling a shared one.

### 2.1 Unified room view

`StructureHomeData` gains a room accessor that returns world-mapped rooms with ownership:

```java
// world-mapped view; interior rooms when present, else legacy config.rooms() as anonymous rooms
public List<InteriorRoom> getRooms(StructureConfig config)
```

Legacy boxes (`hakurei_shrine`, `morichika_shop`) become anonymous public rooms — no owner, no routing. Custom (wand) homes have no interior and are out of scope for ownership.

### 2.2 Filtered sampling

`StructureHomeHolder.getRandomPosInRoom` builds the `MultiStructureBound` over **allowed rooms only** instead of the raw union:

```java
allowed = rooms where owner == null || owner == e.getType()
if (allowed non-empty) bound = MultiStructureBound.of(allowed boxes)   // volume weighting comes free
else bound = getRoomBounds(config)                                     // degenerate: all rooms private to others
```

Volume-weighted per-room distribution is inherited from the prefix-sum sampler; no new sampling code. The stability predicate (`isStableDestination` / `isOutside`) is unchanged.

### 2.3 Effects

- Two residents of different types distribute over rooms; each avoids the other's private room as a wander destination.
- `getRandomPosInBound` (stay-near-house wandering, visitor spawn) rejects candidates that land inside a room private to someone else, so privacy holds outside the room graph too.
- Visitor clones (guest types from `StructureConfig.visitors`) go through the same paths, so a guest never picks a private room either — visitors are never owners.
- No change to `BedRefData` / `StructureRefData` / `HomeModule`.

## 3. Rework B — routing across disconnected rooms

Node semantics are unchanged; the routing layer learns that the outdoors is walkable.

### 3.1 Composite route model

State = room index. Transitions between rooms:

- **interior edge** — an `InteriorNode`, cost `1`, waypoints `posA` (plus `posB` for dual edges);
- **outdoor transfer** — leave via any entry node `e1` and re-enter via any entry node `e2` (entry nodes are both exits and entrances), cost `TRANSFER_BASE + dist(posA(e1), posA(e2)) / TRANSFER_WEIGHT`, waypoints `[posA(e1), posA(e2)]`.

`TRANSFER_BASE` (~3 indoor legs) keeps a doorway cheaper than a short step outside when both exist, and `TRANSFER_WEIGHT` (~16 blocks per leg) prices the walk itself; together they decide when walking outside beats taking indoor legs. Both live in `StructureInterior` and are tuned here, not at call sites.

`StructureInterior` gains the unified router:

```java
/** Full route as an ordered waypoint list, outdoor transfers included; empty when unreachable. */
public List<BlockPos> route(int from, int to)
```

Dijkstra over rooms (double weights; ≤ a few dozen rooms and `nodes + exits²` transfer edges, negligible). `findRoute` stays pure-indoor and is kept for the `checkconfig` parity / debug metric.

### 3.2 Consumer changes

`YoukaiMoveTask.stageViaInterior` keeps its three cases but delegates waypoint assembly:

- indoor → indoor: `route(from, to)`;
- outside → in: nearest entry (unchanged), then `route(entry.roomA(), to)`;
- inside → out: `bestExit` — its cost metric switches from pure `findRoute` legs to `route` legs, so a scattered building prefers the exit of its own component.

`appendRoute` moves into `StructureInterior` as the waypoint emitter behind `route` (it is graph logic, not task logic). The straight-line fallback now fires only when even the composite router finds nothing — i.e. no entry reachable from the current room, which `checkconfig` guarantees cannot happen in valid data.

Because `firstUnreached` walks the waypoint list through ordinary navigation, an outdoor transfer is just two more waypoints: exit the door, vanilla-path over open ground, re-enter. No new path machinery.

### 3.3 Scattered building authoring

`house` is already a box list, so a multi-building structure needs no schema change: author one `InteriorRoom` per interior space across all buildings, wire each component with its own nodes, and give **each component at least one entry node** (that is what makes cross-building transfers possible). Components without an entry are unreachable and rejected by `checkconfig`.

Limitation: the room graph is authored against the **root piece only** (`StructureHomeData.checkInit` maps the first piece), so a scattered structure is currently one template with disconnected interior volumes; rooms living in separate jigsaw parts need a per-piece room schema, which is a follow-up.

### 3.4 checkconfig

- The mutual-reachability requirement changes from "indoor graph connected" to "every room reachable, outdoor transfers allowed". Concrete rule: every room must have ≥1 incident node (interior or entry), and every connected component of the indoor graph must contain at least one entry node.
- `FORBIDDEN_EDGES` (alice: `tower_hall↔corridor_up`, `corridor_gnd↔corridor_up`) stays enforced — transfers must not smuggle in the balcony-onto-the-drop path.
- Nothing about outdoor terrain is statically checkable; transfer plausibility is a runtime property (the straight-line fallback covers a hostile gap).

## 4. Rework C — private rooms

### 4.1 Schema

`InteriorRoom` gains an optional owner:

```java
public record InteriorRoom(String name, BoundingBox bound, @Nullable EntityType<?> owner) {}
```

JSON (backward compatible — owner omitted for public rooms):

```jsonc
{"bound": [12, 5, 7, 26, 8, 14], "name": "wing_upper", "owner": "gensokyolegacy:alice_margatroid"}
```

Authored in `StructureConfigBuilder` through the `room(name, box, owner)` helper; owner must be one of the structure's `entities` — checked in `checkconfig`.

### 4.2 Enforcement — destination selection only

The filter from §2.2 is the entire mechanism:

- **Forbidden**: another character (or a visitor) picking a private room as a wander/destination target via `getRandomPosInRoom`.
- **Allowed**: routing *through* a private room — `stageViaInterior` / `route()` have no owner knowledge, pass-by follows the node graph exactly as the requirement specifies.
- The owner's own tasks are unaffected: `HOME` is the bed position, and the bed sits in the owner's room anyway.

Deliberately not modelled: runtime occupancy (another resident standing in a room). Soft-avoidance of occupied rooms is a possible future layer on top of §2.2, not part of this rework.

### 4.3 Bed cross-check

The owner's bed should sit inside the owner's private room. `checkconfig` derives the bed's room from the template (`roomIndexOf` equivalent over the authored boxes) and fails when a bed of the owner's type lies outside the owned room. A private room without a bed (workroom) stays valid.

## 5. Invariants

- One bed slot per `EntityType` per structure stays; the dupe-bed destruction rule in `BedRefData.blockTick` is unchanged (same-type residents remain out of scope by decision).
- `InteriorNode` semantics (shared / dual / entry) are unchanged; `findRoute` stays pure-indoor.
- Custom homes: no interior graph, no ownership; `RoomData` boxes keep working as today.
- Fallback order: unified route → nearest-entry staging → straight line (last is now a broken-data case, not a feature).
- `checkconfig` remains the sole graph verifier; `SKIP_GRAPH` for legacy structures persists until they migrate to `interior` graphs (optional follow-up, not required).

## 6. Implementation order

1. `InteriorRoom.owner` + datagen builder + `checkconfig` owner/bed rules; `runData` and commit the regenerated `structure_config.json`.
2. Room-aware destination sampling (§2) — independent of routing, ships value immediately.
3. Composite router (§3): `TRANSFER_WEIGHT`, `route()` in `StructureInterior`, `appendRoute` relocation, `stageViaInterior`/`bestExit` delegation, `checkconfig` connectivity relaxation.
4. Optional: migrate `hakurei_shrine` / `morichika_shop` legacy `rooms` into `interior` graphs; then a scattered-building structure to exercise §3.3 end to end.

Code changes carry `@see "doc/design/structure/interior_rework.md"` on `StructureInterior`, `StructureHomeHolder.getRandomPosInRoom`, and the §3 consumer touchpoints.

## 7. Status

Steps 1-3 are implemented (private-room schema, owner-filtered destination sampling, composite router). Step 4 (legacy `rooms` migration, a real scattered structure) remains.

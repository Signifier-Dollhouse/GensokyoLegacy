# Structure Interior — room graph, private rooms and routing

A structure's interior is a graph of named boxes (`InteriorRoom`) joined by waypoints (`InteriorNode`). The graph does
three jobs: it says which rooms an entity may wander into, it carries a walker between rooms that have no direct door,
and it lets a walled building connect to a scattered neighbour by stepping outside.

Related: [home_index.md](home_index.md), [structure_bed_entity.md](structure_bed_entity.md),
[character_visit.md](character_visit.md), [interior_wand.md](interior_wand.md).

## 1. Data

`StructureInterior(rooms, nodes)` lives in `content/attachment/home/datamap/` and stores template-local coordinates.
It is authored in `StructureConfigBuilder` and emitted to `structure_config.json`.

```java
public record InteriorRoom(String name, BoundingBox bound, @Nullable EntityType<?> owner) {}

public record InteriorNode(BlockPos posA, BlockPos posB, int roomA, int roomB) {}
```

`owner` omitted means a public room. `InteriorNode` has three forms:

| form | test | meaning |
|---|---|---|
| shared | `posA == posB` | one doorway on the boundary of two rooms |
| dual / stair | `posA != posB` | the two ends of a staircase or a thick wall |
| entry | `roomB == -1` | a way in and out; `posA` is the doorway |

A structure with no interior data (`StructureInterior.empty()`) is a legacy home: `config.rooms()` becomes anonymous
public rooms and there is no routing graph. Custom homes built with the structure wand have `RoomData` boxes but no
interior graph and no ownership.

`StructureHomeData.getRooms(config)` maps the authored rooms into world coordinates and keeps their owners;
`roomIndexOf(pos)` returns the room containing a world position, or `-1`.

## 2. Destination selection

Wander and visitor destinations are sampled from **allowed rooms only**:

```java
allowed = rooms where owner == null || owner == e.getType()
bound = MultiStructureBound.of(allowed boxes)   // allowed non-empty
      = getRoomBounds(config)                    // degenerate: all rooms private to others
```

`StructureHomeHolder.roomBoundsFor` builds that bound; `getRandomPosInRoom` then samples it exactly as before, so the
volume-weighted prefix-sum distribution comes for free and the stability predicate (`isStableDestination` /
`isOutside`) is unchanged.

`getRandomPosInBound` (stay-near-house wandering and visitor spawns) rejects candidates inside a room private to
someone else. A guest type from `StructureConfig.visitors` goes through the same paths and is never an owner.

Ownership is enforced **at destination selection only**. Routing has no owner knowledge, so a character may pass
through another character's private room when the node graph makes that the shortest way.

## 3. Routing

`StructureInterior.route(from, to)` returns the ordered waypoints from room `from` to room `to`, or an empty list when
they are already together or unreachable. It is a Dijkstra over rooms:

- **interior edge** — cost `1`, waypoints `posA` (plus `posB` for a dual edge);
- **outdoor transfer** — leave through entry node `e1` and re-enter through entry node `e2`, cost
  `TRANSFER_BASE + dist(posA(e1), posA(e2)) / TRANSFER_WEIGHT`, waypoints `[posA(e1), posA(e2)]`.

`TRANSFER_BASE = 3` and `TRANSFER_WEIGHT = 16` live on `StructureInterior`. The base keeps a real indoor doorway
cheaper than a short step outside when both exist; the weight prices the walk itself. Together they make a composite
route compete with a pure-indoor route instead of being a last-resort fallback.

`YoukaiMoveTask.stageViaInterior` is the only consumer. It has four cases:

| from | to | behaviour |
|---|---|---|
| indoors | indoors | `route(from, to)` |
| outside | indoors | walk to `nearestEntry`, then `route(entry.roomA(), to)` |
| indoors | outside | `route(from, exit.roomA())`, then leave through `bestExit` |
| outside | outside | straight line; the graph is not consulted |

`bestExit` picks the entry with the fewest route waypoints, breaking ties by distance to the target, so a scattered
building leaves through its own component instead of walking back through the main house.

`firstUnreached` turns the waypoint list into the next navigation target: an outdoor transfer is simply two more
waypoints, and vanilla navigation walks the open ground between them. No extra path machinery is involved.

## 4. Private rooms

A room is private when `owner` is set. The authoring side is one helper:

```java
room("wing_upper", new BoundingBox(12, 5, 7, 26, 8, 14), GLEntities.ALICE.get())
```

JSON keeps the field optional, so older data stays valid:

```jsonc
{"bound": [12, 5, 7, 26, 8, 14], "name": "wing_upper", "owner": "gensokyolegacy:alice_margatroid"}
```

`owner` must be one of the structure's `entities`. `checkconfig.py` enforces three things:

- an owner is a resident of that structure;
- the owner's bed lies inside her private room (a workroom without a bed is fine);
- every room has at least one incident node, and every connected component of the indoor graph has at least one entry.

The last rule is what makes outdoor transfers safe: a component with no entry can never be entered or left, so the
composite router would otherwise have nowhere to send the walker. `FORBIDDEN_EDGES` (Alice: `tower_hall` ↔
`corridor_up`, `corridor_gnd` ↔ `corridor_up`) stays enforced, so a transfer cannot recreate the balcony-onto-the-drop
path that the indoor graph forbids.

## 5. Scattered buildings

`house` is already a list of boxes, so a multi-building structure needs no schema change: author one `InteriorRoom` per
interior volume across all buildings, wire each component with its own nodes, and give **each component at least one
entry node**. The entry node is the only thing the outdoor transfer needs.

The room graph is authored against the **root piece** only (`StructureHomeData.checkInit` maps the first piece). A
scattered structure is therefore one template with disconnected interior volumes; rooms living in separate jigsaw
parts would need a per-piece room schema, which is a follow-up.

Outdoor terrain cannot be checked statically. A transfer is plausible only at runtime; if the walking line is blocked,
vanilla navigation handles it like any other outdoor target.

## 6. Invariants

- One bed slot per `EntityType` per structure; the same-type duplicate-bed destruction in `BedRefData.blockTick` is
  unchanged.
- `InteriorNode` semantics (shared / dual / entry) are fixed; entry nodes are both exits and entrances.
- Custom homes have no interior graph and no ownership; `RoomData` boxes keep working as before.
- Fallback order: composite route → nearest-entry staging → straight line. The last one is a broken-data case, not a
  feature.
- `checkconfig.py` is the sole graph verifier. Structures with legacy `rooms` may still set `SKIP_GRAPH` until they
  migrate to an `interior` graph.

## 7. Files

| file | role |
|---|---|
| `content/attachment/home/datamap/StructureInterior.java` | rooms, nodes, `route`, transfer costs |
| `content/attachment/home/datamap/InteriorRoom.java` | room box and optional owner |
| `content/attachment/home/datamap/InteriorNode.java` | shared / dual / entry waypoint |
| `content/attachment/home/structure/StructureHomeData.java` | world-mapped rooms, bounds, `roomIndexOf` |
| `content/attachment/home/structure/StructureHomeHolder.java` | allowed-room sampling |
| `content/entity/behavior/task/core/YoukaiMoveTask.java` | `stageViaInterior`, `nearestEntry`, `bestExit` |
| `init/data/structure/StructureConfigBuilder.java` | authored interiors and owners |
| `tools/checkconfig.py` | owner, bed, reachability and node validation |

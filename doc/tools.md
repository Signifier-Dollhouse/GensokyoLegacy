# Structure bounds tools

`StructureConfigBuilder` authors three kinds of template-local box per structure:
the **house** box (the integrity snapshot raster), the **interior** room boxes
(youkai wander / staged-pathfinding space) and the **nodes** that connect them.
They are hand-written `BoundingBox` literals, which means **editing a structure
template silently invalidates them** — nothing at compile time or datagen notices
that a wall moved out from under a room box.

The tools in `tools/` let you re-derive and re-check those boxes against the
shipped `.nbt` without launching the game.

## Why the boxes cannot just be computed

The obvious approach — flood-fill the air, call each pocket a room — does not
work, and it is worth being explicit about why, because it wastes time to
rediscover:

- **Furniture plugs the volume.** A naive air flood fill fragments every room
  around sofas and shelves. `indoor.py` reduces each cell to blocking /
  passable / open-air first, which survives that, but...
- **The building is open to the outdoors.** A house has doors. `integrity` aside,
  the indoor air connects to the yard through every doorway, so "reachable from
  outside" cannot define "indoors" either. Alice's shell is sealed, but hers has
  an unglazed light slot running the full height at `x10–11`, which is outdoor
  air *inside* the building's footprint.
- **Rooms are not connected components.** They are separated by doors and
  half-height dividers, which is exactly what a flood fill treats as solid. So
  the sealed pockets it finds correspond to rooms only when you deliberately
  treat doors as walls.

The boxes therefore stay hand-authored. The tools exist to **check** them, not to
produce them.

## The tools

| tool | what it does |
|---|---|
| `nbt.py` | minimal gzipped-NBT reader with strict validation; prints a byte ruler on malformed input |
| `structview.py` | palette counts, per-layer block maps, enclosed-air pockets |
| `indoor.py` | passability flood fill; classifies every cell and reports the sealed indoor components |
| `void.py` | renders only the sealed indoor void per layer, so room volumes are readable without furniture noise |
| `checkinterior.py` | validates a *proposed* room/node set (the values are duplicated in the file header) |
| `checkconfig.py` | validates the **generated datamap JSON** against the shipped template — this is the one that gates |

### Reading a template

```sh
python3 tools/nbt.py src/main/resources/data/gensokyolegacy/structure/alice_house/root.nbt
python3 tools/structview.py <template.nbt>          # summary + palette histogram
python3 tools/structview.py <template.nbt> air      # enclosed air pockets
python3 tools/structview.py <template.nbt> layers    # every y, x across / z down
python3 tools/structview.py <template.nbt> plan 6    # one y layer, '#' vs air
python3 tools/indoor.py <template.nbt> --quiet      # sealed indoor components
python3 tools/indoor.py <template.nbt>              # ...with per-layer render
python3 tools/void.py <template.nbt>                # indoor void only
```

### Checking bounds

`checkconfig.py` is the authority, because it reads the boxes out of
`src/generated/resources/data/gensokyolegacy/data_maps/worldgen/structure/structure_config.json`
rather than a second copy, so what it validates cannot drift from what ships:

```sh
./gradlew runData
python3 tools/checkconfig.py alice_house src/main/resources/data/gensokyolegacy/structure/alice_house/root.nbt
python3 tools/checkconfig.py marisa_house src/main/resources/data/gensokyolegacy/structure/marisa_house.nbt
python3 tools/checkconfig.py hakurei_shrine src/main/resources/data/gensokyolegacy/structure/hakurei_shrine/root.nbt
```

It enforces, per structure:

- **no outdoor air inside a room box.** A room must not contain a cell that is
  open air reachable from outside the template; that cell is a hole in the wall
  or a mis-sized box. This is the single most useful check — it is what catches
  a room box that swallowed a light well.
- **rooms pairwise disjoint.** `StructureInterior.roomIndexOf` returns the first
  match, so an overlap silently reassigns part of a room.
- **every room is wired into the graph.** `findRoute` walks declared nodes only,
  so a room with no node is unreachable however well its box fits, and a
  youkai standing in it can never path anywhere. This is the check that
  catches a room added for a space nobody then gave a node.
- **forbidden edges stay unwired.** `FORBIDDEN_EDGES` names room pairs that
  touch and must never gain a node — Alice's balcony against the tower hall.
- **nodes whose rooms share no face** are reported as a note, since a doorway
  between two non-adjacent boxes still routes, just through solid structure.
- **every room inside the house box.** The house box is the integrity raster;
  a room cell outside it makes `IntegrityVerifier` skip that cell entirely.
- **node cells are open air with solid support**, inside the room they are
  declared for. A doorway node is a *waypoint the entity walks to*, so a node on
  a door leaf or a wall aims the path at a solid block. A shared node's position
  must sit in `roomA` (see `InteriorNode`); a dual node needs one cell in each room.
- **the bed sits in a room.** `BedRefData` deletes a bed whose cell above it
  falls outside every room, so a bed near a room edge silently disappears.

`void.py` is also the fastest way to answer "what did I miss": render the
indoor void, then diff it against the room boxes. Clusters of indoor air that
no box covers are the gaps. Some are intentional (Alice's wing loft and tower
roof interior are deliberately excluded because they are not standing space),
so this is a review step, not a gate.
- **the stair pair is wired correctly**: the dual node's `posA` is exactly one
  block from the lowest step, `posB` one block from the top, both clear of the run
  itself, and the two room boxes share a face. The run is found by chaining
  bottom-half stairs that step one along x and one up in y; it skips the check
  when it cannot find an unambiguous run.

`checkinterior.py` is the same battery against a candidate set kept in its own
header, for iterating before you touch the Java. It adds two design invariants
beyond the datamap-level ones:

- `MUST_TOUCH` — room pairs required to share a face because a node declares
  them adjacent (Alice's stair pair).
- `FORBIDDEN_EDGES` — room pairs that touch and must *never* be joined by a node.
  Alice's balcony is the case worth knowing about: `corridor_up` is wired to
  the wing so it is usable, and left unwired from `tower_hall` even though the
  two boxes share a face, so the youkai cannot be routed out onto the ledge
  over the drop. Its railing is what makes standing there safe; the missing
  node is what keeps it from being a route. `corridor_gnd` and `corridor_up`
  are listed too — they meet at `y4/y5`, which is a floor, not a way up.

A note on space that is deliberately uncovered, since it looks like a bug and
is not: Alice leaves out the wing loft and the tower roof interior. Neither is
standing space. The general rule the checks encode is that every room needs a
node, so if a space is genuinely unreachable it should not be a room at all.

## Workflow after editing a template

1. `./gradlew runData` if the template size changed (it feeds
   `StructureCache`'s raster size check).
2. Re-derive the geometry: `indoor.py --quiet` for the component list, `void.py`
   for the room shapes, `structview.py plan <y>` when you need block names.
3. Draft the boxes, iterate with `checkinterior.py`.
4. Port them into `StructureConfigBuilder`, **and update the comment block above
   each builder** — the prose there is the only record of why the boxes have the
   shape they do, and it is what makes the next edit tractable.
5. `./gradlew runData` then `checkconfig.py`.
6. Run `checkconfig.py` over the *other* structures too. It is structure-agnostic
   and has already caught real bugs in configs nobody was editing.

## Gotchas

- **Zero-length NBT lists carry a 4-byte length even when the element type is
  `TAG_END`.** A parser that short-circuits on `type == 0` without consuming the
  length desyncs by 4 bytes per empty list and then reports plausible-looking
  garbage. `nbt.py` handles it; it is the first thing to check if a template
  parses with a plausible-but-wrong block count.
- **These templates are dense**: they store `air` explicitly, so `blocks` equals
  the full volume rather than the non-air count. Any tool that assumes vanilla's
  "skip air" will read them wrong.
- **Classify glass, panes, leaves, bars and doors as blocking**, not passable, for
  connectivity. A closed door is a wall for room-finding purposes, and treating
  it as a passage merges the interior with the outdoors. `indoor.py` documents
  this in `state_of`.
- **`half=top` stairs are decoration.** Alice's tower shelves and roof are built
  from them; only the `half=bottom` diagonal chain is a walkable run.

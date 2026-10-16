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
| `stripjarjar.py` | strips the jar-in-jar payload from a `libs/` mod jar — unrelated to structures, listed here because it lives in `tools/` |
| `terrain.py` | exports a biome map and a height map of a dimension from its worldgen json — also unrelated to structures, same reason |
| `cave_bands.py` | plots the biome bands the climate grid cuts, by continentalness and height, and derives their depth thresholds |

### Terrain maps

`terrain.py` renders two previews of a dimension at one pixel per chunk, 512x512
chunks (4096x4096 blocks) by default:

```sh
python3 tools/terrain.py gensokyolegacy:gensokyo
python3 tools/terrain.py gensokyolegacy:gensokyo --seed 9094732767959603503 --size 1024
```

out/terrain/<dimension>_biomes.png
:   every biome in the color Minecraft's own map gives it, from the biome map palette
out/terrain/<dimension>_height.png
:   blue below sea level, then one color per 16 blocks of height above it
out/terrain/<dimension>.txt
:   biome colors, map shares, and a height histogram

The dimension json names a biome source and a noise settings entry, and the noise
settings entry carries the whole noise router, so the tool reads the terrain the same
way the game does rather than mirroring `NoiseRouterData`. Vanilla density functions
and noise parameters come from the NeoForge client-extra jar (found automatically, or
`--vanilla`); the mod's own come from `src/generated/resources`.

The height map is the **terrain** surface, the topmost block whose density is positive,
so under sea level it shows the sea floor and not the water surface — what the game
stores in `WORLD_SURFACE` is the water, a few blocks higher. Caves and the
`base_3d_noise` "cheese" term are dropped for speed; `--caves` and `--cheese` keep
them. Neither is worth the cost at one pixel per chunk, and the surface still lands
within a few blocks of the generated world.

Run it after changing the offset spline, the biome grid, or anything else that moves
the terrain. The peak reaching 220, 14% of the map under water and a quarter of it
above y 80 are the numbers `doc/design/gensokyo_dimension.md` claims, and the map
shows them at a glance.

### Cave report

At one pixel per chunk a cave is at most a pixel of noise, so `--voids` walks the world
vertically instead: 676 columns 24 blocks apart, every 4 blocks of y, evaluating the
router's full `final_density` against the cave-free surface as the datum.

```sh
python3 tools/terrain.py gensokyolegacy:gensokyo --voids --seed -1838080655014084215 --cheese
```

out/terrain/<dimension>_caves.txt
:   carved share of the rock under the surface by band, the share of columns that are
    open at the surface, the share with a void 40 blocks deep, and the depth of the
    roof over the shallowest cave

It is the source of the numbers in `doc/design/gensokyo_dimension.md` §8. Pass
`--cheese`, since dropping `base_3d_noise` moves the surface the report measures from.

### Biome band graph

`cave_bands.py` plots the bands the climate grid cuts, continentalness across and height up,
and derives the bounds they come from. The grid picks a biome per 4x4x4 cell by nearest-cell
lookup over seven climate axes, and two of them do the underground classification: `depth` and
`continentalness`. Depth is 3d and reads about 0 at a column's own surface, falling one unit per
128 blocks; continentalness fixes the height band. So the grid draws a curve in that plane, and
where that curve lands in y is a property of the offset spline rather than of the numbers written
in the source. This measures it instead of assuming it.

```sh
python3 tools/cave_bands.py gensokyolegacy:gensokyo --seed -1838080655014084215 --origin=-384,-384
```

out/terrain/<dimension>_cave_bands.png
:   one column per 0.02 continentalness bucket, coloured into the three families, with the terrain
    surface and both cuts drawn over it

out/terrain/<dimension>_cave_bands.txt
:   the table to quote in the design doc: the bounds, the share of each family that a cell claims,
    the cave band thickness, and where the underground reaches

**Solid fill is a cell that claims the column; pale fill lost on the quadratic falloff.**
`Climate.ParameterPoint.fitness` is the sum of the squared distance outside each axis span, so a
query lands in a *box* around a climate cell and the boundaries are hyperbolae. Inside its own box
a cell wins outright; between boxes the nearest one wins on the quadratic. The tint is therefore
not slack in the numbers — it is where the bounds are genuinely being interpolated rather than
claimed, and the percentage of each family that is claimed is printed in the legend. Two
consequences worth knowing before reading a graph: the boundary between two spans sits at the
**midpoint** of their bounds, not at either bound, so a pair of bounds can be positioned
independently of the line they draw; and past `C_HI` the underground is not claimed at all and has
to earn the region back with a squared continentalness penalty, which is what lets the cave take
the deep rock at high continentalness.

`--bounds` is the default and gives one cave climate region: `D_S` (surface depth upper),
`D_C0` / `D_C1` (cave depth lower / upper), `D_U` (underground depth lower), `C_LO` (cave
continentalness lower) and `C_HI` (underground continentalness upper). Note `D_S + D_U` fixes the
lowland cut, since that cut is their midpoint — keeping the lowlands where they are means the two
must sum to 1.0.

`--slices N` instead derives N cave regions from the measured curve, each thresholded at the
largest depth at the cap inside it. Equal depth spans minimise how far the cap can stray, and the
floor over N slices is the total depth range divided by N — no choice of edges beats it.

The tool **exits non-zero if any column's underground reaches above `--cap-y`**, so it can gate the
change it describes.

### Stripping jarJar from a dev jar

The mod jars under `libs/` are real published builds, so `youkaisfeasts` carries
`META-INF/jarjar/` — `metadata.json` plus a copy of Registrate, `l2core`,
`l2serial`, `l2damagetracker`, `l2modularblocks`, `l2harvester` and TerraBlender.
That duplicates libraries this project already declares as `jarJar(...)`, which is
the JPMS split-package failure the dev launcher cannot work around from any
dependency scope. Strip the payload so the host mod supplies them:

```sh
python3 tools/stripjarjar.py libs/youkaisfeasts-1.1.0.jar --dry-run   # report first
python3 tools/stripjarjar.py libs/youkaisfeasts-1.1.0.jar --backup    # rewrite in place
python3 tools/stripjarjar.py libs/*.jar --dry-run                    # audit every dev jar
```

It rewrites the archive entry by entry (preserving each entry's compression and
the archive comment) rather than mutating in place, since `zipfile` cannot delete
entries, then re-opens the result and checks it: `testzip()`, no surviving
`META-INF/jarjar/`, and no metadata file still mentioning jarJar. A strip is easy
to get wrong silently — the result is still a valid zip and `mods.toml` keeps its
`[[dependencies]]` blocks either way — so the verification is the point of the
tool, not the deletion. Re-running on a clean jar is a no-op.

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

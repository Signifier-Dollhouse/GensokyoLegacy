# Flat-check terrain fitness — `TerrainPredicate`

How a `FlatCheckStructure` decides that a speculative jigsaw layout fits the terrain under it, and how
Myouren Temple gets a looser rule with a cobblestone foundation instead of the flat-ground rule.

Related: [placement.md](placement.md), [structure_bed_entity.md](structure_bed_entity.md).

## 1. Problem

The old check baked one rule into `FlatCheckStructure#validateLayout`:

- the root footprint's 9 samples may span at most `2 * height_tolerance`;
- every rigid piece's centre must be within `height_tolerance` of its placed ground, **both ways**;
- adjacent rigid groups joined by terrain-matching paths may differ by at most `height_tolerance`.

Symmetrical bounds are right for a house that sits *on* the ground. Myouren Temple is one flat tier of
29 rigid pieces, 89 x 97 blocks across, on barrens that are not flat. The temple needs the **gate** on
the ground, must not be buried where the terrain rises behind it, and must be filled underneath where
the terrain falls away. A symmetric window is wrong on both sides: burial has to be rejected, while
floating is fine as long as the foundation closes the gap.

So the rule becomes a field of the structure instead of code, with two implementations.

## 2. Where it sits

| Class | File | Role |
|---|---|---|
| `TerrainPredicate` | `content/worldgen/structure/terrain/TerrainPredicate.java` | Interface: `Codec<? extends TerrainPredicate> codec()` (1.20.1 has no `MapCodec`), `FitResult fit(TerrainFit)`, a diagnostic `describe(TerrainFit)`, a default no-op `afterPlace(level, chunk, pieces)` for the foundation pass, and a `triesAllRotations` default (false) for rules that want the four start rotations tried. Owns the `Codec<TerrainPredicate>` dispatch (`ResourceLocation` type key, resolved lazily), so datagen needs no registry. |
| `TerrainFit` | `content/worldgen/structure/terrain/TerrainFit.java` | Record `(GenerationContext ctx, PieceTree tree, int seaLevel)` plus `height(x, z)` (`getFirstFreeHeight(WORLD_SURFACE_WG)`) and the 9-point `ring(BoundingBox)` sampler (4 corners, 4 edge centres, centre). |
| `FitResult` | `content/worldgen/structure/terrain/FitResult.java` | Record `(boolean pass, String reason, int delta)`: the vertical shift applied to the layout, or the failure reason. |
| `UniformHeightCheck` | `content/worldgen/structure/terrain/UniformHeightCheck.java` | The old rule, verbatim. `height_tolerance` is now its own field. |
| `BastionHeightCheck` | `content/worldgen/structure/terrain/BastionHeightCheck.java` | Anchor one piece on the terrain, one-sided bound for the rest, and the `afterPlace` fill. Also names the anchor's beard: `BEARD_THIN` for the gate, `NONE` for every other piece. |
| `GLBeardedPiece` | `content/worldgen/structure/GLBeardedPiece.java` | `PoolElementStructurePiece` with a per-piece `PieceBeardifierModifier` adjustment, persisted under its own `StructurePieceType` so it survives a start reload. |

`FlatCheckStructure` replaces its `height_tolerance` field with `TerrainPredicate terrain`:

```json
"terrain": {"type": "gensokyolegacy:uniform_height", "height_tolerance": 5}
```

```json
"terrain": {
  "type": "gensokyolegacy:bastion_height",
  "anchor": "gensokyolegacy:myouren_temple/gate",
  "height_tolerance": 4,
  "fill": {"Name": "minecraft:cobblestone"}
}
```

`fill` is a `BlockState` and defaults to dirt, so a bastion that fills with dirt need not name it; Myouren
names cobblestone. `height_tolerance`
also has a default (4) and is omitted when equal to it. The type key is a `ResourceLocation`; the type table is
resolved in `codecFor`, so a new predicate is one class and one branch, with no registry bootstrap and no
datagen ordering constraint.

## 3. The pipeline

Unchanged up to the layout:

```
findGenerationPoint
  └─ candidate stream (MultiSpreadPlacement) → region pick
       └─ checkCandidate
            ├─ prefilter           middle column: above sea, biome in tag
            ├─ checkMarginSafe     neighbouring regions' candidates
            └─ validateLayout
                 ├─ JigsawPlacement.addPieces at the provisional Y
                 ├─ PieceTree.build
                 ├─ biome check     8 points of the union box
                 └─ terrain.fit(...)   ← everything below moved here
```

`validateLayout` no longer knows heights: it builds the tree, runs the biome gate, then calls
`this.terrain.fit(new TerrainFit(ctx, tree, seaLevel))`. A failing `FitResult` is returned as the
candidate's failure reason; a passing one supplies the applied `delta`, added to the stub position.

The layout is normally built once, with the start rotation drawn from the candidate's own random. A predicate
with `triesAllRotations` set rebuilds it with each of the four rotations in a shuffled order and keeps the
first fit (§4.3).

The stub only serves the jigsaw pieces. The foundation is not a piece: after that chunk's pieces are
placed, `StructureStart.placeInChunk` calls `Structure#afterPlace`, and `FlatCheckStructure` forwards it to
`this.terrain.afterPlace` (§5). The predicate default is a no-op, so only the bastion rule fills anything.

## 4. The two predicates

### 4.1 `UniformHeightCheck(height_tolerance)`

Behaviour of the old code, moved without change:

1. 9-point ring of the **root** footprint: every sample must be above sea, and `max - min <= 2 * t`.
2. `spawn = (min + max) / 2`; the start rigid group is shifted so its ground sits on `spawn`.
3. Every rigid piece's centre: above sea, and `|h - ground| <= t`.
4. Every connected non-rigid subgraph: adjacent rigid grounds differ by at most `t`.

Fills nothing. Used by the houses and the shrine (`t = 5`).

### 4.2 `BastionHeightCheck(anchor, height_tolerance, fill)`

1. Find the piece whose template id is `anchor`, read from `SinglePoolElement#template` (exposed by the
   access transformer; `GLSinglePiece` is datagen-only and decodes as plain `SinglePoolElement`, so
   matching on that subclass never fires). It must be rigid and in the start piece's rigid group,
   otherwise the candidate fails (`anchor-missing`, `anchor-group`) — shifting a detached group would tear
   the assembly apart.
2. 9-point ring of the **anchor**, not of the root footprint: above sea, `spawn = (min + max) / 2`, shift the
   anchor's rigid group by `spawn - ground(anchor)`. `delta` is that shift.
3. The anchor keeps the **symmetric** rule: every ring sample `|h - spawn| <= t`. Since `spawn` is the ring
   midpoint, that is exactly `max - min <= 2 * t` (8 for the default `t = 4`). This is the "matches the
   gate to the terrain" half, and it also rejects a gate straddling a cliff.
4. Every **other** rigid piece's centre must satisfy `h < ground + t` (above sea as well): terrain may
   not rise into the piece by more than the sampling tolerance. There is no lower bound: a piece may
   float any distance above the terrain, and the fill in §5 closes the gap.
5. No joint rule. Each rigid group is checked against the terrain on its own; the fill in §5 closes the
   gap below the anchored group.

Vanilla makes this simpler than it looks: a rigid child's `ground_level_delta` is set to
`parent delta - junction offset`, so a rigid-rigid group shares one `groundOf` (verified: Myouren's 29
pieces all report ground 1). The one-sided rule is therefore one number per candidate —
`h < spawn + t` — and the fill target is the same plane for every piece.

`PieceTree` grows `shiftRigidGroupToGround(int piece, int y)`; `shiftStartGroupToGround` becomes a
delegate to it with piece 0.

The rule is one-sided on purpose. The gate anchors the assembly to the terrain; the rest may only rise
out of it. Floating is not an error because the fill turns it into a foundation; burial would put
terrain inside the temple, and no fill can undo that.

### 4.3 Rotation retry

`TerrainPredicate#triesAllRotations` is false by default and true for `BastionHeightCheck`. For such a rule
`validateLayout` builds the layout with each of the four start rotations, in a shuffled order, and keeps the
first fit:

- the rule is direction-dependent: the gate is anchored, and a rise under any other wing fails, so a candidate
  that fails facing one way can fit facing another;
- the four fits almost never overlap (278 of the 282 fitting candidates measured for Myouren fit in exactly
  one rotation), so trying all four is close to four independent chances;
- the order is shuffled from the candidate's own seeded random, so it does not disturb the layout stream, and
  the rare candidate that fits twice does not always take the lowest ordinal.

Measured on the `myouren` seed over 12,288 regions (three 2048 x 2048 chunk samples), among candidates that
reached the layout stage: fit 0.43% -> 1.75% (4.1x), and region placement 0.54% -> 1.96% (3.65x). The extra
cost is three layout builds only on candidates that already passed the prefilter and the margin check
(~1.3 per region, tens of milliseconds per region in total).

`UniformHeightCheck` keeps the drawn rotation: its symmetric rule is not direction-dependent, and the root
footprint it samples is centred on the start.

## 5. Foundation fill

### 5.1 Where

`BastionHeightCheck#afterPlace(level, chunk, pieces)` does the fill, in the same hook vanilla uses for the
woodland mansion's cobblestone: `StructureStart#placeInChunk` calls it after that chunk's jigsaw pieces
have run. It is not a `StructurePiece`, so there is no piece registration, no NBT, and nothing for the
`Beardifier` to read (§5.4). The predicate's `afterPlace` default is a no-op, so only the bastion rule
fills.

### 5.2 Top plane

Scan `pieces` for the anchor (`PoolElementStructurePiece#getElement` is a `SinglePoolElement` whose
template id is the anchor id, see §4.2) and take `PieceTree.groundOf(piece)` = `bbox.minY + ground_level_delta`,
the group's shared plane.
`top = groundY - 1` is the template's bottom layer, `groundY - 2` the first fill cell. Using
`pieces.calculateBoundingBox().minY()` like the mansion would pick the pond's bottom (`-2` for Myouren),
three blocks under the tier, so the anchor is the reliable source.

### 5.3 Algorithm

Per chunk, for each column of the chunk box:

1. **guard:** the block at `top` must be non-air, and the column must be inside a piece box
   (`pieces.isInsidePiece`). That cell is the bottom layer of whatever stands on the ground tier here;
   air means open ground under an arch, an eave, a pond or between wings, not under a floor, and must
   not be filled.
2. **scan down:** from `top - 1` (that is `groundY - 2`), place `fill` and stop at the first non-air
   block. Terrain is placed before features, so the first non-air block is the real surface (beard
   included). The scan is bounded only by the world's minimum build height: a piece may float any
   distance, so every gap must be closable.
3. Air-only (not vanilla's "non-air and non-liquid") keeps a dug pond's water; a column that already
   meets the surface stops on its first non-air block without disturbing anything below.

Per-piece `bbox.minY` would be wrong as the top plane: a roof, a tree canopy or a pond wall hangs above
the ground plane, and filling to its underside builds a fill column up through the temple. The anchor's
`groundOf` is the one y every piece in the group agrees on, and the guard does the local distinction.

### 5.4 Beard

The fill runs after terrain generation, and `BEARD_THIN` raises terrain under every rigid piece, which is
exactly the gap the fill is meant to close. So Myouren's configured `terrain_adaptation` is `none`, and
the beard is per piece:

- `GLBeardedPiece` implements NeoForge's `PieceBeardifierModifier`;
  `BastionHeightCheck#beardAdjustment` returns `BEARD_THIN` for the gate and `NONE` for every other
  piece.
- The vanilla `Beardifier` drops a structure whose `terrain_adaptation` is `none` before it reads any
  piece, so `FlatCheckStructure#terrainAdaptation()` advertises `BEARD_THIN` when the rule says
  `needsBeardifier()`. The configured value stays `none` (the codec omits the default); the
  `adjustBoundingBox` inflation that comes with it is what the Beardifier needs at the footprint edge.
- `GLBeardedPiece` is a registered `StructurePieceType` and writes its adjustment to NBT. A plain
  `PoolElementStructurePiece` would load with the structure default and lose the per-piece override.

With every piece opting out except the gate, the Beardifier leaves the space under the halls air and
`afterPlace` fills it with cobblestone. It also stops adding junction beards for Myouren, because the
`PieceBeardifierModifier` branch does not collect `JigsawJunction`s; that is the wanted behaviour for a
foundation. The trade-off is that a non-gate piece gets no beard carve either, so terrain may still stand
up to `tolerance - 1` blocks high at a piece centre; the fit in §4.2 is the only guard against burial.

## 6. Measured Myouren layout

Assembled from the 29 `structure/myouren_temple/*.nbt` templates with the rigid-rigid junction rule:

| pieces | `bbox.minY` | `groundOf` |
|---|---|---|
| gate, root, path, stairs, rooms, walls, side, tree_0/1, bell (23) | 0 | 1 |
| pond | -2 | 1 |
| roof_3 | 10 | 1 |
| tree_3, tree_4 | 11 | 1 |
| tree_2 | 14 | 1 |
| roof | 18 | 1 |

Consequences for the design:

- the gate is at the same ground plane as everything else, so anchoring it to the terrain anchors the
  whole temple;
- `groundOf` cannot separate the roof from the floor (all 1); `bbox.minY` can, but the fill does not
  need the distinction — the column guard in §5.3 does it locally;
- the fill region is 89 x 97 (x -26..62, z -45..51), so the `afterPlace` pass runs once per intersecting
  chunk rather than once per structure.

## 7. Datagen

`StructFlatBuilding` and `StructFlatJigsawBuilding` trade `int heightTolerance` for a
`TerrainPredicate`, forwarded into `FlatCheckStructure`. Call sites:

| structure | predicate |
|---|---|
| `cirno_nest`, `marisa_house`, `morichika_shop`, `hakurei_shrine`, `alice_house` | `new UniformHeightCheck(5)` |
| `myouren_temple` | `new BastionHeightCheck(GensokyoLegacy.loc("myouren_temple/gate"), 4, Blocks.COBBLESTONE.defaultBlockState())` |

`runData` rewrites the six `worldgen/structure/*.json`: `height_tolerance` moves inside `terrain`, and
`myouren_temple` gains the `terrain` object with its `anchor` (`height_tolerance` equals its default, so it
is not written out; `fill` is cobblestone, not the default, so it is). `StructFlatJigsawBuilding` writes
`terrain_adaptation: none`
when the rule reports `needsBeardifier()`; the codec omits a `none` value, so Myouren's JSON has no
`terrain_adaptation` field while the other five keep `beard_thin`. The generated files must be committed
with the code.

## 8. Diagnostics

`Validated.reason` keeps the stage prefixes `stageOf` reads. New reasons:

| reason | stage |
|---|---|
| `anchor-missing`, `anchor-group` | 2 (layout) |
| `anchor-sea`, `sea` | 3 (root) |
| `anchor[lo-hi]` | 4 (rigid) |
| `rigid-%d[h-vs-g]`, `rigid-sea-%d` | 4 (rigid) |
| `joint[lo-hi]` | 5 (uniform only) |

`TerrainPredicate#describe` prints the numbers behind a failing fit — one line per ring sample and per
rigid piece — and defaults to nothing, so only the rule that has numbers to show implements it.
`FlatCheckStructure#diagnoseRegion` mirrors `findGenerationPoint` without placing, one line per candidate;
`#diagnoseAt` picks the region from a chunk. In game:

```
/gensokyo structure diagnose <structure> [<regionX> <regionZ>]
```

prints the candidate verdicts, then the first candidate that reached the fit with its full trace (anchor
resolution, gate ring heights, plane, and every rigid piece's `h` / `g` / gap / verdict) to chat and the
log. A wrong anchor id shows up there as `anchor ... -> #-1` plus the template id of every piece, which is
how the `GLSinglePiece`/`SinglePoolElement` mismatch from §4.2 was found.

## 9. Decisions

- **Floating unbounded, burial bounded.** The check rejects terrain only when it rises above
  `ground + t`; a piece may hang over a valley at any depth and the fill builds the foundation down to
  the surface. Bounding the gap instead (the first version of this rule) let barrens hillsides bury the
  halls, which no fill can undo.
- **No per-column height sampling at placement.** `BastionHeightCheck` samples 9 points for the anchor
  and one centre per rigid piece (29 for Myouren), like the old rule. Precomputing the gap of every
  column would mean ~8.6 k `getFirstFreeHeight` calls per candidate, and every candidate chunk
  re-evaluates the earlier candidates of its region; the fill instead reads the world once per chunk.
- **Anchor matched by template id, not element class.** Datagen's `GLSinglePiece` serialises as
  `minecraft:single_pool_element`, so a loaded pool holds plain `SinglePoolElement`; an `instanceof
  GLSinglePiece` test is always false and every candidate fails `anchor-missing`. `PieceTree#templateId`
  reads the protected `template` field instead (access transformer), which works for both classes.
- **Gate-only beard, via `PieceBeardifierModifier`.** The rule, not the structure JSON, owns which pieces
  blend into the terrain. The structure advertises `BEARD_THIN` only because the Beardifier filters
  `NONE` structures before it can read a piece, and every piece except the anchor reports `NONE`. This
  keeps the foundation air-only without a custom Beardifier or a mixin.
- **`afterPlace`, not a foundation `StructurePiece`.** The same hook the woodland mansion uses for its
  cobblestone: no foundation piece, no plate NBT, and nothing extra for the `Beardifier` to read. It runs
  after the chunk's pieces, so the guard sees the placed building. `GLBeardedPiece` exists for the beard
  override, not for the foundation.
- **One anchor plane, not per-piece fill.** See §5.2.
- **Guard at `groundY - 1`, not a piece-block test.** One block read per column separates "a floor
  stands on this column" from "open ground under an overhang" without walking templates; the piece-box
  test from the mansion is kept on top of it.
- **Structure-level `height_tolerance` retired.** It had no reader outside the rule, and two predicates
  with different knobs should not share one field name in the structure JSON. Structures live in the
  jar, not in world data, so no released save depends on the old field.
- **The terrain-predicate debug tool is temporary.** Remove it once Myouren Temple generation is fully working and
  has been tested in a live world.

# Fallback structures — the tiny tier

A `FallbackStructure` is the tiny tier of a structure set. It lives in the same `StructureSet` as the
`FlatCheckStructure` primaries and `SecondaryStructure` secondaries and only generates at placement candidates that
are not within the shared radius of a higher-tier start. It never takes part in the region pick.

Related: [placement.md](placement.md), [secondary.md](secondary.md), [structure_bed_entity.md](structure_bed_entity.md).

## 1. Rules

| Rule | Consequence |
|---|---|
| A region remains owned by one `FlatCheckStructure`, chosen by `pickForRegion(seed, rx, rz, salt, flatCount)` and gated by that member's `set_index`/`set_count`. | Fallbacks have no `set_index`/`set_count` and do not change either number. |
| Every placement attempt (candidate chunk) is associated with one fallback member, picked from the position. | `FallbackStructure` picks `pickForPosition(seed, chunkX, chunkZ, salt, fallbackCount)` among the set's fallback members; only the chosen member may generate. |
| A fallback is attempted where the region's picked primary started nowhere in the region. | The fallback abstains within the radius of the primary's start (`p`) and of the secondary's start (`s`); a reserved primary pick starts nowhere and yields nothing to it. |
| A fallback must not sit too close to a primary or secondary start. | Yield within the shared `safety_radius` of the start chunk; the higher tier always wins, with no centre tie-break. |
| Fallbacks may generate at several candidates in one region. | One start per candidate chunk; a candidate within the primary's `safety_radius` of a kept earlier candidate of the same region is skipped. |
| A fallback must not sit too close to a fallback in a neighbouring region. | A candidate below half the primary's `safety_radius` from any region border is skipped, so cross-border pairs are always more than the radius apart without scanning the neighbour. |
| A fallback has lower priority than a secondary structure. | The fallback also yields to the secondary's start; see [secondary.md](secondary.md) §7. The fallback never feeds back into the primary or secondary gates. |

`MultiSpreadPlacement` already yields `attempts` candidate chunks per `spacing x spacing` region, and vanilla
`ChunkGenerator#createStructures` visits every set member at each candidate and stops at the first valid start. The
fallback is therefore just another set member: the answer is decided inside `findGenerationPoint`, not in the
placement.

## 2. Membership and released worlds

The fallback belongs to the **same** `StructureSet` as the primaries and secondaries (`"set": "<same id>"`). This is
required: a structure that is not in the set is never asked for a start.

Datagen keeps the tiers apart:

- `set_count` is computed over `FlatCheckStructure` members only, as today (including the reserved-slot total of
  the forest houses); the secondary has its own index space;
- the fallback's position among `FallbackStructure` members in `structures` is its identity for the per-position
  pick; it is read back from the set at worldgen, so the entry needs no index field;
- adding, removing or reordering fallbacks reshuffles which fallback appears at a position, but cannot move a
  primary or secondary pick, because neither `flatCount` nor `set_index` changes. This is the "does not occupy set
  index / add to set count" guarantee.

A region's primary pick may land on a reserved slot (forest houses reserve 2-3). There is no primary member with that
index, so there is no primary start to yield to: every candidate of the region is eligible for the fallback, subject
to the fallback's own gates (position pick, prefilter, layout) and the distance rules. The same holds when the region's
secondary pick is reserved or its check fails.

A set with fallbacks but without primary members places nothing — the distance radius comes from the primaries — so
datagen rejects that configuration.

## 3. Decision flow

`findGenerationPoint(ctx)` at candidate `me`:

```
1. resolve set -> MultiSpreadPlacement, primary members, secondary members, fallback members
   (empty when unbound / set is not MultiSpreadPlacement / this is not in the set)
2. me must be one of placement.getPotentialChunks(seed, me.x, me.z), at candidate index idx
3. flatCount = the primary members' set_count (the datagen total, reserved slots included), not the member list
   size; flatRadius = max safety_radius over the set's primary members
4. border margin: 2 * borderDist(me) < flatRadius -> empty
5. fallback spacing: greedily walk candidates 0 .. idx-1, discarding points that fail step 4, and keeping a
   candidate only when it is farther than flatRadius (squared distance) from every already kept point; if me is
   within that radius of any kept point -> empty
6. if this is not pickForPosition(seed, me.x, me.z, placement.salt(), fallbackCount) among the fallbacks -> empty
7. fallback prefilter (sea / own biome tag at the middle column) fails -> empty
8. higher-tier distance: p = the picked primary's first passing candidate (memo, -1 when reserved or failing) and
   s = the picked secondary's start (memo, -1 when its gate fails); reject me when it is within flatRadius of
   cand[p] or cand[s] in this region, and for every neighbouring region when me is closer to that border than
   the radius
9. fallback's own layout check (jigsaw, biome gate, terrain predicate) -> stub or empty
```

A reserved region owns no start: steps 4-7 and 9 still run, and step 8 finds nothing to yield to, so every candidate
that survives the border margin, spacing, the position pick and the prefilter is attempted. Step 8 still reads the
neighbouring regions' verdicts, because the distance rule is about the actual starts around `me`, not about this
region's ownership.

`borderDist` is the same quantity `marginSafe` uses: the smallest chunk distance from `me` to the four edges
of its `spacing x spacing` region. The `2 * borderDist < flatRadius` form means a kept point is at least half a
radius from every edge. Two points in adjacent regions at distances `d` and `d'` from the shared edge have chunk
coordinates `d + d' + 1` apart on the normal axis, so even at the smallest kept distance the separation is at least
`2 * ceil(radius / 2) + 1 > radius`; Euclidean distance is at least that, so the `<= radius` collision test cannot
fire. The `+ 1` is the edge chunk itself, which is why exactly half a radius is allowed.

Gate order is the cost order. Steps 3-6 are pure arithmetics on the seed and candidate list, step 7 is two
height/biome samples, step 8 is memo lookups plus one `getPotentialChunks` per neighbour consulted, and step 9 is the
speculative fallback layout. A memo lookup can trigger one whole-region higher-tier scan, which is shared by every
candidate of that region, the secondary and the tiny tier. No speculative primary or secondary layout is built here.

Step 9 must be the flat's **single-candidate** verdict, not "the flat placed nothing in this region": a candidate
suppressed by an earlier flat pass still answers "the flat passed here", so the fallback abstains. This keeps one
answer per candidate and needs no region-wide simulation.

The context handed to step 9 must seed its random like the flat's own: vanilla's `GenerationContext` constructor
already does `setLargeFeatureSeed(seed, chunkX, chunkZ)`, and `FlatCheckStructure#ctxFor(ctx, pos)` does the same, so
the fallback builds one for `me` through the flat and passes that. The flat check reads the flat's `biomes()`,
`startPool` and the terrain predicate from the instance, not from the context.

### Fallback spacing is greedy, not verified

Step 5 compares the candidate positions of the region only. A candidate counts as a previous selected point even if
the flat owns it, even if its associated fallback is a different member, and even if it would fail its own
prefilter or layout: the rule deliberately checks no world state and no generation verdict for earlier points. Only
the two positional gates apply: a point that fails the border margin or spacing is not kept, so it does not block
points farther along the chain. The candidate order is fixed by `MultiSpreadPlacement#candidates`, so every
fallback computes the same kept chain.

Cross-region pairs are handled by step 4 instead of a neighbour scan. Both sides of any shared border apply the same
half-radius margin, so the closest two fallbacks in adjacent regions are more than one radius apart; the half-radius
band is symmetric, so neither side needs to know whether the other kept a point.

## 4. The distance rule

The flat's own rule stays where it is: `JigsawCheck#marginSafe` (shared with the secondary) rejects a candidate within
`safety_radius` of a neighbouring region's prefilter-passing candidate that is at most as close to its region centre.
The fallback applies the same radius to a different pair:

- higher tier vs fallback: `FallbackStructure#nearStart` rejects a candidate within `safety_radius` chunks of the
  picked primary's start (`p`) or the picked secondary's start (`s`) in the same region; `nearNeighbourStart` does the
  same for a neighbouring region, and only runs when the candidate is closer to a border than the radius. The higher
  tier always wins: no tie-break, because the higher tier does not evaluate fallbacks and a tie-break could let both
  through. `p` and `s` are the memoised whole-region verdicts of [secondary.md](secondary.md) §4, so the fallback never
  rebuilds a higher-tier layout. A reserved pick owns no start, so only tiers that hold a start are consulted.
- fallback vs fallback: `FallbackStructure#spacingBlocked`, per step 5 — candidates are walked in attempt order and a
  candidate within `safety_radius` chunks of a kept earlier point is skipped, without checking whether that earlier
  point generated anything. A skipped point is not kept, so it does not block later candidates. Across regions,
  step 4 skips candidates below half the radius from any border, which bounds every cross-border pair to more
  than one radius without a neighbour scan.

The fallback reads `safety_radius` from the set's primary members rather than owning a copy (`max` across them, should
a datapack let them disagree), so the tiers cannot drift if the radius is tuned. Datagen keeps the value equal across a
set's primaries.

## 5. Code plan

### New

- `content/worldgen/structure/FallbackStructure.java`
  - codec: `settings`, `start_pool`, `size`, `use_expansion_hack`, `max_distance_from_center`, `terrain`,
    `set`, and no `set_index`/`set_count`;
  - `findGenerationPoint` per §3; own layout check through `JigsawCheck`;
  - `resolve()` reads placement, primary and secondary members, fallback members and this member's fallback index from
    the set;
  - static `pickForPosition`, `spacingBlocked` (step 5), `nearStart` and `nearNeighbourStart` (step 8);
  - port `FlatCheckStructure`'s `verifyRange` guard (`max_distance_from_center + 12 <= 128`);
  - no `LocatableStructure`: the tiny tier has no region pick, so the dev locate tools reject it.
- `content/worldgen/structure/JigsawCheck.java`
  - moved `validateLayout` + `prefilter` + `Prefilter` + `Validated` out of `FlatCheckStructure`
    into a record parameterised by start pool, depth, expansion hack, max distance, `TerrainPredicate` and biome
    holder; all three structures call it.
- `init/data/structure/helper/StructFallbackBuilding.java`
  - mirrors `StructFlatJigsawBuilding`: one template pool per part at `<id>/<part>`, generation from the root part,
    and registers `FallbackStructure` with `StructBuilding.setRef(context)`.
  - no beds, no home config and no `safety_radius` of its own.
- `init/data/structure/helper/StructFallbackSingleBuilding.java`
  - the same fallback with one template, mirroring `StructFlatBuilding`: the pool is registered under the structure
    id, but its sole element points at a separate template id, so a biome plot can stay under `biome/...`.

### Changed

- `init/registrate/GLWorldGen.java`: `STRUCTURES.reg("fallback", () -> () -> FallbackStructure.CODEC)`.
- `FlatCheckStructure`: exposes package-private `setIndex()`, `setCount()`, `safetyRadius()` and
  `firstPassInRegion(...)`; its `checkMarginSafe` moved into `JigsawCheck#marginSafe`; no public API change.
- `MultiSpreadPlacement`: static `borderDistance(ChunkPos, int)` and `distanceSquared(ChunkPos, ChunkPos)` for the
  fallback's positional gates.
- `helper/SetContext.java`: `tiny(self)` hands out a context with `setIndex = setCount = -1`; primaries and secondaries
  use `of(self, sameTier, total)` with indices computed over their own tier only.
- `helper/StructBuilding.java`: `default int safetyRadius()` returning 0 and `default Role role()` returning `PRIMARY`;
  the flat building records already expose the component under that name, and `StructFallbackBuilding#role` returns
  `TINY`.
- `helper/StructStructure.java`: unchanged — a fallback is recognised by its building's `role()`.
- `init/data/structure/GLStructureGen.java`
  - `Registries.STRUCTURE`: primaries and secondaries get `SetContext.of(e, sameTier, setTotal(...))`; tinies get
    `SetContext.tiny(e)`;
  - `Registries.STRUCTURE_SET`: keep writing **all** members — vanilla needs them in the set; the existing
    spacing/attempts check now runs over every member, plus a `safety_radius` check over the primary members, and a
    set without primaries is rejected;
  - `dataMap`: skips `Role.TINY` members (no beds, rooms or visits, nothing to register);
  - first entry: `small_graveyard` in the `myouren_temple` set, template
    `gensokyolegacy:biome/graveyard/small_graveyard` from `structure/biome/graveyard/small_graveyard.nbt`,
    `UniformHeightCheck(5)`, no home config. It shares the barrens tag with the temple and fills the candidates the
    temple's own fit rejected; the barrens' pools, groves and tombs avoid both members.
- `doc/design/structure/placement.md`: one line in §1 pointing here.

## 6. Edge cases

- **Set not bound / not a `MultiSpreadPlacement`:** fallback places nothing, same as the flat.
- **Fallback not listed in `set`:** `indexOf(this) < 0` → nothing.
- **Fallback count changes:** per-position association reshuffles; flat picks do not. Documented as acceptable
  because every flat `set_count` is untouched.
- **Flat passes at this candidate but was suppressed by an earlier flat pass:** fallback abstains; the candidate
  stays empty. This follows "if the flat failed the check" literally and avoids a second start at a chunk the flat
  owns.
- **Region pick is reserved:** no flat owns the region, so step 9 is skipped and every candidate is fallback
  territory; the border margin, spacing, the fallback's own gates and the neighbouring regions' flats still apply.
- **Candidate near a region border:** skipped below half the flat's `safety_radius`, which keeps fallback pairs
  in adjacent regions more than one radius apart without checking the neighbour's kept points.
- **`/locate`:** the mixin already walks every candidate and asks each member; a fallback is found when its own
  `findGenerationPoint` passes. Locating a primary or secondary cannot return a fallback, because
  `getStructureGeneratingAt` only reports starts of the requested holders.
- **Cost:** a memoised higher-tier verdict per region, not per candidate: the first tiny candidate of a region that
  needs the picked primary's or secondary's start scans that region once, and every later candidate of the region (and
  the higher tiers' own candidates) read the same entry. No speculative flat layout is built per tiny candidate.

## 7. Verification

- On a dev world over several seeds, check that:
  - no position has both a flat pass and a fallback start;
  - a region with a flat start may still hold fallbacks, but none within `safety_radius` of a passing flat
    candidate;
  - reserved-pick regions may hold fallbacks at any candidate that survives the spacing and distance gates,
    and hold none within `safety_radius` of a flat in a neighbouring region;
  - every kept fallback candidate has `2 * borderDist >= safety_radius`, and no two fallbacks — same region or
    across a border — are within `safety_radius` of each other;
  - fallbacks appear at several candidates in one region on average, and are not adjacent.
- Re-run `tools/terrain.py` / the existing structure checks only if a fallback `.nbt` or `StructureConfig` is added;
  fallbacks have no home config, so `tools/checkconfig.py` does not apply.

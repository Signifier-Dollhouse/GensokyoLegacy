# Fallback structures — filling the positions a flat check rejects

A `FallbackStructure` is a second structure type that lives in the same `StructureSet` as the
`FlatCheckStructure` members and only generates at placement candidates where the region's picked flat structure
failed its eligibility check, or where the region's pick is a reserved slot. It never takes part in the region pick.

Related: [placement.md](placement.md), [structure_bed_entity.md](structure_bed_entity.md).

## 1. Rules

| Rule | Consequence |
|---|---|
| A region remains owned by one `FlatCheckStructure`, chosen by `pickForRegion(seed, rx, rz, salt, flatCount)` and gated by that member's `set_index`/`set_count`. | Fallbacks have no `set_index`/`set_count` and do not change either number. |
| Every placement attempt (candidate chunk) is associated with one fallback member, picked from the position. | `FallbackStructure` picks `pickForPosition(seed, chunkX, chunkZ, salt, fallbackCount)` among the set's fallback members; only the chosen member may generate. |
| A fallback is attempted where the region's picked flat failed its check at that candidate; a reserved pick has no flat, so every candidate of that region is eligible. | Mutual exclusion at one candidate: the fallback abstains when the flat passes, and the flat already returns empty when it fails. A reserved region has no flat to yield to. |
| A fallback must not sit too close to a flat. | Reuse the flat's `safety_radius` margin rule, with the flat always winning. |
| Fallbacks may generate at several candidates in one region. | One start per candidate chunk; a candidate within the flat's `safety_radius` of a kept earlier candidate of the same region is skipped. |
| A fallback must not sit too close to a fallback in a neighbouring region. | A candidate below half the flat's `safety_radius` from any region border is skipped, so cross-border pairs are always more than the radius apart without scanning the neighbour. |

`MultiSpreadPlacement` already yields `attempts` candidate chunks per `spacing x spacing` region, and vanilla
`ChunkGenerator#createStructures` visits every set member at each candidate and stops at the first valid start. The
fallback is therefore just another set member: the answer is decided inside `findGenerationPoint`, not in the
placement.

## 2. Membership and released worlds

The fallback belongs to the **same** `StructureSet` as the flats (`"set": "<same id>"`). This is required: a
structure that is not in the set is never asked for a start.

Datagen keeps the two groups apart:

- `set_count` is computed over `FlatCheckStructure` members only, as today (including the reserved-slot total of
  the forest houses);
- the fallback's position among `FallbackStructure` members in `structures` is its identity for the per-position
  pick; it is read back from the set at worldgen, so the entry needs no index field;
- adding, removing or reordering fallbacks reshuffles which fallback appears at a position, but cannot move a
  flat region pick, because neither `flatCount` nor `set_index` changes. This is the "does not occupy set index /
  add to set count" guarantee.

A region's pick may land on a reserved slot (forest houses reserve 2-3). There is no flat member with that index,
so there is no flat ownership to yield to: every candidate of the region is eligible for the fallback, subject to
the fallback's own gates (position pick, prefilter, layout) and the distance rules.

A set with fallbacks but without flat members places nothing — the distance radius comes from the flats — so
datagen should reject that configuration.

## 3. Decision flow

`findGenerationPoint(ctx)` at candidate `me`:

```
1. resolve set -> MultiSpreadPlacement, flat members, fallback members
   (empty when unbound / set is not MultiSpreadPlacement / this is not in the set)
2. me must be one of placement.getPotentialChunks(seed, me.x, me.z), at candidate index idx
3. flatCount = the flat members' set_count (the datagen total, reserved slots included), not the member list
   size; flat = the flat member whose set_index == pickForRegion(seed, region(me), placement.salt(), flatCount);
   flatRadius = the safety_radius shared by the set's flat members
   -- a reserved slot leaves flat = null and does not stop the fallback
4. border margin: 2 * borderDist(me) < flatRadius -> empty
5. fallback spacing: greedily walk candidates 0 .. idx-1, discarding points that fail step 4, and keeping a
   candidate only when it is farther than flatRadius (squared distance) from every already kept point; if me is
   within that radius of any kept point -> empty
6. if this is not pickForPosition(seed, me.x, me.z, placement.salt(), fallbackCount) among the fallbacks -> empty
7. fallback prefilter (sea / own biome tag at the middle column) fails -> empty
8. flat margin: for the picked flat of me's region (when flat != null, me excluded) and of every neighbouring
   region, a candidate within flatRadius chunks of me that passes that flat's prefilter -> empty
9. flat != null and flat.passesCandidate(ctxFor(me), me, placement) -> empty   // the flat owns this candidate
10. fallback's own layout check (jigsaw, root height tolerance, rigid/joint) -> stub or empty
```

A reserved region runs steps 1-8 and 10 with `flat = null`: step 9 has no owner to yield to, so every candidate
that survives the border margin, spacing, the position pick and the prefilter is attempted. Step 8 still looks at
the neighbouring regions' picks, because the distance rule is about the actual flat starts around `me`, not about
this region's ownership.

`borderDist` is the same quantity `checkMarginSafe` uses: the smallest chunk distance from `me` to the four edges
of its `spacing x spacing` region. The `2 * borderDist < flatRadius` form means a kept point is at least half a
radius from every edge. Two points in adjacent regions at distances `d` and `d'` from the shared edge have chunk
coordinates `d + d' + 1` apart on the normal axis, so even at the smallest kept distance the separation is at least
`2 * ceil(radius / 2) + 1 > radius`; Euclidean distance is at least that, so the `<= radius` collision test cannot
fire. The `+ 1` is the edge chunk itself, which is why exactly half a radius is allowed.

Gate order is the cost order. Steps 3-6 are pure arithmetics on the seed and candidate list, step 7 is two
height/biome samples, step 8 samples only candidates inside the radius (normally a handful), step 9 is the
speculative flat layout, and step 10 the speculative fallback layout. In a region where the flat passes early, most
candidates stop at step 9; in a region where it never passes they pay both layouts; a reserved region skips step 9
and pays only the fallback layout. Both are the layouts the two structures would build for themselves anyway.

Step 9 must be the flat's **single-candidate** verdict, not "the flat placed nothing in this region": a candidate
suppressed by an earlier flat pass still answers "the flat passed here", so the fallback abstains. This keeps one
answer per candidate and needs no region-wide simulation.

The context handed to step 9 must seed its random like the flat's own: vanilla's `GenerationContext` constructor
already does `setLargeFeatureSeed(seed, chunkX, chunkZ)`, and `FlatCheckStructure#ctxFor(ctx, pos)` does the same, so
the fallback builds one for `me` through the flat and passes that. The flat check reads the flat's `biomes()`,
`startPool` and tolerance from the instance, not from the context.

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

The flat's own rule stays where it is: `FlatCheckStructure#checkMarginSafe` rejects a candidate within
`safety_radius` of a neighbouring region's prefilter-passing candidate that is at most as close to its region centre.
The fallback does not reuse that method, because it needs the radius test without the centre tie-break; it applies the
same radius to two pairs instead:

- flat vs fallback: `FallbackStructure#nearFlat`. The flat always wins. A flat candidate within `safety_radius`
  chunks whose prefilter passes rejects the fallback; no tie-break, because the flat does not evaluate fallbacks and
  a tie-break could let both through. The same region's picked flat is scanned over its own candidate list, a
  neighbouring region's picked flat over that region's candidates, and the neighbour scan only runs when the
  candidate is closer to a border than the radius.
- fallback vs fallback: `FallbackStructure#spacingBlocked`, per step 5 — candidates are walked in attempt order and a
  candidate within `safety_radius` chunks of a kept earlier point is skipped, without checking whether that earlier
  point generated anything. A skipped point is not kept, so it does not block later candidates. Across regions,
  step 4 skips candidates below half the radius from any border, which bounds every cross-border pair to more
  than one radius without a neighbour scan.

The fallback reads `safety_radius` from the set's flat members rather than owning a copy, so the two cannot drift
if the flat's value is tuned. A reserved region has no picked flat, so the value is taken from any flat member of
the set; datagen must keep the value equal across a set's flats.

The flat check uses the prefilter only, mirroring the existing flat rule: cheap, and conservative when the nearby
flat would have failed its full layout.

## 5. Code plan

### New

- `content/worldgen/structure/FallbackStructure.java`
  - codec: `settings`, `start_pool`, `size`, `use_expansion_hack`, `max_distance_from_center`, `height_tolerance`,
    `set`, and no `set_index`/`set_count`;
  - `findGenerationPoint` per §3; own layout check through `JigsawCheck`;
  - `resolve()` reads placement, flat members, fallback members and this member's fallback index from the set;
  - static `pickForPosition`, `spacingBlocked` (step 5) and `nearFlat` (step 8);
  - port `FlatCheckStructure`'s `verifyRange` guard (`max_distance_from_center + 12 <= 128`).
- `content/worldgen/structure/JigsawCheck.java`
  - moved `validateLayout` + `prefilter` + `freeHeight` + `Prefilter` + `Validated` out of `FlatCheckStructure`
    into a record parameterised by start pool, depth, expansion hack, max distance, tolerance and biome holder;
    both structures call it, and `FlatCheckStructure#diagnoseRegion` keeps working through `check()`.
- `init/data/structure/helper/StructFallbackBuilding.java`
  - mirrors `StructFlatJigsawBuilding`: one template pool per part at `<id>/<part>`, generation from the root part,
    and registers `FallbackStructure` with `StructBuilding.setRef(context)`.
  - no beds, no home config and no `safety_radius` of its own.

### Changed

- `init/registrate/GLWorldGen.java`: `STRUCTURES.reg("fallback", () -> () -> FallbackStructure.CODEC)`.
- `FlatCheckStructure`: exposes package-private `setIndex()`, `setCount()`, `safetyRadius()`, `passesCandidate(...)`,
  `prefilterAt(...)` and `ctxFor(ctx, pos)`; no public API change.
- `MultiSpreadPlacement`: static `borderDistance(ChunkPos, int)` and `distanceSquared(ChunkPos, ChunkPos)` for the
  fallback's positional gates.
- `helper/SetContext.java`: `fallback(self)` hands out a context with `setIndex = setCount = -1`; flats keep using
  `of(self, flats, total)` with indices computed over the flat members only.
- `helper/StructBuilding.java`: `default int safetyRadius()` returning 0; the flat building records already expose
  the component under that name.
- `helper/StructStructure.java`: unchanged — a fallback is recognised as `building instanceof StructFallbackBuilding`.
- `init/data/structure/GLStructureGen.java`
  - `Registries.STRUCTURE`: flats get `SetContext.of(e, flats, setTotal(e.set(), flats))`; fallbacks get
    `SetContext.fallback(e)`;
  - `Registries.STRUCTURE_SET`: keep writing **all** members — vanilla needs them in the set; the existing
    spacing/attempts check now runs over every member, plus a `safety_radius` check over the flat members, and a
    set without flats is rejected;
  - `dataMap`: skips fallbacks (no beds, rooms or visits, nothing to register);
  - example entry: one fallback in `magical_forest_houses` (its own template), one in the `hakurei_shrine` /
    `cirno_nest` sets once their fallback assets exist. The fallback's biome tag is the union of the set's flat
    tags; the forest houses all use `magical_forest`, so that tag is reused as is.
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
  `findGenerationPoint` passes. Locating a flat cannot return a fallback, because `getStructureGeneratingAt` only
  reports starts of the requested holders.
- **Cost:** roughly one extra flat layout per candidate in a region where the flat passes early, roughly two
  layouts per candidate where it never passes. If profiling shows this, memoise the flat verdict per
  `(seed, chunk)`; a per-thread bounded cache is enough because a region is usually walked by one thread.

## 7. Verification

- Extend `FlatCheckStructure#diagnoseRegion` (or add the same report to `FallbackStructure`) with a per-candidate
  line carrying the flat verdict, the chosen fallback, the distance verdict and the fallback verdict; run it over
  several seeds on a dev world and check that:
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

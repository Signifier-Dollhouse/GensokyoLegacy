# Secondary structures — filling regions the primary cannot

A `SecondaryStructure` is the middle tier of a structure set. It is a smaller flat-check structure (usually within
24x24) that only generates in a region where the region's picked `FlatCheckStructure` (the primary) placed no start,
and it places at most one start per region. It never takes part in the primary pick.

The three tiers of a set:

| Tier | Class | Size | Per region | Gate |
|---|---|---|---|---|
| Primary | `FlatCheckStructure` | 30-60 | at most one | region pick, then the first candidate passing the flat check |
| Secondary | `SecondaryStructure` | up to 24 | at most one, only if no primary start | primary verdict is "no start", own region pick, then the first candidate passing the flat check |
| Tiny | `FallbackStructure` | up to 16 | many | any candidate that survives its check and is far enough from every kept tiny, primary and secondary start |

Priority is **primary > secondary > tiny**. The tiny tier is the existing `FallbackStructure`; this document calls it
"tiny".

Related: [placement.md](placement.md), [fallback.md](fallback.md), [structure_bed_entity.md](structure_bed_entity.md).

## 1. Rules

| Rule | Consequence |
|---|---|
| A secondary may exist only in a set that contains at least one primary. | Datagen rejects a set with secondaries and no primary; at worldgen a secondary whose set has no primary member places nothing. |
| A secondary is attempted only where the region's picked primary placed no start. | Region-level gate: `firstPassInRegion(...) == -1`. A region that holds a primary start holds no secondary. |
| At most one secondary may start in a region. | One secondary member is picked per region by `pickForRegion` over the **secondary** index space, and only its first passing candidate returns a stub. |
| The secondary has the same prefilter, margin rule and speculative layout as the primary. | Both build a `JigsawCheck` with the set's shared `safety_radius` and run the same `checkCandidate`. |
| The secondary never takes part in the primary pick. | Its `set_index`/`set_count` count secondary members only; adding, removing or reordering secondaries cannot move a primary pick. |
| A reserved primary pick is a region without a primary. | The secondary is eligible there: `firstPassInRegion` is `-1` without scanning, exactly like a primary that failed its check. |
| A tiny structure yields to a secondary. | Tiny candidates within the shared `safety_radius` of the secondary's start are rejected, in the region and across borders. |
| The secondary reads no lower tier. | Its verdict depends on the primary only; the tiny tier is invisible to it, so the tier graph stays acyclic. |

`MultiSpreadPlacement` yields `attempts` candidate chunks per `spacing x spacing` region, and vanilla
`ChunkGenerator#createStructures` visits every set member at each candidate and stops at the first valid start. Priority
is therefore enforced by abstention inside `findGenerationPoint`, not by the order of `StructureSet#structures` (which
vanilla shuffles per chunk).

## 2. Membership, index space and released worlds

The secondary belongs to the **same** `StructureSet` as the primaries and tinies (`"set": "<same id>"`); a structure
that is not in the set is never asked for a start.

- It is a separate class with its own structure type `gensokyolegacy:secondary` and its own codec. The codec omits
  `safety_radius`: the radius is the set's, read from the primary members (see §6).
- `set_index` / `set_count` live in the secondary entry and count **secondary members only**. Datagen computes them over
  the secondary group, not over every member of the set.
- The secondary total may exceed the number of secondary members. A reserved secondary slot means "no secondary in
  this region", and the tiny tier fills it. This is the same reserved-slot pattern the forest houses use for primaries.
- The primary total is still computed over primary members only. The forest houses keep `set_count = 4` with three
  primaries; adding a secondary cannot change the primary pick distribution.
- A secondary's index and its group total are baked into JSON. Like the primary's `set_index`/`set_count`, they must not
  change once released; reordering the authored list is safe because the index travels with the entry.

Datagen guards (see §8):

- a set with secondaries must include at least one primary;
- all members of a set share spacing and attempts;
- all primary members share `safety_radius`; the secondary reads that value;
- all secondary members share `set_count`;
- each secondary's `set_index` is below its `set_count`.

## 3. Region outcome

For region `(seed, rx, rz)`, the set's outcome is a pure function of the seed and the region:

```
p = first candidate in attempt order where the picked primary passes the flat check, else -1
    (-1 when the primary pick is a reserved slot)
s = (p == -1) ? first candidate in attempt order where the picked secondary passes, else -1
    (-1 when the secondary pick is a reserved slot, or s's check never passes)
tiny(t) = t is the per-position picked tiny
          and t passes its own prefilter and layout
          and (p < 0 or dist(t, cand[p]) > radius)
          and (s < 0 or dist(t, cand[s]) > radius)
          and t survives the greedy tiny-vs-tiny spacing and the half-radius border margin
```

A member's whole job is to answer "is this candidate chunk my start?". The tier graph is acyclic:

```
primary    reads only the placement
secondary  reads p
tiny       reads p and s
```

Because no tier reads a lower tier, each verdict is stable, order-independent, and can be memoised.

## 4. The primary verdict: a region memo

`p` is a whole-region verdict. At candidate `i` the primary itself only knows candidates `j < i` (it must, or a region
could hold two primary starts), but the secondary at candidate `0` must know that the primary will pass at candidate
`17`. There is no exact cheaper rule: the primary's check is a pure function of seed and chunk, so proving "no primary
start" means evaluating every candidate of the region once.

### 4.1 `FlatCheckStructure#firstPassInRegion`

```
firstPassInRegion(placement, seed, rx, rz):          // only ever called on the picked primary
    pick = pickForRegion(seed, rx, rz, placement.salt(), setCount)
    if this.setIndex != pick: return -1               // not the picked member
    for i, c in placement.getPotentialChunks(seed, rx*spacing, rz*spacing):
        if checkCandidate(ctxFor(seed, c), c, placement).isPresent(): return i
    return -1
```

A reserved primary pick names no member at all, so there is no instance to call: the secondary's `resolve` reports
"no primary" and the gate at §5 step 4 treats it as `-1` without a scan.

The key is `(ChunkGenerator, seed, rx, rz)`: one registry serves every dimension, and they share a level seed but have
different generators. The primary instance is the cache's owner, so two sets never share an entry. The primary itself
reads this verdict in `findGenerationPoint`, so it no longer rescans candidates `j < i`; the region is scanned once and
every later candidate reads the memo.

### 4.2 `SecondaryStructure#startInRegion`

```
startInRegion(placement, seed, rx, rz):
    pick = pickForRegion(seed, rx, rz, placement.salt() ^ SECONDARY_PICK_MIX, secondaryCount)
    if my secondary index != pick: return -1
    primary = member with the primary pick
    if primary != null and primary.firstPassInRegion(...) != -1: return -1
    for i, c in candidates:
        if checkCandidate(ctxFor(seed, c), c, placement).isPresent(): return i
    return -1
```

The secondary's own scan stops at its first passing candidate. In a region where the primary starts, only the cheap pick
test and the primary memo run. In a region where the primary failed, the scan runs once and usually stops at the first
candidate because a secondary is small and easy to fit.

### 4.3 The cache

- One `RegionMemo` per structure instance, holding one `int` per `(ChunkGenerator, seed, rx, rz)` (`-1` = no start).
- `ConcurrentHashMap`; read with `get`, write with `put`, never `computeIfAbsent`: a scan may build up to `attempts`
  speculative jigsaw layouts and must not hold a bin lock while doing it. Two threads may compute the same region
  concurrently; the function is pure, so the last write is identical.
- Bound the map (clear at a few thousand entries). Eviction only costs a recompute.
- **Clear on server stop.** Every `RegionMemo` registers itself in a static weak set; `ServerStoppedEvent` calls
  `RegionMemo.clearAll()`, which clears every live memo. The weak set means a memo whose structure instance was dropped
  by a datapack reload can be collected without a hook. Add the same `clearAll()` to the existing
  `OnDatapackSyncEvent` (`player == null`) branch of `GLMetaEventHandlers`, next to the other server caches, so a
  reload cannot leave entries keyed to instances that no longer exist.
- A world switch or a new world with the same seed recomputes the same verdicts.

### 4.4 Cost

The memo is shared by every candidate chunk of the region, by the secondary and by the tiny tier, so the scan is paid
once per region instead of once per candidate. The worst case is `attempts` layouts per region (the primary fails
everywhere). The primary's own flow already rescans candidates `j < i` at every `i`, which is quadratic per region, so
the memo is small next to it; and the tiny tier stops paying a speculative primary layout per candidate (§7), which
more than pays for the memo.

## 5. Decision flow

`SecondaryStructure#findGenerationPoint(ctx)` at candidate `me`:

```
1. resolve set -> placement, primary members, secondary members, this member's secondary index
   (empty when unbound / not MultiSpreadPlacement / no primary member / this member not listed)
2. me must be one of placement.getPotentialChunks(seed, me.x, me.z), at candidate index idx
3. primary = the member whose set_index == pickForRegion(seed, region, salt, primaryCount), or null (reserved)
4. primary != null and primary.firstPassInRegion(placement, seed, region) != -1 -> empty
5. pickForRegion(seed, region, salt ^ SECONDARY_PICK_MIX, secondaryCount) != my index -> empty
6. startInRegion(placement, seed, region) != idx -> empty
7. return the stub of its own checkCandidate: prefilter -> margin -> neighbour primary tie-break -> layout
```

`SECONDARY_PICK_MIX` is a distinct constant so the primary and secondary picks are independent draws from the same
salted random family. Steps 3-6 are arithmetic on the seed and the candidate list plus the memo; step 7 is the only
costly part and is memoised.

A `null` primary in step 3 is a reserved pick, so step 4 is skipped: the region counts as having no primary start and
the secondary may fill it (§1, §9).

## 6. Distance rules

The secondary reuses the set's `safety_radius` (read from the primary members, `max` across them if a datapack lets
them disagree) and the primary's margin rule. The pairs:

| Pair | Rule | Who yields |
|---|---|---|
| primary - primary | `marginSafe` centre tie-break, both sides | the start farther from its region centre |
| primary - secondary | the secondary runs `marginSafe` against its own tier and the centre tie-break against the neighbouring primary's start | the secondary unless it is strictly closer to its centre |
| secondary - secondary | both run `marginSafe` | the start farther from its region centre |
| primary - tiny | tiny keeps `> safety_radius` from the primary's start | tiny |
| secondary - tiny | tiny keeps `> safety_radius` from the secondary's start | tiny |
| tiny - tiny | greedy spacing and the half-radius border band | later candidate |

Same-region primary - secondary is impossible by the gate in step 4. Primary/secondary - tiny may coexist in one region
outside the radius; that is what "fill spots without either" means.

The primary cannot read the secondary: that would make its verdict depend on the middle tier and close a cycle. So the
cross-border primary - secondary tie-break is evaluated only by the secondary (`neighbourPrimaryFree`), against the
neighbour's memoised primary start. When the secondary is strictly closer to its region centre, both starts keep and the
pair may sit within the radius; the primary cannot defer. Every other cross-tier pair is handled by the tiny's radius.

`marginSafe` (today `FlatCheckStructure#checkMarginSafe`, moved into `JigsawCheck` by §8) compares chunk distance to the
region centre and tests only the prefilter, so it assumes the two members' prefilters agree at each other's chunk. Keep
a secondary's biome tag equal to the primaries' in the same set (they are all in the set's biomes by construction);
with different tags a cross-border pair could both survive. This is the same caveat the primaries already have.

## 7. Tiny tier changes

The existing `FallbackStructure` becomes the tiny tier. It already runs in every candidate chunk; the changes are:

- `resolve()` collects secondary members as well, alongside the primaries and tinies, and computes the picked
  secondary the same way the secondary itself does.
- The old `nearFlat` (scan the picked primary's candidates within the radius and test the primary's prefilter) and the
  per-candidate `passesCandidate` call are replaced by exact distances: reject `me` when it is within the shared radius
  of `cand[p]` (the primary's start) or `cand[s]` (the secondary's start), in this region and in every neighbouring
  region within the radius of the border.
- The greedy tiny-vs-tiny `spacingBlocked` chain and the `2 * borderDist < radius` border band are unchanged. They are
  still the only tiny-vs-tiny rule.

Why exact: the memo now gives the actual start chunk of the higher tiers, so yielding to every *prefilter-passing*
primary candidate is both unnecessary and harmful. With 24 candidates in a 32x32 region almost every chunk is within
8 chunks of one, so the prefilter rule suppresses the tiny tier nearly everywhere in the primary's biome. Exact
distance also removes one speculative primary layout per tiny candidate.

There is no released-world impact: no tiny entry exists yet (`StructFallbackBuilding` is not used by
`GLStructureGen`), so this changes only planned behaviour. `fallback.md` records the rule as it is implemented today
and links here for the change.

## 8. Code plan

### New

- `content/worldgen/structure/SecondaryStructure.java`
  - codec: `settings`, `start_pool`, `size`, `use_expansion_hack`, `max_distance_from_center`, `terrain`,
    `set`, `set_index`, `set_count`; no `safety_radius`;
  - `findGenerationPoint` per §5, `resolve()` per §2, `startInRegion` per §4.2 and `neighbourPrimaryFree` per §6;
  - `SECONDARY_PICK_MIX`, and a `verifyRange` guard copied from `FlatCheckStructure`
    (`max_distance_from_center + 12 <= 128`);
  - the shared check through `JigsawCheck` (§8 "Changed").
- `content/worldgen/structure/RegionMemo.java`
  - one `ConcurrentHashMap<RegionKey, Integer>` per instance,
    `RegionKey(ChunkGenerator generator, long seed, int rx, int rz)`;
  - `int get(...)` / `void put(...)` / `void clear()`, a static weak registry of live memos, and
    `static void clearAll()` called from `ServerStoppedEvent` and from the datapack-sync branch of
    `GLMetaEventHandlers`.
- `init/data/structure/helper/Role.java` (or a nested enum of `StructBuilding`): `PRIMARY`, `SECONDARY`, `TINY`.

### Changed

- `JigsawCheck`: add `safetyRadius` and `marginSafe(ctx, pos, placement)` (moved from
  `FlatCheckStructure#checkMarginSafe`), and make `checkCandidate` run prefilter -> margin -> layout. This is the single
  copy of the flat check that primary and secondary share.
- `FlatCheckStructure`: `check()` passes its own radius; `checkCandidate` delegates to the shared check; add
  `firstPassInRegion` backed by a `RegionMemo`; `passesCandidate`/`prefilterAt` go away with this change.
  `diagnoseRegion` reads the shared check and can report `p`.
- `FallbackStructure`: collect secondaries in `resolve()`, replace `nearFlat` with `nearStart`/`nearNeighbourStart`
  (§7), reorder the gates so the exact distance checks precede the tiny layout.
- `GLWorldGen`: `STRUCTURES.reg("secondary", () -> () -> SecondaryStructure.CODEC)`.
- `helper/StructBuilding`: `default Role role() { return Role.PRIMARY; }`; `StructFallbackBuilding#role` returns `TINY`.
- `helper/StructFlatBuilding` and `helper/StructFlatJigsawBuilding`: carry a `Role` (PRIMARY by default) and branch in
  `registerStructure` to build `FlatCheckStructure` or `SecondaryStructure`. This replaces the
  `instanceof StructFallbackBuilding` checks in `GLStructureGen`.
- `helper/SetContext`: `of(self, sameRoleMembers, total)` for primaries and secondaries, `tiny(self)` for the tiny tier.
  Indices are computed inside the role group only.
- `init/data/structure/GLStructureGen`:
  - group members by `role()`; primaries use `setTotal(set, PRIMARY)` (the forest houses keep `4`), secondaries use
    `setTotal(set, SECONDARY)` (their own reserved total, default the group size), tinies keep `SetContext.tiny`;
  - the guards of §2;
  - `dataMap` skips `Role.TINY` only: a secondary with a bed or visits is a home and must be registered, and
    `tools/checkconfig.py` applies if it gets a `StructureConfig`.
- `event/GLMetaEventHandlers`: `ServerStoppedEvent` -> `RegionMemo.clearAll()`; the same call in the
  `OnDatapackSyncEvent` (`player == null`) branch.
- `doc/design/structure/placement.md`: link here and name the three tiers.
- `doc/design/structure/fallback.md`: link here; note the priority order and the exact-distance tiny rule.

### Example entry

One secondary in the forest houses (its own template, `magical_forest` biome tag, `set_count` over the secondary
group), and one in the cirno / hakurei sets once their secondary assets exist. Every secondary added to
`STRUCTURES` must also appear in the set's total if it is meant to be pickable.

## 9. Edge cases

- **Reserved primary pick** (forest houses slot 3): there is no primary member for the pick, so the secondary treats
  the primary verdict as `-1` without a scan and is eligible. This is intentional: a reserved slot is a region without
  a primary, and the secondary is exactly what fills those regions. The tiny tier still fills the rest of the region.
  Treat adding a primary into a reserved slot later as a world-breaking change (the existing rule).
- **Set not bound / not a `MultiSpreadPlacement`:** the secondary places nothing, same as the primary.
- **Set has no primary member:** datagen rejects it; a foreign datapack gets `resolve()` -> empty, so nothing places.
- **This member is not listed in its set:** `resolve()` -> empty.
- **Multiple secondaries:** exactly one is picked per region; the others return empty at step 5. Their templates need
  not be related.
- **Reserved secondary pick:** no secondary starts; the tiny tier fills the region.
- **Primary starts at this chunk:** the secondary abstains at every candidate of the region (step 4), not only at the
  primary's chunk.
- **Duplicate candidate chunks** (the placement does not dedupe): the first candidate index wins, both for `p` and for
  `s`; `candidates.indexOf(me)` returns the first occurrence, which is the one that can start.
- **`/locate`:** the mixin asks every requested holder's `findGenerationPoint` at every candidate, so a secondary is
  found when its own gate passes. Locating a primary never reports a secondary, because
  `getStructureGeneratingAt` only reports starts of the requested holders.
- **Datapack-edited radius:** the secondary and the tiny tier read the primaries' `safety_radius`; take the maximum
  across them so a datapack that lets them disagree stays conservative instead of crashing or leaving a hole.
- **Chunk generation order:** the outcome depends only on seed and region, so any order, any thread count and `/locate`
  produce the same starts.
- **Server stop / world switch:** `RegionMemo.clearAll()` drops every entry; a new world recomputes from its own seed.

## 10. Verification

- Add `SecondaryStructure#diagnoseRegion(seed, rx, rz, ...)` printing: the primary pick and `p`, the secondary pick and
  `s`, and a per-candidate verdict (`primary-started`, `not-picked`, `suppressed-by-#k`, `place-<reason>`, `pass`).
- Regression for the primary: run `FlatCheckStructure#diagnoseRegion` over several seeds and regions before and after
  the `JigsawCheck` extraction and diff the output. It must be empty; this is the only guard, because the project has
  no JUnit tests.
- Invariants on a dev world, over several seeds:
  - no region has both a primary start and a secondary start;
  - at most one secondary start per region;
  - every secondary start is at `s`, and its region has `p == -1`;
  - reserved primary regions may hold a secondary;
  - every tiny start is farther than the radius from every primary and secondary start in its region and in the
    neighbouring regions;
  - no two starts of any tier share a chunk;
  - a region whose primary pick is reserved and whose secondary pick is reserved may still hold tinies.
- `tools/checkconfig.py` after `runData` if the secondary gets a `StructureConfig`; `tools/terrain.py` is not needed
  for structure placement.

## 11. Not in scope

- Renaming `FallbackStructure` or the `gensokyolegacy:fallback` type id.
- Per-tier radii: all three tiers use the set's primary `safety_radius`.

The primary now reads its own `firstPassInRegion` instead of rescanning `j < i` (implemented with this tier). The
verdict is observationally identical; the §10 `diagnoseRegion` diff over several seeds is the guard.

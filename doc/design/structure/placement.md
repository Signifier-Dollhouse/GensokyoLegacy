# Structure placement — multi-spread sets and the back-reference

How a gensokyo legacy structure reaches a start position, and why the placement values live in only one place.

Related: [home_index.md](home_index.md), [structure_bed_entity.md](structure_bed_entity.md).

## 1. The pair

| Class | File | Role |
|---|---|---|
| `MultiSpreadPlacement` | `content/worldgen/structure/MultiSpreadPlacement.java` | `minecraft:random_spread` sibling registered as `gensokyolegacy:multi_spread`. Yields `attempts` uniform candidates per `spacing x spacing` region from one salted random (`getPotentialChunks`), and hooks `getPotentialStructureChunk` / `isPlacementChunk` so vanilla `/locate` and `ChunkGeneratorStructureState` drive off the attempt-0 candidate. `separation` is unused and never written. `salt()` is widened to public for the structure holding it. |
| `FlatCheckStructure` | `content/worldgen/structure/FlatCheckStructure.java` | The structure itself. Decides, per candidate chunk, whether a start is placed: prefilter (sea/biome at the middle column) → margin safety against neighbouring regions → speculative jigsaw layout with biome/root-height/rigid/joint checks. Only the least-index passing candidate of a region survives. |
| `ChunkGeneratorMultiSpreadMixin` | `mixin/ChunkGeneratorMultiSpreadMixin.java` | Vanilla `getNearestGeneratedStructure` walks all `attempts` candidates instead of one, and pre-filters candidate chunks whose middle column samples no biome in the set's biome tag. |

Both sides must agree on the candidate grid, which is a function of
`(seed, chunkX, chunkZ, spacing, spreadType, salt, attempts)`.

## 2. One copy of the values: the set is the reference

The structure set is the only place those four numbers live:

```
data/<ns>/worldgen/structure_set/magical_forest_houses.json
  placement: {type: gensokyolegacy:multi_spread, spacing: 32, salt: 2026165872, attempts: 24}
  structures: [{structure: gensokyolegacy:marisa_house, weight: 1}, ...]
```

and the structure entry names that set instead of repeating the numbers:

```
data/<ns>/worldgen/structure/marisa_house.json
  type: gensokyolegacy:flat_check
  set: gensokyolegacy:magical_forest_houses
  set_index: 1, set_count: 4
```

`FlatCheckStructure#set` is a `Holder<StructureSet>` read from that `set` field, and `#placement` hands out its
`MultiSpreadPlacement`, which is the same object the vanilla placement code uses. Both sides therefore compute candidates
from one source, and no third party can make them disagree.

### Binding

A codec cannot resolve the holder: structures decode before any structure set does (structure-set entries reference
structures by holder, which is what fixes that order), so the set registry is empty while a structure loads. `StructureSet.CODEC`
therefore only gets a *reference* out of it. Consequences, all handled in `#placement`:

- the holder arrives unbound, and `#placement` swaps in the registry's own holder the first time worldgen reads it — the
  value is touched only in worldgen, so that is where it binds;
- a `set` naming no existing set (or a set not placed by `MultiSpreadPlacement`) simply leaves the structure placing nothing;
- datagen has no set registry either, so `StructBuilding#setRef` hands the structure a
  `dev.xkmc.l2core.init.reg.ench.DataGenHolder` carrying just the id (value null). It serialises as `"set": "<id>"` because
  that holder's `canSerializeIn` is always true, and datagen never reads the value.

A datapack reload rebuilds both registries and the structure instances with them, so the bound holder never goes stale.

## 3. Why the old copy was a bug factory

An earlier version stored `spacing` / `spread_type` / `salt` / `attempts` in the structure entry as well. Vanilla decodes
each registry entry independently, so the two copies could drift, and the symptom was silent: the placement asked the
structure about a chunk that was not one of the structure's own candidates, `findGenerationPoint` returned empty for
every chunk, and nothing generated anywhere.

**SparseStructures** (CurseForge project `911437`) did exactly that. Its `MakeStructuresSparse` mixin injects into
`RegistryDataLoader#loadElementFromResource` and, for every `worldgen/structure_set` entry that is not
`minecraft:concentric_rings`, rewrites the placement JSON:

- `spacing` (and `separation`, which the field adds when absent) multiplied by its `spreadFactor` — the default 2 turns
  spacing 32 into 64;
- `salt`, replaced by a hash of the structure set id when its `idBasedSalt` option is on (the default);
- `frequency: 0` for a set whose factor is 0, which disables it — that one is the mod working as intended.

With the back-reference there is nothing to fight: the placement and the structure read the same rewritten values, so our
structures follow the pack-wide factor (rarer, different positions) instead of vanishing. Existing chunks keep whatever they
had; newly generated ones use the new grid. A factor of 0 still disables the set, as SparseStructures intends.

Verified on the dev world (seed read from `level.dat`, candidates recomputed independently): with the mod absent the start
lands on the authored 32-chunk grid with salt 160465900; with it present the start lands on the rewritten 64-chunk grid with
salt 162382118 — its `abs(hash("gensokyolegacy:cirno_nest"))`.

## 4. Released-world rules

`set_index` / `set_count` decide which member of a set a region picks, and both live in the structure entry, so they still
must never change once released. Datagen writes them together with `set` (`init/data/structure/`), so a member's index and
its set's grid move in one step.
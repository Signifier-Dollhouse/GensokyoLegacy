# Fairy dust — the off-thread home search

Sprinkling fairy dust on a flower points at a character's home. The search is a ring-by-ring walk over the
structure grid, run on a background thread while a mote circles the flower, so the tick loop never waits for it.

Related: [placement.md](placement.md), [cooldown.md](../item/cooldown.md).

## 1. The cast

`FairyDustItem#useOn` resolves the tag from the flower, consumes one dust (creative excepted) and hands the cast to
`FairyDustSearch#start`:

| flower | tag |
|---|---|
| pink petals, poppy | `hakurei_shrine` |
| dandelion, oxeye daisy | `marisa_house` |
| azure bluet | `myouren_temple` |
| cornflower | `morichika_shop` |
| any tulip | `alice_house` |
| any other flower | `character_homes` (all six homes) |

`start` resolves the tag on the server thread, keeps only `LocatableStructure` members that a `multi_spread` set places
(primaries and secondaries, never the tiny tier), and schedules the ticker with
`SchedulerHandler.schedulePersistent`. `FairyDustSearch#isBusy` keeps one cast per
player in flight; a cast left behind by a stopped server is pruned on the next use.

## 2. The ticker

`tick` runs once per server tick until it returns `true`:

1. one dust mote on a circle around the flower, 30 degrees per tick at radius 0.6;
2. poll the pending ring future — `isDone` then `join`, so the tick never blocks;
3. a hit casts the ray and ends; an empty last ring scatters and refunds; otherwise submit the next ring.

Only one ring is in flight at a time, so a long search is a sequence of short background tasks rather than a burst.

## 3. Why the search can leave the tick thread

`LocatableStructure#locateRegion` is the same pure check worldgen workers already run:
`ChunkStatusTasks.generateStructureStarts` calls `ChunkGenerator#createStructures` on the worker pool, which calls
`Structure#findGenerationPoint`. `locateRegion` walks a region's candidate stream in order and returns the least-index
candidate passing `checkCandidate`, exactly as `findGenerationPoint` does, and reads no chunk and no stored start.

Everything it touches is immutable or per-call, captured before the worker starts:

| input | state |
|---|---|
| `RegistryAccess` | frozen snapshot |
| `ChunkGenerator` / `BiomeSource` / `RandomState` | stateless density trees |
| `StructureTemplateManager` | `ConcurrentMap` + `computeIfAbsent` |
| `LevelHeightAccessor` | a `LevelBounds` record of the two bounds, not the `ServerLevel` |
| `LocatableStructure` | all fields final; `TerrainPredicate` is a record |
| candidate random | a fresh `WorldgenRandom` per candidate |

The dust does not use `ServerLevel#findNearestMapStructure`: that path reads `StructureCheck`'s plain maps and the
unsynchronized `RegionFileStorage` cache, and its `level.getChunk(STRUCTURE_STARTS)` bounces to the main thread and
joins. `/locate` needs the stored start; a particle ray does not.

## 4. Rings

Ring `n` is the perimeter of the `(2n+1)^2` region square of the structure set's own spacing, so a tag whose members
share a spacing walks outward one cell at a time. `MAX_RING = 8` is about 4 km on the 32-chunk grid and a home is
normally in ring 1-3; the first ring with a hit wins, which is the first ring outward, not necessarily the globally
nearest start.

## 5. Main-thread budget

Per tick the server does one `sendParticles` and one future poll. All region checks run on the single
`gensokyo-fairy-dust` thread, which is not `Util.backgroundExecutor()`: the worldgen pool must not be starved by a
search. Casts from different players queue on that thread instead of competing with chunk generation.

## 6. Region cache

`LocatableStructure#locateRegion` is memoised by `RegionMemo` per `(structure, generator, seed, region)`,
misses included, so a second cast over the same ground only pays for regions neither cast has seen. The cache is
shared with `/gensokyo structure scan` (see [placement.md](placement.md) §5), bounded, and
cleared on server stop and on datapack reload.

## 7. Failure and refund

When the last ring comes back empty the mote scatters and the consumed stack is dropped at the flower with the default
pickup delay. Creative casts consume nothing and refund nothing. On server stop the ticker exits immediately and the
worker stops at the next region boundary.

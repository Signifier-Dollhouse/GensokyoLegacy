# Overworld region (TerraBlender)

`GLOverworldRegion` is the only TerraBlender region in the mod. It places `sakura_forest`,
`magical_forest` and `myouren_temple_barrens` in the overworld. This note records why the class also
emits a `DEFERRED_PLACEHOLDER` point, because that is not obvious from TerraBlender's API.

## How TerraBlender 4.1 places biomes

The region's parameter points are **not** merged into the vanilla `MultiNoiseBiomeSource` list. Instead:

1. `MixinParameterList.initializeForTerraBlender` builds one `Climate.RTree` per registered region.
   Index 0 is the vanilla overworld tree (`DefaultOverworldRegion`).
2. A `uniqueness` noise (seeded, `overworldRegionSize` zooms) picks a region index for every x/z column
   from the registered weights. The choice does not look at climate, height or land/water.
3. `MixinMultiNoiseBiomeSource.getNoiseBiome` calls `findValuePositional`, which searches **only the tree
   of the region that owns the column**, using `Climate.RTree.Node::distance` — sum of squared distances
   to each axis interval, with no "inside the box" test.
4. If the winner is `terrablender:deferred_placeholder`, the search is repeated in the vanilla tree.

Consequence: within a region cell, a column that matches none of the region's points gets the nearest
declared point. Ocean is outside the inland continentalness ranges, so an ocean column in our region
would become our biome. The region cannot be restricted to land: the uniqueness layer only knows x/z.

## Why `VanillaParameterOverlayBuilder` is not enough

`VanillaParameterOverlayBuilder.build()` appends placeholder points for the complement of the declared
ranges — but `inversePoints` takes the complement of **every** axis and crosses them. A placeholder
therefore requires the column to be outside the declared ranges on *all six* axes at once. An ocean
column is only outside continentalness (temperature and humidity are still neutral), so one of our real
points is closer and wins. `Region.addBiomeSimilar` has the same gap: copied vanilla points still
contain no ocean points.

## The rule used here

`addBiomes` emits the declared points and then one `DEFERRED_PLACEHOLDER` that spans the whole parameter
space with `offset = FALLBACK_OFFSET`. The `VanillaParameterOverlayBuilder` stays only as the collector;
its own partial placeholders are subsumed by this one. `Climate.ParameterPoint.fitness` adds
`offset^2` and the target's offset axis is always 0 (`TargetPoint.toParameterArray`), so this leaf has
the constant fitness `0.01^2 = 100` against a real point's `d^2`, where `d` is the 6-D interval
distance to the nearest declared box:

- `d < FALLBACK_OFFSET` → our biome (this includes everything inside the boxes, where `d = 0`);
- `d > FALLBACK_OFFSET` → placeholder → vanilla.

`offset = 0` cannot be used: the placeholder would tie every declared point at `d = 0` and win outside,
and `RTree` tie-breaking is traversal-order dependent, so the biomes would flicker or vanish entirely.
A negative offset would win inside the boxes too. Only the constant matters, so the guarantee is exact:
our biome covers the declared boxes plus a 6-D shell of radius `FALLBACK_OFFSET`, and nothing beyond.

### Choosing the offset

Measured against the ranges in `GLOverworldRegion`:

| undeclared band | width of the gap from the declared hull | shell share at 0.01 |
|---|---|---|
| coast (continentalness) | 0.08 | 12.5% |
| hot (temperature) | 0.45 | 2.2% |
| peaks (weirdness `PEAK_NORMAL`) | 0.20 | 5.0% |
| high erosion | 0.95 | 1.1% |

The coast is the narrowest, so `FALLBACK_OFFSET` must stay below 0.08 to never reach the ocean; 0.01
eats 12.5% of the coast band and less of everything else. The shell is a 6-D ball, not a per-axis strip,
so a column far outside on one axis is never rescued because it is close on another.

Depth is declared `span(-1.0, 0.3)`. Depth reads 0 at the column surface and one unit per 128 blocks,
so this covers the air above it and about 38 blocks below, and the surface band has depth distance 0
instead of competing with the placeholder. Below 0.3 the depth distance alone exceeds `FALLBACK_OFFSET`,
so caves fall to the placeholder and stay vanilla; the 0.01 shell shifts that cut to about 0.31.

`GLModConfig.COMMON.regionWeight` is the weight of this region in the uniqueness layer; it controls how
many region cells the biome gets, not where inside a cell it may spawn.

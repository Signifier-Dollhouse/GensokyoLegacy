# Gensokyo biomes (`init/data/biome/`)

The twelve biomes of `gensokyo_dimension_biome_guide.md` §3 and §4, and the two that were already there
(`magical_forest`, `sakura_forest`). Registry keys live in `GLBiomes`, features in `GensokyoBiomeFeatures`, and the
places they are selected from in `GensokyoDimension`'s climate grid.

The guide's own Y bands (64–80 / 80–96 / 96–128) are **not** what the grid uses. `gensokyo_dimension.md` §3 replaced
them with continentalness quantiles, so the guide's "lower ground / upper ground / Mountain" labels are kept here as
*band names* and mapped onto the height bands that actually exist:

| guide band | grid region | surface y | share |
|---|---|---|---|
| lower ground | region 2 | 66 … 80 | 25.7% |
| upper ground | region 3 | 80 … 96 | 24.3% |
| Mountain | region 4 | 96 … 128 | 20.2% |
| Underground | `depth = 1.0` row | — | — |

The guide gives four biomes per band and classifies them **hot humid, cold humid, hot dry, cold dry**, in that order.
So does the grid: temperature and humidity are each cut once, at 0, and the four cells of a band are filled in the
guide's order. Nothing is repeated and nothing is left over.

| band | hot humid | cold humid | hot dry | cold dry |
|---|---|---|---|---|
| lower ground | sunflower_plains | muenzuka | human_village | myouren_temple_barrens |
| upper ground | magical_forest | bamboo_forest_of_the_lost | sakura_forest | hill_of_the_nameless |
| Mountain | untrodden_valley | genbu_ravine | youkai_mountain_foothills | sanctuary |

Each biome's own `temperature` and `downfall` follow its cell, so the sky colour and the snow line agree with the corner
of the plane the grid picked it from. The one exception is `myouren_temple_barrens`: the cold dry corner, cold enough
to sit under the snow line, and snow over its coarse dirt would be wrong — so it turns precipitation off instead.

`ocean`, `beach`, `grove` and `peak` are deliberately untouched — they are still vanilla copies, and a follow-up guide
covers them.

---

## 1. The construction rule: a vanilla base plus a tail

No new biome is built from scratch. Each one copies the features of a vanilla biome that already has the right terrain,
carvers, ores and decoration, then appends a short custom tail. `GLBiomes.touhou` and `GLBiomes.generationOf` do
that; `GLBiomes.copyOf` is the same machinery for a biome that keeps vanilla's climate too.

Two consequences, both load-bearing:

- **The base must be read through `VanillaRegistries.createLookup()`, not reused.** A holder owned by another provider
  fails `canSerializeIn` at write time.
- **The tail goes strictly after every vanilla feature, never between two of them.** See §3.

The base only ever supplies *features*. Climate, colours and spawns are spelled out per biome, because a copy of
`minecraft:forest` that keeps `minecraft:forest`'s sky colour is not a Muenzuka.

| biome | base | why that base |
|---|---|---|
| `muenzuka` | `forest` | deciduous canopy, dark forest floor to break up with podzol |
| `human_village` | `plains` | open, already tilled-looking, plus its pumpkin and sugar cane patches |
| `myouren_temple_barrens` | `badlands` | the dry dead-bush and coarse-dirt look, and the quiet spawn weighting |
| `bamboo_forest_of_the_lost` | `snowy_taiga` | snowy spruce understorey; Eientei's grove really is a winter one |
| `hill_of_the_nameless` | `meadow` | open flowered hill with no canopy to hide in |
| `untrodden_valley` | `taiga` | conifers, ferns, already cold |
| `genbu_ravine` | `swamp` | the clay disk and reeds read as wet banks, and the lily pads work underwater |
| `youkai_mountain_foothills` | `windswept_hills` | sparse oak on gravelly ground |
| `sanctuary` | `meadow` | meadow's own sparse birch reads as a deliberate grove |
| `hell_of_blood_pools` | `swamp` | reeds and lily pads are what a pool basin wants |
| `animal_realm` | `lush_caves` | dense overgrowth and dripping water already cave-shaped |
| `hell_of_blazing_fires` | `badlands` | scorched, dry, and already carries lava lakes |
| `former_hell` | `dripstone_caves` | plain stone and dripstone, so the masonry has something to sit on |

`sunflower_plains` stays the copy it was — the guide names it, and it is the hot humid corner of the lower ground.
There is no snowy biome: the guide gives a band its four, and the one genuinely cold cell of the whole dimension is
`bamboo_forest_of_the_lost`, which is snow-covered anyway.

---

## 2. The feature tail

`GensokyoBiomeFeatures` owns 20 configured features and 23 placed ones under
`worldgen/{configured,placed}_feature/gensokyo_biome/`. Three kinds:

**Ground cover** — `RANDOM_PATCH` of `SIMPLE_BLOCK` on `BlockTags.DIRT`, the same shape as the magical forest's grass
and flowers. The vanilla flower and grass features of the base are kept as well, so the mod blocks are an accent
rather than a replacement.

**Soil disks** — `Feature.DISK`, in the `UNDERGROUND_ORES` step. Podzol under Muenzuka's trees, coarse dirt through the
barrens, mud under the bamboo and in Genbu Ravine.

**Cave deposits** — the same patch, but placed with vanilla's own cave decoration pipeline
(`EnvironmentScanPlacement` downward + `RandomOffsetPlacement`) in the `UNDERGROUND_DECORATION` step, and gated on
`BlockPredicate.solid()` instead of the dirt tag so they survive a hell palette that is not vanilla stone.

| placed feature | tries / count | used by |
|---|---|---|
| `stones` | 2 patches, mossy cobble / cobble / stone brick | hill, barrens, valley, foothills, ravine |
| `grave_marker` | 1 in 4 chunks | muenzuka |
| `ghost_flowers` | 3 patches | muenzuka |
| `village_flowers` | 4 patches | human village |
| `barrens_scrub` | 3 patches | barrens |
| `hill_of_the_nameless_flowers` | 4 patches | hill of the nameless |
| `bamboo_dense` / `bamboo_sparse` | 6 patches / 1 in 3 | bamboo forest of the lost |
| `ravine_moss` | 6 patches | genbu ravine |
| `foothill_moss` | 4 patches | youkai mountain foothills |
| `sanctuary_trees` / `sanctuary_flowers` | 1 in 5 chunks / 3 patches | sanctuary |
| `blood_deposit` / `bone_pile` | 3 patches / 1 in 6 | hell of blood pools |
| `den_rubble` / `fungus_patch` | 1 in 5 / 4 patches | animal realm |
| `fire_vent` / `ash_patch` | 1 in 4 / 5 patches | hell of blazing fires |
| `old_rubble` | 1 in 4 | former hell |

The guide's §7 order — surface palette, common vegetation, ground accents, water, structures, atmosphere — was not
followed literally, because a biome's identity here comes mostly from its base and its colours. Every list in the
guide is a suggestion of what *can* be there, and the palette table below is what was taken from it.

### Deliberately not done

- **No structures.** Every structure in the mod is either a character home (magical forest, sakura forest) or Cirno's
  nest. A village biome with no houses in it is a gap, not a bug, but filling it is a separate job.
- **No custom fluids.** The guide's blood pools and lava chapter both say to use a real red fluid or to use a clearly
  recognisable block palette. `hell_of_blood_pools` takes the second option: a red palette of netherrack, crimson
  nylium and redstone ore, and the pools stay water.
- **No ambient particles or music.** `GLLang` and `BiomeSpecialEffects` both support them; no sound event for any of
  these locations exists yet.

---

## 3. Feature order

`FeatureSorter` links biomes by the order placed features appear in them, and a cycle between two biomes crashes chunk
generation **at runtime**, not at datagen — `tools/feature_cycle_check.py` is the only thing standing between a typo
and a world that will not load.

The rule that makes it impossible to get wrong: every biome's tail is a subsequence of one fixed list,
`GensokyoBiomeFeatures.ORDER`. Two biomes sharing a custom feature therefore always place it in the same relative
order, and because the tail is appended after the whole vanilla chain, no custom feature can ever land between two
vanilla ones. `GensokyoBiomeFeatures.tail` checks the subsequence at class-init time and throws if a tail is out of
order, so the mistake is a datagen crash rather than a world that loads for an hour first.

After any change here:

```
./gradlew runData
python3 tools/feature_cycle_check.py build/moddev/artifacts/neoforge-21.1.238-client-extra-aka-minecraft-resources.jar src/generated/resources
python3 tools/terrain.py gensokyolegacy:gensokyo
```

---

## 4. Surface rules

`GensokyoDimension.surfaceRules` is `SurfaceRuleData.overworld()` with four biome branches in front of it, one per
underground biome, and nothing else changed. This is the guide's §6 split: shared material stays dimension-wide, and a
biome branch is only for a palette that genuinely differs.

| biome | floor | 3 blocks under it | the rest of the column |
|---|---|---|---|
| hell of blood pools | netherrack | crimson nylium | redstone ore |
| animal realm | packed mud | dirt | deepslate |
| hell of blazing fires | blackstone | magma block | basalt |
| former hell | stone bricks | cracked stone bricks | polished deepslate |

Three things make this safe:

- **The underground rows only win about 128 blocks down** (`gensokyo_dimension.md` §4), so no surface position is ever
  one of these biomes and the branches claim nothing above the overworld's grass, dirt and underwater gravel.
- **Each branch is complete.** A branch that returned one state for a matched position would swallow everything the
  overworld tree would have put below it, deepslate included. Three rules cover floor, near-floor and body, so nothing
  falls through.
- **The near-floor rule is a widened `stoneDepthCheck(3, false, FLOOR)`,** not `UNDER_FLOOR`. `UNDER_FLOOR` adds
  `surfaceDepth`, which is the aquifer depth and is zero almost everywhere underground, so it only ever reaches one
  block down. `stoneDepthAbove` is the distance to the nearest floor above, so an offset of 3 is what buys a deposit.

The surface biomes get no branch of their own. Their palettes are grass and dirt, which is what the overworld tree
already says everywhere else; the dry and wet ones are done with the coarse dirt and mud disks from §2 instead, which
also keeps them patchy rather than uniform. `genbu_ravine` is the one that would want a real mud bank here; the disk is
the cheaper half of that and the surface rule is the part worth revisiting when a ravine has actual ravine terrain.

---

## 5. Climate grid

Every cell of `GensokyoDimension.SURFACE` that used to be a vanilla copy now names a guide biome. `REGION_EDGES`, the
offset spline and every height share are untouched, so `terrain.py` still reports the same bands — only the labels
changed. Measured on 256 × 256 chunks at seed 0:

```
grove                     12.34%      peak                      5.49%
ocean                     10.84%      magical_forest            5.45%
myouren_temple_barrens     7.11%      genbu_ravine              5.31%
muenzuka                   7.05%      sanctuary                 5.17%
human_village              6.38%      beach                     4.75%
bamboo_forest_of_the_lost  6.25%      sakura_forest             4.64%
hill_of_the_nameless       6.10%      untrodden_valley          3.74%
sunflower_plains           5.94%      youkai_mountain_foothills 3.44%
```

Each of the three open-ground bands splits into four near-equal quarters, which is what cutting both climate axes once
gives: the lower ground band is 25.73% of the map and its four cells are 5.94 / 7.05 / 6.38 / 7.11%.

The four underground biomes never appear on this map: `terrain.py` samples one column per chunk at the surface, and the
underground rows only win 128 blocks down.

`UNDERGROUND` dropped its temperature split entirely. It used to be snowy when cold and magical forest otherwise, which
made every cave in the dimension the same biome. The guide §4 gives the underground no climate classification at all —
just four places — so it is one cell per region, keyed on how far the surface above it sits: blood under the water
table and the coast, the animal realm under the lower ground, the old hell under the upper ground, and the burning hell
under the mountain and above it. The higher the ground, the deeper the heat.

---

## 6. Tags

`GLStructureTagGen.genBiomeTag` gains `is_plains`, `is_mountain` (repointed from the removed `mountain` to the four
biomes that replaced it), `is_forest`, `is_taiga`, `is_underground`, `is_spooky`, `is_floral`, `is_wet` and `is_dry`.

## 7. Names

`GLLang.Biome` carries the English display names; the zh_cn strings are hand-authored in
`lang/zh_cn/world.json` and merged by `organize.ResourceOrganizer`. The lang key is the vanilla
`biome.gensokyolegacy.<id>` one rather than the usual `gensokyolegacy.*` scheme, because that is the key the F3 biome
readout and the locator item look up.

`ocean`, `beach`, `grove` and `peak` have no display name yet, for the same reason they have no guide: they fall back
to the raw registry id.
# Gensokyo biomes (`init/data/biome/`)

Every biome the Gensokyo dimension can select: the twelve of `gensokyo_dimension_biome_guide.md` §3 and §4, the two that
were already there (`magical_forest`, `sakura_forest`), and the four fixed locations of
`gensokyolegacy_fixed_biomes_guide.md`. Registry keys live in `GLBiomes`, features in `GensokyoBiomeFeatures`, and the
places they are selected from in `GensokyoDimension`'s climate grid.

The sixteen split three ways:

- **twelve band biomes** (§1) — the guide's lower ground / upper ground / Mountain / Underground, one vanilla base each.
- **two established** — `magical_forest` and `sakura_forest`, which predate this and are left alone.
- **four fixed locations** (§7) — Misty Lake, its shore, the Tengu Settlement and the summit above it.

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

Each biome's own `temperature` and `downfall` follow its cell, so the sky colour and the hot/cold split read off the
numbers the same way they read off the map. None of them is actually cold, though: see §6. The hot cells of each band
are lifted a little to keep the split legible now that the cold cells are floored. `myouren_temple_barrens` is the one
biome with precipitation off, because a dry barrens should not rain.

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

What a base is *not* allowed to keep is listed in `gensokyo_dimension.md` §5: the canyon carver, the amethyst geode and
the two monster rooms go from every biome, and `misty_lake` loses the water trees besides.

| biome | base | why that base |
|---|---|---|
| `muenzuka` | `forest` | deciduous canopy, dark forest floor to break up with podzol |
| `human_village` | `plains` | open, already tilled-looking, plus its pumpkin and sugar cane patches |
| `myouren_temple_barrens` | `badlands` | the dry dead-bush and coarse-dirt look, and the quiet spawn weighting |
| `bamboo_forest_of_the_lost` | `snowy_taiga` | spruce understorey, so the grove reads as a winter one — though the biome itself no longer snows, §6 |
| `hill_of_the_nameless` | `meadow` | open flowered hill with no canopy to hide in |
| `untrodden_valley` | `taiga` | conifers, ferns, already cold |
| `genbu_ravine` | `swamp` | the clay disk and reeds read as wet banks, and the lily pads work underwater |
| `youkai_mountain_foothills` | `windswept_hills` | sparse oak on gravelly ground |
| `sanctuary` | `meadow` | meadow's own sparse birch reads as a deliberate grove |
| `hell_of_blood_pools` | `swamp` | reeds and lily pads are what a pool basin wants |
| `animal_realm` | `lush_caves` | dense overgrowth and dripping water already cave-shaped |
| `hell_of_blazing_fires` | `badlands` | scorched, dry, and already carries lava lakes |
| `former_hell` | `dripstone_caves` | plain stone and dripstone, so the masonry has something to sit on |

The four fixed locations have their own bases and their own section, §7.

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
| `lake_clay` / `lake_cattails` / `misty_lake_lily_pads` | 2 patches / 1 in 3 / 1 in 3 | misty lake |
| `shore_sand` / `shore_reeds` / `shore_flowers` | 4 / 3 / 3 patches | misty lake shore |
| `settlement_paths` | 1 in 4 | tengu settlement |
| `summit_soil` / `summit_moss` / `summit_scree` / `summit_trees` | 2 / 4 / 4 patches / 1 in 16 | youkai mountain summit |

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

`ORDER` only constrains features this project owns. A **vanilla placed feature cannot go in a tail**, because it
already has a fixed position in vanilla's own chains: appending `minecraft:patch_waterlily` to one biome's tail put it
after `minecraft:seagrass_river`, while `minecraft:swamp`'s own chain has it before
`minecraft:brown_mushroom_swamp`, and the two together cycle. Reuse the vanilla *configured* feature behind a placed
feature of our own instead — that is what `misty_lake_lily_pads` is.

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

The four fixed locations do have branches, because they are the biomes whose *material* is the point. They are the
opposite shape from the hell palettes above, and the difference is the whole trick:

| | hell palettes | fixed-location branches |
|---|---|---|
| keyed on | the biome alone | biome **plus** floor, water level and slope |
| claims | the whole column | only the positions it names |
| unmatched positions | impossible — would swallow the deepslate | fall through to the next entry, to the overworld tree |

Because they only filter, they are three separate entries in the sequence rather than one branch per biome, and none of
them can shadow the dimension's grass and dirt. In the guide's §6 priority order:

1. **lake bed** (`misty_lake` + `misty_lake_shore`) — sand on the underwater floor, stone where the bank is steep,
   gravel through the two blocks under it. Shared across both biomes so the bed palette is continuous where water
   crosses the boundary, which is the guide's own instruction.
2. **summit** (`youkai_mountain_summit`) — stone where it is steep, gravel where `summit_surface` noise is low, stone
   everywhere else. The soil pockets are *not* a third state here; they are the grass disk in the tail, which is the
   cheaper place for them.
3. **shore waterline** (`misty_lake_shore`) — sand at the water line, gravel under.
4. the four hell palettes.
5. `SurfaceRuleData.overworld()`.

`tengu_settlement` has no branch at all, which is also the guide's instruction: its paths are
`settlement_paths`, a placed feature, because a surface rule would paint the whole biome floor. Its topsoil is the
dimension's.

Two honest limitations. `UNDERWATER` is `waterBlockCheck(0, 0)`, i.e. `blockY <= waterHeight`, which is a *level*
test rather than an "is there water here" test — a cave floor that happens to sit under the water line in the lake will
also get sand, which is the same trade vanilla's own ocean makes. And `UNDER_BED` is a widened
`stoneDepthCheck(2, false, FLOOR)` rather than `UNDER_FLOOR`, for the reason given in `hellPalette` above.

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

## 6. Snow and freezing

Nothing in this dimension freezes on the strength of the climate grid. The grid picks *which biome* a column is; the
snow line is a property of that biome's own `temperature` field, and the two are deliberately decoupled — `muenzuka`
is the cold humid cell of the lower ground and does not snow, while `human_village` is the hot dry cell and could have
been anything.

The chain, all of it vanilla:

1. Every base biome carries `minecraft:freeze_top_layer` (from `globalOverworldGeneration` → `addSurfaceFreezing`).
   `SnowAndFreezeFeature` asks the biome twice, once for the block under the surface and once for the surface itself.
2. `Biome#shouldSnow` / `shouldFreeze` both bail on `warmEnoughToRain`, which is `getTemperature(pos) >= 0.15F`.
3. `getTemperature` is the declared temperature through `TemperatureModifier.NONE`, then
   `getHeightAdjustedTemperature`, which above y 80 subtracts `(noise * 8 + y - 80) * 0.00125` — up to 0.07 at the
   top of the mountain band and 0.19 on a y 220 peak.

So the threshold is **0.15 of an altitude-adjusted temperature**, not of the declared one, and that is the whole story:

| biome | declared | band y | worst altitude loss | effective | snows? |
|---|---|---|---|---|---|
| myouren_temple_barrens | 0.2 | 66 … 80 | 0 (below y 80) | 0.2 | no |
| muenzuka | 0.3 | 66 … 80 | 0 | 0.3 | no |
| human_village | 0.8 | 66 … 80 | 0 | 0.8 | no |
| sunflower_plains | 0.8 (copy) | 66 … 80 | 0 | 0.8 | no |
| magical_forest | 0.7 | 80 … 96 | 0.02 | 0.68 | no |
| sakura_forest | 0.5 | 80 … 96 | 0.02 | 0.48 | no |
| bamboo_forest_of_the_lost | 0.3 | 80 … 96 | 0.02 | 0.28 | no |
| hill_of_the_nameless | 0.3 | 80 … 96 | 0.02 | 0.28 | no |
| untrodden_valley | 0.5 | 96 … 128 | 0.07 | 0.43 | no |
| genbu_ravine | 0.35 | 96 … 128 | 0.07 | 0.28 | no |
| youkai_mountain_foothills | 0.6 | 96 … 128 | 0.07 | 0.53 | no |
| sanctuary | 0.35 | 96 … 128 | 0.07 | 0.28 | no |
| grove | **−0.2** (copy) | 128 … 192 | 0.11 | −0.31 | **yes, vanilla's own** |
| tengu_settlement | 0.4 | 128 … 192 | 0.11 | 0.29 | no |
| youkai_mountain_summit | 0.4 | 192 … 220 | 0.19 | 0.21 | no, but only just |
| misty_lake | 0.3 | ≤ 63 | 0 (below y 80) | 0.3 | no |
| misty_lake_shore | 0.4 | 63 … 66 | 0 | 0.4 | no |

Every biome this project defines clears the line, though `youkai_mountain_summit` clears it by only 0.06 at the very top
of its band — raise its declared temperature before assuming anything else is safe. The margin is what the column is
for: the altitude loss is a noise term, so a biome only ever snows in the top few blocks of its band and only in the
coldest few of those. The six biomes the grid puts in a cold cell declare 0.2 to 0.35, comfortably warm, because a cold *cell* is a
place on the climate map and not a licence to freeze — `bamboo_forest_of_the_lost` is the clearest case, where the guide
sits a winter bamboo grove in a cold corner and the map should not put snow on the ground.

**Nothing in Gensokyo snows.** `grove` used to: it was a verbatim vanilla copy at −0.2, and it was the one biome the
snow line caught. `gensokyolegacy_fixed_biomes_guide.md` replaced it with `youkai_mountain_summit`, which is declared
0.4, so the exception is gone with it. `sunflower_plains` is the only vanilla copy left, and it is at 0.8.

Two other routes to ice were checked and do not fire here. `SurfaceRules.temperature()` reads the *climate sample*
rather than a biome, and `SurfaceRuleData.overworld()` only uses it inside the frozen-ocean branch — there is no
frozen ocean in Gensokyo. `TemperatureModifier.FROZEN` is likewise only on frozen oceans, and nothing this project
registers uses it.

---

## 7. The four fixed locations

`gensokyolegacy_fixed_biomes_guide.md` names Misty Lake, Misty Lake Shore, the Tengu Settlement and the Youkai Mountain
Summit. They are not a fifth through eighth band: each one takes **the whole plane of a single height band**, so what
selects them is continentalness and nothing else. That is also why they slot into the four slots the vanilla copies used
to hold, one for one, and why the grid needed no new machinery.

| band | surface y | share | was | now |
|---|---|---|---|---|
| region 0 | ≤ 63 | 10.84% | `ocean` (copy) | **misty_lake** |
| region 1 | 63 … 66 | 4.74% | `beach` (copy) | **misty_lake_shore** |
| region 5 | 128 … 192 | 12.34% | `grove` (copy) | **tengu_settlement** |
| region 6 | 192 … 220 | 5.49% | `peak` (two-source copy) | **youkai_mountain_summit** |

The one thing the grid had to carry over is the waterline invariant of §3. `ocean` was the drowned set only
because its continentalness edge sat on the waterline of the offset spline; `misty_lake` now sits in exactly that cell
and inherits the invariant unchanged, so the lake is still precisely the set of drowned columns. `tools/terrain.py`
confirms it: `misty_lake` measures 10.84% against 10.55% under water, the same near-match `ocean` had.

| biome | base | why |
|---|---|---|
| `misty_lake` | `river`, **minus `trees_water`** | a freshwater lake. `minecraft:ocean` was the obvious base and is wrong: both it and `river` carry water trees, and mangroves in a freshwater lake are exactly the swamp the guide rules out |
| `misty_lake_shore` | `beach` | already the sand-and-gravel shoreline; the guide's waterline and coves are added on top |
| `tengu_settlement` | `forest` | a wooded settlement edge, and distinct from `youkai_mountain_foothills`, which is `windswept_hills` |
| `youkai_mountain_summit` | `stony_peaks` | literally the exposed-rock biome, and its emerald and amethyst veins are the guide's "soil pockets" |

The water itself comes from terrain and aquifers, not from features, as the guide requires — and in Gensokyo that is
already true, because region 0 *is* the drowned set. There is no lake feature to write.

### Climate

All four follow the same rule as §6: nothing here freezes. The summit is the awkward one, since it is the highest
ground in the dimension and y 192 … 220 costs up to 0.19 of altitude-adjusted temperature, so it declares 0.4 rather
than something that would read as alpine. Its altitude is carried by the palette — stone, scree, no trees, sparse
moss — and not by its temperature field, which is only a visual dial. The guide makes snow conditional on "if this
biome should be cold enough", so this is the branch where that question was answered.

### Tails

| biome | tail |
|---|---|
| `misty_lake` | clay and gravel patches on the bed, cattails in the coves, sparse lily pads |
| `misty_lake_shore` | boulders, a sand/gravel/mud waterline, reeds at the edge, ferns and flowers inland |
| `tengu_settlement` | ferns and grass on the unused slopes, gravel and coarse dirt paths |
| `youkai_mountain_summit` | soil pockets (a grass disk on stone), moss and glow lichen, scree, a rare pine |

Two of those needed a ground predicate that is not `BlockTags.DIRT`, because the lake's surface rule makes the bed sand
and gravel: `onGround(tries, palette, blocks...)` is the general form, and `onDirt` is the `BlockTags.DIRT` shorthand.

### Not done

The guide asks for the Scarlet Devil Mansion and the tengu buildings as structures, with terrain validity and
spacing. No such templates exist — the mod's only structures are the character homes and Cirno's nest — so both
settlements land as the natural ground they sit on, exactly as `human_village` does. That is a separate job with a
separate template budget, and the guide is explicit that these must not be faked with random placed features.

---

## 8. Tags

`GLStructureTagGen.genBiomeTag` gains `is_plains`, `is_mountain` (repointed from the removed `mountain` to the four
biomes that replaced it), `is_forest`, `is_taiga`, `is_underground`, `is_spooky`, `is_floral`, `is_wet`, `is_dry`,
`is_aquatic`, `is_mountain/peak`, plus `is_wet` for the two lake biomes.

`misty_lake` is deliberately **not** in `is_ocean`. It is a freshwater lake, and anything that branches on that tag
would bring the oceans' own overrides with it.

## 9. Names

`GLLang.Biome` carries the English display names; the zh_cn strings are hand-authored in
`lang/zh_cn/world.json` and merged by `organize.ResourceOrganizer`. The lang key is the vanilla
`biome.gensokyolegacy.<id>` one rather than the usual `gensokyolegacy.*` scheme, because that is the key the F3 biome
readout and the locator item look up.

Every one of the seventeen has a display name. `sunflower_plains` keeps a name that reads like the vanilla biome it
copies, which is deliberate: it is the same grass.
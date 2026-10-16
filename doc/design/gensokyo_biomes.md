# Gensokyo biomes (`init/data/biome/`)

Every biome the Gensokyo dimension can select: the twelve of `gensokyo_dimension_biome_guide.md` §3 and §4, the two that
were already there (`magical_forest`, `sakura_forest`), and the four fixed locations of
`gensokyolegacy_fixed_biomes_guide.md` plus the one it does not name. Registry keys live in `GLBiomes`, features in
`GensokyoBiomeFeatures`, and the places they are selected from in `GensokyoDimension`'s climate grid.

The seventeen split four ways:

- **twelve band biomes** (§1) — the guide's lower ground / upper ground / Mountain / Underground, each with its own written-out feature list.
- **two established** — `magical_forest` and `sakura_forest`, which predate this and are left alone.
- **four fixed locations** (§7) — Misty Lake, its shore, the Tengu Settlement and the summit above it.
- **one split** — `secret_heaven_cliff`, the dry half of the settlement's band (§7).

The guide's own Y bands (64–80 / 80–96 / 96–128) are **not** what the grid uses. `gensokyo_dimension.md` §3 replaced
them with continentalness quantiles, so the guide's "lower ground / upper ground / Mountain" labels are kept here as
*band names* and mapped onto the height bands that actually exist:

| guide band | grid region | surface y | share |
|---|---|---|---|
| lower ground | region 2, low erosion half | 66 … 96 | 28.0% |
| upper ground | region 2, high erosion half | 66 … 96 | 19.9% |
| Mountain | region 3 | 96 … 128 | 19.9% |
| Underground | `depth = 0.53` row | — | — |

The lower ground and the upper ground **share one region**: the grid merges them into the y 66 … 96 plain and splits
them on erosion instead of on height, so each of the eight appears at every height in the band. The shares are over
512 × 512 chunks at seed −1838080655014084215, and both halves move with the seed for the reason
`gensokyo_dimension.md` §4 gives.

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

## 1. The construction rule: a written-out list plus a tail

No biome is built from scratch, but none of them is copied from a vanilla biome any more either. Each one **writes out
its own features, step by step**, in the call that registers it, and takes a short custom tail at the end of the step
it belongs to. `GLBiomes.Layers` is the whole mechanism; the shared runs it draws on (`OVERWORLD_ORES`, `PLAINS_GROUND`,
`NETHER_COMMON`, …) are named by what they are.

This replaced a vanilla base per biome, and the reason was not tidiness. Copying `minecraft:deep_dark` wholesale is how
`mountain_interior` ended up growing `sculk_vein` and `sculk_patch_deep_dark`: nobody asked for sculk, it arrived
because the base was the nearest thing with the right weight of quiet rock, and there was no line in the source that
could be edited to remove it. The only way to see a biome's features was to know which vanilla biome it was standing
on.

Two consequences, both load-bearing:

- **A vanilla name in a comment is a reason, not a source.** Where a list was transcribed from a vanilla biome, the
  biome is named so a reader can go and check what was taken and what was left.
- **The tail goes strictly after every vanilla feature, never between two of them.** See §3.

Climate, colours and features are all spelled out per biome, because a copy of `minecraft:forest` that keeps
`minecraft:forest`'s sky colour is not a Muenzuka. **Spawns are empty in every biome**: natural spawning in Gensokyo
belongs to the mod's own entities, added by the `plain_fairy` biome modifier, not to a vanilla spawn table copied in
beside them.

| biome | features after | why |
|---|---|---|
| `muenzuka` | `forest` | deciduous canopy, dark forest floor to break up with podzol |
| `human_village` | `plains` | open, already tilled-looking, plus its pumpkin and sugar cane patches |
| `myouren_temple_barrens` | `badlands` | the dry dead-bush and coarse-dirt look |
| `bamboo_forest_of_the_lost` | `snowy_taiga` | spruce understorey, so the grove reads as a winter one — though the biome itself no longer snows, §6 |
| `hill_of_the_nameless` | `meadow` | open flowered hill with no canopy to hide in |
| `untrodden_valley` | `taiga` | conifers, ferns, already cold |
| `genbu_ravine` | `swamp` | the clay disk and reeds read as wet banks, and the lily pads work underwater |
| `youkai_mountain_foothills` | `windswept_hills` | sparse oak on gravelly ground |
| `sanctuary` | `meadow` | meadow's own sparse birch reads as a deliberate grove |
| `hell_of_blood_pools` | `soul_sand_valley` | a basin of souls is the one nether pool; basalt pillars stand it up off the floor |
| `animal_realm` | `warped_forest` | the one nether biome that is mostly growth, which is what a realm of animals is buried in |
| `hell_of_blazing_fires` | `basalt_deltas` | basalt columns and blobs off the floor make a hall a delta rather than a cave |
| `former_hell` | `crimson_forest` | roots and vines growing back through a ruin is the whole idea of the place |

The four underground biomes keep the overworld ore set and `NETHER_COMMON` — fire, soul fire, glowstone, the two
nether mushrooms — and take the rest of their nether biome, less that biome's **ores**. Those are deliberately absent:
the nether ores are placed against the nether's own 0…128 column and copied verbatim they land at y 5…41, nowhere
near the underground biomes, which sit at y 64…195. The overworld ore set replaces them.

The nether **springs** are dropped for the same reason of clutter, with one exception. `spring_open` puts lava wherever
a cave has a floor, which is what the nether wants and Gensokyo does not. `spring_delta` is not that: it is the deltas'
lava *surface*, the one feature that makes a biome of basalt a lava biome at all, so `hell_of_blazing_fires` keeps it
as `DELTA_LAVA`. Without it the blazing fires are hot-looking rock — the surface rule only gives that biome
`magma_block`, which glows but does not flow.

### A spring that works on nether rock

`SPRINGS` gives every underground biome vanilla's overworld `spring_lava` and `spring_water`, and in the four hell
biomes **neither places a single block**. `SpringFeature` refuses unless the block above *and* the one below is in
its `valid_blocks`, and that list is stone, granite, diorite, andesite, deepslate, tuff, calcite and dirt — while the
hell palettes are netherrack, blackstone, crimson nylium, magma, basalt, packed mud and stone brick. They work in
`mountain_interior`, whose whole column is stone, and nowhere else.

That is why the blazing fires carry a second one, `gensokyolegacy:blazing_hell_lava_spring`: vanilla's
`spring_lava_nether`, whose valid blocks are netherrack, soul sand, gravel, magma and blackstone — two thirds of what
that biome stands on. It still will not place on basalt, so lava collects on the blackstone and magma of the palette
and not the basalt between them.

Note that even where `SPRINGS` does work, `spring_lava_overworld`'s height range is
`very_biased_to_bottom(0, below_top 8)`. In a −64…320 world that biases to the world floor, well below the y 64…195
band, so it contributes little lava inside these biomes. It is there for the caves below them, not for the halls.

The four fixed locations have their own lists and their own section, §7.

`sunflower_plains` is the hot humid corner of the lower ground, `plains` plus `patch_sunflower`. It is also the one
biome that keeps **vanilla's** climate and special effects rather than Gensokyo's: temperature 0.8, downfall 0.4, water
`#3F76E4`, fog `#C0D8FF`, sky `#78A7FF`, and no grass or foliage override. It was a verbatim vanilla copy until the
feature lists were written out, and a sunflower field is a plains — nothing about sunflowers changes what the grass
looks like. Only its features are ours.
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
| `cliff_scree` | 1 patch | secret heaven cliff |
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
4. **cliff faces** (`secret_heaven_cliff`) — stone where it is steep, and nothing else, so the flat ground falls
   through to the overworld tree. This is the only branch in the list that is a single `steep()` test, and that is the
   whole of it: the scree is the tail's, and the barrenness is the base's.
5. the four hell palettes.
6. `SurfaceRuleData.overworld()`.

`tengu_settlement` has no branch at all, which is also the guide's instruction: its paths are
`settlement_paths`, a placed feature, because a surface rule would paint the whole biome floor. Its topsoil is the
dimension's. `secret_heaven_cliff` takes the `steep()` half of the summit's branch and none of its palette, which is
the point of it being a *cliff* and the limit of what it is meant to be.

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

Each of the open-ground bands splits into four near-equal quarters, which is what cutting both climate axes once
gives. The lower ground's four cells are 8.14 / 8.10 / 6.04 / 5.73 % of the map and the upper ground's are
6.96 / 5.32 / 4.16 / 3.45 %, out of a plain that holds 47.90% — the same plain the two bands held before the merge
in `gensokyo_dimension.md` §4, which is to say the erosion cut moved the share between the two grounds without
changing the share of the plain itself.

The four underground biomes never appear on this map: `terrain.py` samples one column per chunk at the surface, and the
underground bands only win more than 128 blocks down. Their placement is `doc/design/gensokyo_dimension.md` §9.

`UNDERGROUND` is **one continentalness row split by temperature and humidity**, not one cell per region. It used to be
keyed on how far the surface above it sat — blood under the water table and the coast, the animal realm under the lower
ground, the old hell under the upper ground, the burning hell under the mountain — which made every cave in the
dimension the same biome, and keyed the underground to the height chain so it inherited every move of the offset
spline. Now one row spans continentalness `[-1.00, 0.22]` and depth `[0.53, 1.50]`, and the four biomes are cut out of
it by climate:

| | hot | cold |
|---|---|---|
| **humid** | `hell_of_blood_pools` | `former_hell` |
| **dry** | `hell_of_blazing_fires` | `animal_realm` |

`former_hell` is then repeated a second time under the mountain, at continentalness `[0.50, 1.00]` and depth
`[1.70, 2.00]`. It is the only one of the four that appears in both rows; the other three belong to the low ground and
have no business claiming the mountain's deep rock.

### `mountain_interior`

The fifth rock band, and the only one that is not a climate region: a single cell over the whole temperature and
humidity plane at continentalness `[0.50, 1.00]`, depth `[0.30, 0.53]`. It is the rock between the surface and the
underground — under the mountain, and nowhere else.

It is `minecraft:stony_peaks`' feature list — the mountain's own rock is overworld rock, not nether rock: its ore set,
its emerald, `ore_infested`, its lava lakes and its glow lichen — with an **empty** tail. It used to be built on
`Biomes.DEEP_DARK`, which is where the sculk in the halls came from: `sculk_vein` and `sculk_patch_deep_dark` are
deep dark's own step-7 features and were inherited wholesale. Their placement has no guard against being above ground,
only a biome check, and the surface rule paints the whole column stone, so they spread over every hall floor.

Its surface rule is plain `stone` over the whole column: the band never has a floor of its own to grade into, so there
is nothing for deepslate to blend towards. It has no `freeze_top_layer` either — the band only ever sits below a
surface, so the top-layer step has nothing to act on. Spawns are empty and the temperature range is `[0.6, 0.8]`.

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
| myouren_temple_barrens | 0.2 | 66 … 96 | 0.03 | 0.17 | no |
| muenzuka | 0.3 | 66 … 96 | 0.03 | 0.27 | no |
| human_village | 0.8 | 66 … 96 | 0.03 | 0.77 | no |
| sunflower_plains | 0.8 (copy) | 66 … 96 | 0.03 | 0.77 | no |
| magical_forest | 0.7 | 66 … 96 | 0.03 | 0.67 | no |
| sakura_forest | 0.5 | 66 … 96 | 0.03 | 0.47 | no |
| bamboo_forest_of_the_lost | 0.3 | 66 … 96 | 0.03 | 0.27 | no |
| hill_of_the_nameless | 0.3 | 66 … 96 | 0.03 | 0.27 | no |
| untrodden_valley | 0.5 | 96 … 128 | 0.07 | 0.43 | no |
| genbu_ravine | 0.35 | 96 … 128 | 0.07 | 0.28 | no |
| youkai_mountain_foothills | 0.6 | 96 … 128 | 0.07 | 0.53 | no |
| sanctuary | 0.35 | 96 … 128 | 0.07 | 0.28 | no |
| grove | **−0.2** (copy) | 128 … 192 | 0.11 | −0.31 | **yes, vanilla's own** |
| tengu_settlement | 0.4 | 128 … 192 | 0.11 | 0.29 | no |
| secret_heaven_cliff | 0.35 | 128 … 192 | 0.11 | 0.24 | no |
| youkai_mountain_summit | 0.4 | 192 … 220 | 0.19 | 0.21 | no, but only just |
| misty_lake | 0.3 | ≤ 63 | 0 (below y 80) | 0.3 | no |
| misty_lake_shore | 0.4 | 63 … 66 | 0 | 0.4 | no |

Every biome this project defines clears the line, though the plain's `myouren_temple_barrens` clears it by only 0.02
and `youkai_mountain_summit` by 0.06 at the very top of its band — raise its declared temperature before assuming
anything else is safe. The margin is what the column is for: the altitude loss is a noise term, so a biome only ever
snows in the top few blocks of its band and only in the coldest few of those. `myouren_temple_barrens` owes its 0.02 to
the plain merge: before it, the lower ground stopped at y 80 and lost no altitude at all, so the cell had 0.05 in hand
and now has to hold it across the upper half of the band. `secret_heaven_cliff` is the tightest of the ones added
since the guide, at 0.09, and it is the second half of a band that already has a colder neighbour, so it is the first
thing to re-check if anything moves.
The six biomes the grid puts in a cold cell declare 0.2 to 0.35, comfortably warm, because a cold *cell* is a
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

## 7. The fixed locations

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

`secret_heaven_cliff` is the exception and is not in the guide. It is the **dry half of the settlement's band**, so
continentalness still fixes the height and humidity alone splits it off: `tengu_settlement` takes `HUMID` over the full
temperature span and `secret_heaven_cliff` takes `DRY`. Both halves are in the same region, so the pair measures 8.66%
and 7.28% of the map over 256 × 256 chunks at seed −1838080655014084215, against the band's 13.25%.

That is the whole of the selection rule, and it is deliberately the cheapest one that reads as "beside the town rather
than in the same cell": humidity is already cut once at 0 for the four-cell bands, so the cliff needs no new axis, no
new edge and no new machinery — only a second cell in the row.

The one thing the grid had to carry over is the waterline invariant of §3. `ocean` was the drowned set only
because its continentalness edge sat on the waterline of the offset spline; `misty_lake` now sits in exactly that cell
and inherits the invariant unchanged, so the lake is still precisely the set of drowned columns. `tools/terrain.py`
confirms it: `misty_lake` measures 10.84% against 10.55% under water, the same near-match `ocean` had.

| biome | base | why |
|---|---|---|
| `misty_lake` | `river`, **minus `trees_water`** | a freshwater lake. `minecraft:ocean` was the obvious base and is wrong: both it and `river` carry water trees, and mangroves in a freshwater lake are exactly the swamp the guide rules out |
| `misty_lake_shore` | `beach` | already the sand-and-gravel shoreline; the guide's waterline and coves are added on top |
| `tengu_settlement` | `forest` | a wooded settlement edge, and distinct from `youkai_mountain_foothills`, which is `windswept_hills` |
| `secret_heaven_cliff` | `windswept_forest` | the barer neighbour of `forest`, and a third base in the mountain band so the three stay distinguishable: sparse spruce and `patch_grass_badlands` against forest's birches, oaks and flowers, and nothing like the foothills' `windswept_hills` |
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
| `secret_heaven_cliff` | boulders and a sparser scree of gravel and tuff, one patch per chunk |
| `youkai_mountain_summit` | soil pockets (a grass disk on stone), moss and glow lichen, scree, a rare pine |

The cliff's tail is the settlement's with the paths dropped and `cliff_scree` in place of `foothill_moss`: no moss, no
ferns and no bracken, and a quarter of the settlement's moss density. It is a surface rule that carries the rest — a
`steep()` test to bare stone, which is what makes a cliff read as a cliff rather than as a barer field — while the
flat ground falls through to the dimension's shared grass over dirt. `MYOUREN_TEMPLE_BARRENS` is the opposite trade and
the reason the cliff is not built that way: it takes the whole `badlands` surface rule with it, which is a bigger jump
than "a bit more barren" asks for next to a town.

Precipitation stays **on** for the cliff, which is also what keeps it off the snow line: it declares 0.35 against the
settlement's 0.4, and `gensokyo_dimension.md`'s snow table below is the place to check that margin before changing it.

Two of those needed a ground predicate that is not `BlockTags.DIRT`, because the lake's surface rule makes the bed sand
and gravel: `onGround(tries, palette, blocks...)` is the general form, and `onDirt` is the `BlockTags.DIRT` shorthand.

### Not done

The guide asks for the Scarlet Devil Mansion and the tengu buildings as structures, with terrain validity and
spacing. No such templates exist — the mod's only structures are the character homes and Cirno's nest — so both
settlements land as the natural ground they sit on, exactly as `human_village` does. That is a separate job with a
separate template budget, and the guide is explicit that these must not be faked with random placed features.

---

## 8. Tags

`GLStructureTagGen.genBiomeTag` writes `is_magical`, `is_forest`, `is_plains`, `is_mountain`, `is_mountain/peak`,
`is_underground`, `is_spooky`, `is_floral`, `is_wet`, `is_beach`, `is_aquatic`, `is_dry` and `is_taiga`, plus
`has_structure/*` and `has_spawn/plain_fairy`.

**All of these are ours** — `gensokyolegacy:is_forest`, not `minecraft:is_forest` or `c:is_forest`. The names match
the vanilla ones so the classification still reads as one, but the namespace is the point: those tags are shared
vocabulary. Every mod that ships a forest biome writes to the same `is_forest`, so its contents are a claim *about*
the biome that other mods read when deciding their own behaviour, and a mod testing `c:is_forest` to decide where to
put mushrooms would otherwise start finding them on a cliff it cannot see. The same goes for the rest: `is_beach` and
`is_dry` in particular are branches other mods write conditionals against, not a description we get to broadcast.

The one consumer that reads a vanilla pair is our own: `has_spawn/plain_fairy` nests **both** `c:is_forest` /
`c:is_plains` and our two. Reading a tag and writing it are different acts — the naming argument above is entirely about
the write — and the modifier is not scoped to a dimension, so it has to keep reaching the overworld's forests and
plains or the vanilla world loses its fairies. Ours carry Gensokyo's, the shared pair carries vanilla's.

Nothing else in the mod or in the game reads any of these — grepping the decompiled sources, `is_forest`,
`is_plains`, `is_mountain`, `is_taiga` and `is_underground` are consumed nowhere at runtime; they exist as
classification and as something a datapack can widen. That is why the vanilla writes could go without anything else
noticing.

`misty_lake` is deliberately **not** in `is_ocean` either way, and still not in our own `is_aquatic` alone: it is a
freshwater lake, and anything that branches on an ocean tag would bring the oceans' own overrides with it.

## 9. Names

`GLLang.Biome` carries the English display names; the zh_cn strings are hand-authored in
`lang/zh_cn/world.json` and merged by `organize.ResourceOrganizer`. The lang key is the vanilla
`biome.gensokyolegacy.<id>` one rather than the usual `gensokyolegacy.*` scheme, because that is the key the F3 biome
readout and the locator item look up.

Every one of the seventeen has a display name. `sunflower_plains` keeps a name that reads like the vanilla biome it
copies, which is deliberate: it is the same grass.
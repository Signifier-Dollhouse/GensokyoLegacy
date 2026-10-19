# Gensokyo biomes (`init/data/biome/`)

Every biome the Gensokyo dimension can select: the twelve of `gensokyo_dimension_biome_guide.md` §3 and §4, the two that
were already there (`magical_forest`, `sakura_forest`), and the four fixed locations of
`gensokyolegacy_fixed_biomes_guide.md` plus the one it does not name.

Each biome is a class of its own in `biomes/`, extending `AbstractBiomeGen`: `MuenzukaBiomeGen` is the Muenzuka biome's whole
registration, and `BiomeGen.KEY` on it is the key the climate grid, the tags and the structures ask for. The shared
plumbing lives in `AbstractBiomeGen` (the vanilla runs, `Layers`, the `Registrar`, the biome factory overloads) and in
`GensokyoBiomeFeatures` (the features more than one biome places); the places the biomes are selected from are in
`GensokyoDimension`'s climate grid. `GensokyoLegacy#gatherData` lists every instance in one `BiomeGen.build` call, the
same way it lists the quest dialogs in one `QuestDialogData.build` call.

That split is for merges, not for tidiness: a biome's features, its climate and its colours are all in its own file,
so two branches editing two different biomes never open the same one.

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
its own features, step by step**, in `build`, and takes a short custom tail at the end of the step it belongs to.
`BiomeGen.Layers` is the whole mechanism; the shared runs it draws on (`OVERWORLD_ORES`, `PLAINS_GROUND`,
`NETHER_COMMON`, …) are named by what they are and live in `AbstractBiomeGen`.

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
| `muenzuka` | `forest`, **minus `trees_birch_and_oak`** | a burial grove: half locust over the markers, half the oak and birch that grew here before — see §2.2 |
| `human_village` | `plains` | open, already tilled-looking, plus its pumpkin and sugar cane patches |
| `myouren_temple_barrens` | `badlands` | the dry dead-bush and coarse-dirt look, with the locust that stands over the temple's markers |
| `bamboo_forest_of_the_lost` | `snowy_taiga` | spruce understorey, so the grove reads as a winter one — though the biome itself no longer snows, §6 |
| `hill_of_the_nameless` | `meadow` | open flowered hill with no canopy to hide in |
| `untrodden_valley` | `taiga`, **minus `trees_taiga`** | half maple, half oak and birch: the grove is the reason the valley reads as untrodden rather than as cold — see §2.2 |
| `genbu_ravine` | `swamp`, plus `lush_caves` | the clay disk and reeds read as wet banks, the lily pads work underwater, and lush caves' eight put the growth and vines in the ravine floor — see below |
| `youkai_mountain_foothills` | `windswept_hills`, **minus `trees_windswept_hills`** | maple again, and the thinnest of the three groves: the wind is what thins it — see §2.2 |
| `sanctuary` | `forest` | the mountain's own grove — birch and oak under the cherry tail, against the hill's open meadow beside it |
| `hell_of_blood_pools` | `soul_sand_valley` | a basin of souls is the one nether pool; basalt pillars stand it up off the floor |
| `animal_realm` | `warped_forest` | the one nether biome that is mostly growth, which is what a realm of animals is buried in |
| `hell_of_blazing_fires` | `basalt_deltas` | `minecraft:delta` pools plus basalt columns and blobs off the floor make a hall a delta rather than a cave |
| `former_hell` | `crimson_forest` | roots and vines growing back through a ruin is the whole idea of the place |

The three **bolded** rows are the ones that drop a vanilla tree placement for one of the mod's own. The run stays in
`AbstractBiomeGen` unchanged for the biomes that still want it, and the ones that do not take `FOREST_FLOOR`,
`TAIGA_FLOOR` or `CLIFF_FLOOR` — the same list with that one placement filtered back out. The count is left out of the
shared run on purpose and lives in the biome's own placed feature instead, so a grove can be as thin as the wind makes
it or as dense as a valley floor.

The hill is the one biome whose base cannot carry what it is for. It is open meadow, so its only canopy is the
sparse trees it draws itself, and its foliage override sits a fifth under its grass override (`#5F724B` against
`#87A06A`) — a shadowed hill is the nameless one, a hill at meadow brightness is a field of daisies. Its
`hill_of_the_nameless_flowers` tail is seven patches against the village's four, laid over the meadow's own
`flower_meadow`.

The four underground biomes keep the overworld ore set and `NETHER_COMMON` — fire, soul fire, glowstone, the two
nether mushrooms — and take the rest of their nether biome, less that biome's **ores**. Those are deliberately absent:
the nether ores are placed against the nether's own 0…128 column and copied verbatim they land at y 5…41, nowhere
near the underground biomes, which sit at y 64…195. The overworld ore set replaces them.

The nether **springs** are dropped for the same reason of clutter, with one exception. `spring_open` puts lava wherever
a cave has a floor, which is what the nether wants and Gensokyo does not. `spring_delta` is not that: it is the deltas'
lava *surface*, the one feature that makes a biome of basalt a lava biome at all, so `hell_of_blazing_fires` keeps it
as `DELTA_LAVA`. Without it the blazing fires are hot-looking rock — the surface rule only gives that biome
`magma_block`, which glows but does not flow.

### Lush caves, on the ravine floor

`genbu_ravine` is `swamp` on the surface and `minecraft:lush_caves` below it, and it takes **all eight** of lush caves'
features rather than a selection:

| | key | step |
|---|---|---|
| vegetation | `lush_caves_ceiling_vegetation`, `cave_vines`, `lush_caves_clay`, `lush_caves_vegetation`, `rooted_azalea_tree`, `spore_blossom`, `classic_vines_cave_feature` | `vegetal_decoration`, in `addLushCavesVegetationFeatures`'s own order |
| special ore | `ore_clay` | `underground_ores`, between the default ore set and the soft disks, which is where vanilla writes it |

Taking them whole is the point: the floor growth, the two vine sets, the ceiling growth and the rooted azalea all hang
off the same cave surfaces, so any proper subset of the seven is a fragment of one decoration rather than a style. The
order inside the step is `addLushCavesVegetationFeatures`'s because `FeatureSorter` reads it, not for looks.

What it does **not** replace is the ravine's own: `ravine_moss` and `ravine_stones` still come after the lush block in
`vegetal_decoration`, and `ravine_mud` is still last in `underground_ores`. Lush caves is growth and vines on a cave
surface; it is not a ground cover, and its moss and mud were never Gensokyo's to begin with. Nothing else takes these
— `genbu_ravine` is the one surface biome in the dimension with a ravine in it.

### `minecraft:delta` is the one that fills them

`spring_delta` was being read as the delta's lava, and it is not: it is a **spring**, one block of lava every
sixteenth try. The feature that fills a delta is `minecraft:delta`, which the blazing fires were also missing — and it
is the one that matters, because it is the whole *volume*:

| | tries | what it puts down |
|---|---|---|
| `minecraft:delta` | 40, **on every layer** | a pool of lava with a `magma_block` rim, wherever there is a one-block air pocket under a solid floor |
| `minecraft:spring_delta` | 1 in 16 | a single lava block, and only where the block below is in the nether's spring list |

`DeltaFeature.isClear` wants the block above to be air and the other five neighbours to be solid, which is exactly the
one-block pocket a cave floor leaves, so the pools land on the cavern floors and the basalt columns then stand out of
them. With the columns but no `delta`, the biome had basalt growing off dry rock — `BASALT_COLUMNS` now carries all
three in vanilla's order, `delta` first.

### The ravine's springs

`genbu_ravine` replaces the shared `SPRINGS` with a single feature: vanilla's own `spring_water` at **three times** the
count, and **no lava at all**.

| | vanilla | ravine |
|---|---|---|
| `spring_water` | 25 tries | **75** |
| `spring_lava` | 20 tries | **none** |

Same configured feature, same height range — `bottom … 192` — so the only thing that changed is how often it tries.

It **replaces** rather than joins, which is the part that is easy to get wrong. The feature is in the `fluid_springs`
step, so carrying vanilla's as well would place every spring twice over and make the count a lie about what the biome
is doing. Replacing rather than adding is also what takes the lava with it: the shared pair's `spring_lava` was the
only lava the ravine had, so dropping that one key is what leaves it a wet place and nothing else. The lava the ravine
does have is the aquifer's — the sea of `gensokyo_dimension.md` §11 and the lava lakes it also carries — not a spring.

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

The features live with the biome that is the only one to use them, under
`worldgen/{configured,placed}_feature/gensokyo_biome/`. Only `stones`, `foothill_moss`, `lake_cattails`, `maple_trees`
and `maple_fallen_leaves` are shared enough to belong in `GensokyoBiomeFeatures`. Five kinds:

**Ground cover** — `RANDOM_PATCH` of `SIMPLE_BLOCK` on `BlockTags.DIRT`, the same shape as the magical forest's grass
and flowers. The vanilla flower and grass features of the base are kept as well, so the mod blocks are an accent
rather than a replacement.

**Standing deco** — the same patch, but drawn by `gensokyolegacy:double_block` instead: the jizo and the wasteland
gravestone are `DoubleBlockImpl` delegates that occupy two blocks, and vanilla's `simple_block` writes only the bottom
half. A top half with nothing under it passes the placement predicate and then drops off on the first neighbour
update, so a patch of them written the vanilla way comes apart as it generates. These are also the only two patches
placed by `Registrar.floor(...)` and drawn by `patch(...)` with a `y_spread` of 0; §2.3 has both.

**Soil disks** — `Feature.DISK`, in the `UNDERGROUND_ORES` step. Podzol under Muenzuka's trees, coarse dirt through the
barrens, mud under the bamboo and in Genbu Ravine.

**Cave deposits** — the same patch, but placed with vanilla's own cave decoration pipeline
(`EnvironmentScanPlacement` downward + `RandomOffsetPlacement`) in the `UNDERGROUND_DECORATION` step, and gated on
`BlockPredicate.solid()` instead of the dirt tag so they survive a hell palette that is not vanilla stone.

**One lake** — `former_hell_lake`, see §2.1. It is the only feature in the tail that is not a patch of blocks.

| placed feature | tries / count | used by |
|---|---|---|
| `stones` | 2 patches, mossy cobble / cobble / stone brick | hill, barrens, valley, foothills, ravine |
| `grave_marker` | 1 in 4 chunks | muenzuka |
| `gravestone` | 4 tries in 1 patch per chunk, on the floor | muenzuka |
| `ghost_flowers` | 3 patches | muenzuka |
| `muenzuka_trees` | 4 trees, locust half | muenzuka |
| `village_flowers` | 4 patches | human village |
| `barrens_scrub` | 3 patches | barrens |
| `barrens_gravestone` | 4 tries in 1 patch per chunk, on the floor | barrens |
| `barrens_jizo` | 24 tries in 1 patch, all four kinds, on the floor | barrens |
| `barrens_locust_trees` | 6 trees | barrens |
| `hill_of_the_nameless_flowers` | 7 patches | hill of the nameless |
| `bamboo_dense` / `bamboo_sparse` | 6 patches / 1 in 3 | bamboo forest of the lost |
| `ravine_moss` | 6 patches | genbu ravine |
| `foothill_moss` | 4 patches | youkai mountain foothills |
| `valley_mixed_trees` / `maple_fallen_leaves` | 10 trees / 2 patches | valley, settlement 6, foothills 2 |
| `cherry_trees` / `cherry_litter` | 15 trees / 2 patches | sakura forest |
| `sanctuary_trees` / `sanctuary_flowers` | 1 in 5 chunks / 3 patches | sanctuary |
| `blood_deposit` / `bone_pile` | 3 patches / 1 in 6 | hell of blood pools |
| `den_rubble` / `fungus_patch` | 1 in 5 / 4 patches | animal realm |
| `fire_vent` / `ash_patch` | 1 in 4 / 5 patches | hell of blazing fires |
| `old_rubble` | 1 in 4 | former hell |
| `former_hell_lake` | 1 in 6 chunks, in the LAKES step | former hell |
| `lake_clay` / `lake_cattails` / `misty_lake_lily_pads` | 2 patches / 1 in 3 / 1 in 3 | misty lake |
| `shore_sand` / `shore_reeds` / `shore_flowers` | 4 / 3 / 3 patches | misty lake shore |
| `settlement_paths` | 1 in 4 | tengu settlement |
| `cliff_scree` | 1 patch | secret heaven cliff |
| `summit_soil` / `summit_moss` / `summit_scree` / `summit_trees` | 2 / 4 / 4 patches / 1 in 16 | youkai mountain summit |

### 2.1 The ruins' standing water

`former_hell_lake` is a water pool in a cavern, and it is neither of the two things a vanilla lake is:

- `minecraft:lake_underground_water` takes the first open block a downward scan finds and pours water into whatever
  surrounds it, which is how a one-block pocket in the rock ends up holding a puddle;
- the heightmap placements put a lake on the open surface, which is what the magical forest's pond wants and what this
  biome must not have — the ruins are underground, and a pool you can see the sky in is not underground.

So it is placed by walking the column. The placed feature gives a position in the biome's depth band
(`HeightRangePlacement.uniform(bottom, absolute(64))`, no heightmap), and `LakeFeature` reads that column from the
world floor up to the topmost rock of the column. Every stretch of open air between two solid blocks is a candidate
room if:

| test | value | why |
|---|---|---|
| room height | at least `clearance` = 5 open blocks over the floor | the 5 blocks of air clearance, read on the cavern floor itself. No ceiling on the size: a cavern a hundred blocks high is still a cavern |
| roof | below the column's `WORLD_SURFACE_WG` height | an open run that reaches the surface is the sky, so this is what keeps the pool out of the open and off the world's surface |
| floor | `isSolid()` somewhere in the two blocks the box reaches | a bed over a void is a shell, and the water would hang in it. It may sit a block high or a block low |
| biome | same as the placement's | the caverns cross the depth bands, so without it the wrong hell's cavern gets water |

**The pool is dug into the floor, not stood on it.** A room's block comes back one below its waterline, so the
maker's topmost water sits on the cavern's own floor and the air pocket it cuts above that — which opens the floor
itself — is what makes the pit visible. A pool laid on top of the floor is a tub standing in the cave.

**The surface is level and only the bottom follows the blob.** The water in every column reaches the waterline, and
its depth is whatever the ellipsoid leaves underneath: deepest under the middle of a component, shallow where it
thins out. That is what makes the pool elliptical. A surface that followed the blob as well would put the rim's
water a block or two below the middle's, in a notch of its own, which is both the hole in the pool's edge and a
slope for the water to run down.

**The blob's own margin belongs to the pool, not to the shore.** An ellipsoid thins out towards its edge, and where
it is thinner than the depth of the water it never reaches the waterline: those columns are the pool's outer margin
and the saddle between two of its components, and they are opened rather than walled. Treating them as shore put a
wall of stone brick straight across the water two lobes share.

**The ground it is dug into may step.** The bed goes on the highest rock the column has within the two blocks the
box reaches, so a floor that dropped a block under part of the pool deepens it there instead of refusing it, and
the maker cuts through whatever the floor puts in the way above the water — otherwise a pool laid against a step
would be roofed shut by it. Rock standing over the wall is taken for the wall's material, so the rim ends up even
where the floor runs high. What it will not take is a bed with nothing under it: rock has to be there within the
two blocks, or the pool hangs in a shell.

The rooms come back **all of them, in a random rotation**, and the first that fits is used. A column with several
rooms used to spend every one of its lakes in the lowest, which is both the worst-looking room and the one a cave
system has most of.

`LakeMaker` then checks the whole footprint the same way: rock within the two blocks the box reaches for every
column that holds water, nothing liquid in the water itself, and `clearance` of open air over each column's highest
water — counting the air pocket the maker cuts itself, since on a dug-in pool those blocks are the floor it opens.
It lays the wall that dams the pool out of the ruin's own stone brick, one block thick — a cavern floor is flat, so without a rim
the water would spread over it and flood the cavern instead of standing in it, and stone brick is what this biome's
rock is made of — and nothing else: no turf cap, no planting, since a flooded floor is neither.

Everything above is decided once, in the maker's `pre`, not while it writes: the shape says which columns hold
water, but not whether the thin ones are pool margin or shore, nor where the bed of a column goes when the cavern
floor has stepped under it, and a decision taken while writing is a decision taken twice. `pre` settles all of that
per column and the gen is left a walk over what it settled.

The magical forest's pond and this are the same feature (`gensokyolegacy:lake`) with the same ellipsoid maker; they
differ in `mode` and in what the mode checks. See `magical_forest_vegetation.md` §6.5 for the pond.

The guide's §7 order — surface palette, common vegetation, ground accents, water, structures, atmosphere — was not
followed literally, because a biome's identity here comes mostly from its base and its colours. Every list in the
guide is a suggestion of what *can* be there, and the palette table below is what was taken from it.

### 2.2 The mod's own trees, and the leaves under them

Six of the seven `TreeType`s used to be sapling-only: a configured feature with no placed feature pointing at it. Six
biomes now plant them, and the split is the one the rest of this document already uses — a tree **several** biomes
share is one configured feature in `GensokyoBiomeFeatures`, the **count** is each biome's own placed feature.

| placed feature | configured feature | trees |
|---|---|---|
| `valley_mixed_trees` | `valley_mixed_trees` | 10 |
| `settlement_maple_trees` | `maple_trees` | 6 |
| `foothills_maple_trees` | `maple_trees` | 2 |
| `cherry_trees` | `cherry_trees` | 15 |
| `muenzuka_trees` | `muenzuka_trees` | 4 |
| `barrens_locust_trees` | `gensokyolegacy:locust_tree` | 6 |

Every one of these is a `RANDOM_SELECTOR` that plants **one** tree per placement call, so the biome's
`CountPlacement` is the whole of the density and nothing is hidden in the selector.

| selector | list | default | share |
|---|---|---|---|
| `maple_trees` | red 1/3, orange 1/2 | yellow | red 33.3 %, orange 33.3 %, **yellow 33.3 %** |
| `valley_mixed_trees` | `maple_trees` 0.5, oak 0.5 | birch | **maple 50 %**, oak 25 %, birch 25 % |
| `muenzuka_trees` | locust 0.5, oak 0.5 | birch | **locust 50 %**, oak 25 %, birch 25 % |
| `cherry_trees` | pink 0.5 | purple | pink 50 %, purple 50 % |

The two numbers in the `chance` column are not shares. `WeightedPlacedFeature.chance` is a `floatRange(0, 1)` that
`RandomSelectorFeature` tests with `nextFloat()` **in list order, stopping at the first that fires**, so an entry's
share is its own chance times every chance that missed before it:

```
P(entry i) = chance(i) × Π (1 − chance(j)) for every j before it
P(default) = Π (1 − chance(j)) over the whole list
```

Two consequences worth writing down, because both were got wrong here first:

- **`chance = 1` is not "first" or "weight 1".** `nextFloat()` is `[0, 1)`, so `1.0` matches on every roll, the first
  entry takes the grove entirely and the default is dead code. A selector has to be written in fractions of a whole.
- **The odds compound, so the list does not sum to the shares.** `[orange 0.25, yellow 0.25]` reads like 25 / 25 and
  is actually 25 / 18.75 / 56.25. The share an entry has to be given is the one it wants **divided by everything that
  missed before it**, which is why the table above never reads as the split it produces:

  | wanted | list | why |
  |---|---|---|
  | 50 / 25 / 25 | first 0.5, second 0.5 | two halves of a whole, and the default takes the other quarter |
  | 33.3 each | first 1/3, second 1/2 | a third, then half of the two thirds that are left, and the default the rest |

  With only two list slots and a default, an even two-way split is the one that needs no arithmetic — `0.5` and `0.5`
  — and an even three-way needs `1/3` and `1/2`, which is the least obvious pair of numbers in the whole feature
  tail. Measuring the generated JSON against the roll is the only check that catches either of these.

`valley_mixed_trees` takes the whole of `maple_trees` as its first entry rather than repeating the three maples: an
inline placed feature with no modifiers has a count of zero but is still *called*, so it rolls one maple per call
with the same red/orange/yellow split inside it. `barrens_locust_trees` needs no selector at all — there is one locust
and it has the barrens to itself.

The maple belt runs `youkai_mountain_foothills` → `tengu_settlement` → `untrodden_valley`, thinning upward with the
wind. `sakura_forest` is outside it and replaces `minecraft:trees_cherry` outright; nothing else in the dimension
plants vanilla cherry any more. `untrodden_valley` is the one grove the maples do not have to themselves, and
`muenzuka` is the same shape with the locust: a burial grove is half the tree that grows back over a grave and half
the oak and birch that grew there before, at the four a chunk that keeps the ground reading as a graveyard.

Every one of those four biomes that grows maple or cherry also carries a carpet of the matching fallen leaves:

| placed feature | carpet | patches |
|---|---|---|
| `maple_fallen_leaves` | `mixed_maple_fallen_leaves`, shared by the three groves | 2 |
| `cherry_litter` | pink and purple cherry fallen leaves, 50 % each | 2 |

`maple_fallen_leaves` is the only placed feature of the set that is genuinely shared, because the canopy filter
already scales it: three groves at one density means the sparse one gets little litter under its few crowns without
its count needing to differ. The placement is the magical forest's, moved onto `Registrar.litter` so a normal biome
can use it: `CountPlacement`, `in_square`, `gensokyolegacy:canopy` with `under: true`, then the
`MOTION_BLOCKING_NO_LEAVES` heightmap and `biome`. The no-leaves heightmap is what puts the carpet on the floor
under the crowns rather than on top of them, and the canopy filter drops the positions that turned out to be open sky.
It only reads correctly because the trees are listed **before** the carpet in each biome's own feature list — that list
is the order the `FeatureSorter` breaks ties in, and the filter needs the crowns already up when it runs.

The carpet itself is `fallenLeaves(tries, spread, species…)`, weighted three to one towards the single layer: a
carpet that is two blocks tall everywhere reads as snow rather than as litter, and a patch puts a lot of them side by
side.

### 2.3 The standing decos

The jizo and the gravestone are the only two patches drawn by `gensokyolegacy:double_block` rather than by vanilla's
own `simple_block`: they are `DoubleBlockImpl` delegates occupying two blocks, and `simple_block` writes only the
bottom half. A top half with nothing under it passes the placement predicate and then drops off on the first
neighbour update, so the feature writes both halves itself.

They are the only two patches in the tail placed by `Registrar.floor(...)` rather than `cover(...)`, and that one
word is the whole of why they work. Everything else decorates with `HEIGHTMAP_WORLD_SURFACE`, and **that heightmap
counts leaves** — a locust canopy six blocks up *is* the surface of its column by the time the tail runs, so a deco
placed with it lands on the roof of the tree. `floor` uses `MOTION_BLOCKING_NO_LEAVES` instead, which returns the
ground the surface rule actually laid down, and the jizo then stand on the floor whether or not a tree is over them.

The difference is not cosmetic. Under `cover` the decos were also *invisible under canopy*: the only defence against
landing on the roof was that leaves are not in `BlockTags.DIRT`, so the predicate refused the try — which also meant
every column with a tree over it could never carry a jizo at all. On `floor` the predicate is read at the floor, so
it describes what it says, and a grove of locust is exactly where the roadside row goes.

Two things follow that are worth keeping separate:

- **The dirt tag stays.** It is no longer load bearing against the canopy, but it is still the rule that keeps a deco
  off the gravel, the sand and the exposed stone. Widening it to `BlockPredicate.solid` would be a different change,
  not a better one.
- **Moving the decos ahead of the trees instead is not the fix.** It works, but it has to be done identically in every
  biome sharing `stones` or `foothill_moss`, because `FeatureSorter` throws `Feature order cycle found` when two
  biomes disagree about the order of one placed feature. Choosing the heightmap over the ordering keeps the tail in
  the order the other features are already in.

The patch itself is `patch(tries, spread, 0, …)`. The zero is `y_spread`: `RandomPatchFeature` takes its vertical
offset as `nextInt(y_spread + 1) - nextInt(y_spread + 1)`, so at the vanilla 3 only a quarter of the tries land back on
the terrain surface at all and the rest are spent where the block below is not the one the heightmap promised. That is
affordable in a patch of 24 grass and not in a patch of four gravestones, which is why `patch(...)` names the spread.

| placed feature | patches | tries | placement |
|---|---|---|---|
| `barrens_jizo` | 1 per chunk | 24, all four kinds | `floor` |
| `barrens_gravestone` | 1 per chunk | 4 | `floor` |
| `gravestone` (muenzuka) | 1 per chunk | 4 | `floor` |

Tries are the whole of a patch's density, so a feature is only as common as it is set to be. An earlier gravestone of
**two** tries behind a `RarityFilter(3)` — two thirds of a try per chunk against the jizo's 24 — was thirty-six times
rarer and read as *not spawning at all* rather than as sparse. When a thing is "not appearing", the first question is
how much rarer it is than the thing that is appearing, not whether its feature runs.

When that does not settle it, the command does, placing the feature under the player's feet with none of the biome's
rarity filter or `BiomeFilter` in the way:

```
/place feature gensokyolegacy:gensokyo_biome/barrens_jizo ~ ~ ~
```

Jizo appearing means the feature is sound and the problem is the predicate or the density; nothing appearing means
`DoubleBlockFeature` itself is wrong. `/place feature` is the first thing to reach for and the density is the last
thing to change.

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

The rule that makes it impossible to get wrong: every biome's tail is a subsequence of one fixed order, and because the
tail is appended after the whole vanilla chain, no custom feature can ever land between two vanilla ones. That order
is not written down anywhere — `BiomeGen.Registrar` records each biome's placed features in the order it registers
them, and `BiomeGen.mergeOrder` merges every biome's list into the one order they can all be read as a subsequence
of. Two biomes sharing a custom feature therefore always place it in the same relative order, and the two of them
disagreeing is a cycle that `mergeOrder` reports as a datagen crash rather than a world that loads for an hour first.

A biome that never registers a shared feature cannot conflict with any other, which is most of them: `stones` is the
one feature eight biomes place, and it lives in `GensokyoBiomeFeatures` because splitting it would mean two placed
features that happen to be identical.

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
| `tengu_settlement` | a maple grove, ferns and grass on the unused slopes, gravel and coarse dirt paths |
| `secret_heaven_cliff` | boulders and a sparser scree of gravel and tuff, one patch per chunk |
| `youkai_mountain_summit` | soil pockets (a grass disk on stone), moss and glow lichen, scree, a rare pine |

The settlement's maple is the mountain band's own, thinned by the climb, and the paths and the moss are what tell it
apart from the valley grove below. The other three in that table are on bare rock and keep their pine.

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

`GLBiomeGen.genBiomeTag` writes `is_magical`, `is_forest`, `is_plains`, `is_mountain`, `is_mountain/peak`,
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